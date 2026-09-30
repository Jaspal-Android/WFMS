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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class BaseViewModel(application: Application) : AndroidViewModel(application) {

    open fun <T> executeApiCall(
        apiCall: suspend () -> T,
        liveData: MutableLiveData<ApiState<T>>,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null
    ) {
        if (Utils.isInternet(getApplication())) {
            viewModelScope.launch {
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
