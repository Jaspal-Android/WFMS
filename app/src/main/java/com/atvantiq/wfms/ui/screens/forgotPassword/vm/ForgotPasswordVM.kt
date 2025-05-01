package com.atvantiq.wfms.ui.screens.forgotPassword.vm

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class ForgotPasswordVM(application: Application) : AndroidViewModel(application) {

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
}