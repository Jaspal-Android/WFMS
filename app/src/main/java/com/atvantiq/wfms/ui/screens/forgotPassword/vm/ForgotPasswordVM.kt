package com.atvantiq.wfms.ui.screens.forgotPassword.vm

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.repository.auth.IAuthRepo
import com.atvantiq.wfms.models.forgotPassword.ForgotPasswordResponse
import com.atvantiq.wfms.network.ApiState
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordVM @Inject constructor(
    application: Application,
    private val authRepo: IAuthRepo
) : BaseViewModel(application) {

    //Variable declaration

    var clickEvents = MutableLiveData<ForgotPassClickEvents>()
    var errorHandler = MutableLiveData<ForgotPassErrorHandler>()

    var emailAddress = ObservableField<String>().apply {
        set("")
    }

    //Methods
    fun onResetPasswordClick(){
        if(isValidForgotPassDetails()){
            clickEvents.value = ForgotPassClickEvents.ON_RESET_PASSWORD_CLICK
        }
    }

    fun onBackLoginClick(){
        clickEvents.value = ForgotPassClickEvents.ON_BACK_TO_LOGIN_CLICK
    }

    /*
   * Validate Methods
   * */
    private fun isValidForgotPassDetails(): Boolean {
        return when {
            emailAddress.get().toString().trim().isEmpty() -> {
                errorHandler.value = ForgotPassErrorHandler.EMPTY_EMAIL_ADDRESS
                false
            }
            else -> {
                true
            }
        }
    }

    /*
    * ForgetPassword API
    * */
    var forgotPasswordResponse  = MutableLiveData<ApiState<ForgotPasswordResponse>>()
    fun sendForgotPassword(email:String) {
        val params = JsonObject().apply {
            addProperty("email", email)
        }
        executeApiCall(
            apiCall = {authRepo.forgotPassword(params)},
            liveData = forgotPasswordResponse,
        )
    }
}