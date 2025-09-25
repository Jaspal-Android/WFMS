package com.atvantiq.wfms.ui.screens.login

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.data.repository.auth.IAuthRepo
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.utils.Utils
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginVM @Inject constructor(
    application: Application,
    private val authRepo: IAuthRepo
) : BaseViewModel(application) {

    var isPasswordVisible = true
    val userName = MutableLiveData<String>("")
    val password = MutableLiveData<String>("")
    val isButtonEnabled = MutableLiveData<Boolean>(true)

    val clickEvents = MutableLiveData<LoginClickEvents>()
    val errorHandler = MutableLiveData<LoginErrorHandler>()
    val networkError = MutableLiveData<Boolean>()
    val loginResponse = MutableLiveData<ApiState<LoginResponse>>()

    // Click event handlers
    fun onForgetPasswordClick() = postClickEvent(LoginClickEvents.ON_FORGET_PASSWORD_CLICK)
    fun onSubmitLoginClick() { if (isValidLoginDetails()) loginRequest() }
    fun onPasswordToggleClick() = postClickEvent(LoginClickEvents.ON_PASSWORD_TOGGLE)

    private fun postClickEvent(event: LoginClickEvents) {
        clickEvents.value = event
    }

    private fun isValidLoginDetails(): Boolean {
        return when {
            userName.value.isNullOrBlank() -> {
                errorHandler.value = LoginErrorHandler.EMPTY_USERNAME
                false
            }
            password.value.isNullOrBlank() -> {
                errorHandler.value = LoginErrorHandler.EMPTY_PASSWORD
                false
            }
            else -> true
        }
    }

    private fun loginRequest() {
        val params = JsonObject().apply {
            addProperty("email", userName.value.orEmpty().trim())
            addProperty("password", password.value.orEmpty().trim())
        }
        isButtonEnabled.value = false
        executeApiCall(
            apiCall = { authRepo.loginRequest(params) },
            liveData = loginResponse,
            onSuccess = { isButtonEnabled.value = true },
            onError = { isButtonEnabled.value = true }
        )
    }
}
