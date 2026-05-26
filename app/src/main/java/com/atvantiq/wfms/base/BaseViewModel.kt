// BaseViewModel.kt
package com.atvantiq.wfms.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.utils.NoInternetException
import com.atvantiq.wfms.utils.Utils
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
                } catch (e: Exception) {
                    onError?.invoke(e)
                    liveData.postValue(ApiState.error(e))
                }
            }
        } else {
            liveData.postValue(ApiState.error(NoInternetException("No Internet Connection")))
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
                } catch (e: Exception) {
                    state.update { prev -> onError(prev, e) }
                }
            }
        } else {
            state.update { prev -> onError(prev, NoInternetException("No Internet Connection")) }
        }
    }
}