// BaseViewModel.kt
package com.atvantiq.wfms.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.atvantiq.wfms.network.ApiErrorKind
import com.atvantiq.wfms.network.ApiErrorMapper
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.utils.NoInternetException
import com.atvantiq.wfms.utils.Utils
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class BaseViewModel(application: Application) : AndroidViewModel(application) {

    // Requests that may be superseded, by the LiveData they report to. Only touched on the main
    // thread (viewModelScope runs on Dispatchers.Main.immediate).
    private val supersedable = mutableMapOf<MutableLiveData<*>, Job>()

    /**
     * @param cancelPrevious for reads that a newer request replaces (search text, filter, page 1
     * after a refresh, month picker): cancels the request still running for [liveData], so a slow
     * answer to an older query can never overwrite the result of the newer one. A cancelled
     * request reports nothing. Never use it for writes: cancelling a POST mid-flight leaves the
     * user unsure whether it was applied.
     */
    open fun <T> executeApiCall(
        apiCall: suspend () -> T,
        liveData: MutableLiveData<ApiState<T>>,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null,
        cancelPrevious: Boolean = false
    ) {
        if (cancelPrevious) supersedable.remove(liveData)?.cancel()
        if (Utils.isInternet(getApplication())) {
            val job = viewModelScope.launch {
                liveData.postValue(ApiState.loading())
                try {
                    val response = apiCall()
                    onSuccess?.invoke(response)
                    liveData.postValue(ApiState.success(response))
                } catch (e: CancellationException) {
                    // The screen is gone (or the caller cancelled); this is not a failed request.
                    throw e
                } catch (e: Exception) {
                    val error = toUserFacingError(e)
                    onError?.invoke(error)
                    liveData.postValue(ApiState.error(error))
                }
            }
            if (cancelPrevious && job.isActive) {
                supersedable[liveData] = job
                job.invokeOnCompletion { if (supersedable[liveData] === job) supersedable.remove(liveData) }
            }
        } else {
            val error = noInternetError()
            onError?.invoke(error)
            liveData.postValue(ApiState.error(error))
        }
    }

    protected fun <S, T> executeStateRequest(
        state: MutableStateFlow<S>,
        apiCall: suspend () -> T,
        setLoading: (S) -> S,
        onSuccess: (S, T) -> S,
        onError: (S, Throwable) -> S
    ) {
        if (Utils.isInternet(getApplication())) {
            viewModelScope.launch {
                state.update(setLoading)
                try {
                    val response = apiCall()
                    state.update { prev -> onSuccess(prev, response) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val error = toUserFacingError(e)
                    state.update { prev -> onError(prev, error) }
                }
            }
        } else {
            state.update { prev -> onError(prev, noInternetError()) }
        }
    }

    /** The offline error shown by screens; its message is localized. */
    protected fun noInternetError(): NoInternetException =
        NoInternetException(getApplication<Application>().getString(ApiErrorKind.NO_INTERNET.messageRes))

    /**
     * Screens show `throwable.message`, so every failure is converted once here into an exception
     * with a message a user can act on. HTTP failures stay `HttpException`s, so 401 handling in
     * the screens is unaffected.
     */
    private fun toUserFacingError(e: Exception): Exception {
        val error = ApiErrorMapper.userFacing(e) { kind ->
            getApplication<Application>().getString(kind.messageRes)
        }
        if (ApiErrorMapper.kindOf(error).isUnexpected) reportUnexpectedError(e)
        return error
    }

    /**
     * Failures that are neither connectivity nor a server answer (bad JSON, an unexpected
     * exception) are bugs, and the user only sees a generic message, so report the original.
     */
    protected open fun reportUnexpectedError(error: Exception) {
        runCatching { FirebaseCrashlytics.getInstance().recordException(error) }
    }
}
