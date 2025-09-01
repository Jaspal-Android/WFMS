// BaseViewModel.kt
package com.atvantiq.wfms.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.utils.NoInternetException
import com.atvantiq.wfms.utils.Utils
import kotlinx.coroutines.launch

open class BaseViewModel(application: Application) : AndroidViewModel(application) {
    protected fun <T> executeApiCall(
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
}