package com.atvantiq.wfms.ui.screens.login

import com.atvantiq.wfms.network.ApiState
import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.atvantiq.wfms.data.repository.auth.IAuthRepo
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.atvantiq.wfms.utils.Utils
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LoginVM @Inject constructor(
    application: Application,
    private val authRep: IAuthRepo
) : AndroidViewModel(application) {

    /*
    * Variables Declarations
    * */

    var isPasswordVisible = true

    var userName = ObservableField<String>().apply {
        set("")
    }
    var password = ObservableField<String>().apply {
        set("")
    }

    var isButtonEnabled = ObservableField<Boolean>().apply {
        set(true)
    }

    var clickEvents = MutableLiveData<LoginClickEvents>()
    var errorHandler = MutableLiveData<LoginErrorHandler>()
    var networkError = MutableLiveData<Boolean>()



    /*
    * Methods
    * */

    fun onForgetPasswordClick() {
        clickEvents.value = LoginClickEvents.ON_FORGET_PASSWORD_CLICK
    }

    fun onSubmitLoginClick() {
       loginRequest()
    }

    fun onPasswordToggleClick() {
        clickEvents.value = LoginClickEvents.ON_PASSWORD_TOGGLE
    }


    /*
    * Validate Methods
    * */
    private fun isValidLoginDetails(): Boolean {
        return when {
            userName.get().toString().trim().isEmpty() -> {
                errorHandler.value = LoginErrorHandler.EMPTY_USERNAME
                false
            }

            password.get().toString().trim().isEmpty() -> {
                errorHandler.value = LoginErrorHandler.EMPTY_PASSWORD
                false
            }

            else -> {
                true
            }
        }
    }


    /*
  * Login API
  * */
    var loginResponse = MutableLiveData<ApiState<LoginResponse>>()

    private fun loginRequest() {
        if (Utils.isInternet(getApplication())) {
            if(!isValidLoginDetails()){
                return
            }
            isButtonEnabled.set(false)
            var params = JsonObject()
            params.addProperty("email", userName.get().toString().trim())
            params.addProperty("password", password.get().toString().trim())
            viewModelScope.launch {
                loginResponse.postValue(ApiState.loading())
                try {
                    isButtonEnabled.set(true)
                    var  response = authRep.loginRequest(params)
                    loginResponse.postValue(ApiState.success(response))
                }catch (e:Exception){
                    isButtonEnabled.set(true)
                    loginResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            networkError.value = true
        }
    }

}