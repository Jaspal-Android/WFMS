package com.atvantiq.wfms.ui.screens.forgotPassword.newPassword.vm

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class CreatePasswordVM(application: Application) : AndroidViewModel(application) {

    //Variable declaration
    var isPasswordVisible = true
    var isConfirmPasswordVisible = true

    var clickEvents = MutableLiveData<CreatePassClickEvents>()
    var errorHandler = MutableLiveData<CreatePassErrorHandler>()

    var password = ObservableField<String>().apply {
        set("")
    }

    var confirmPassword = ObservableField<String>().apply {
        set("")
    }

    /*
    * API Methods
    * */

    fun onPasswordToggleClick() {
        clickEvents.value = CreatePassClickEvents.ON_PASSWORD_TOGGLE_CLICK
    }

    fun onConfirmPasswordToggleClick() {
        clickEvents.value = CreatePassClickEvents.ON_CONFIRM_PASSWORD_TOGGLE_CLICK
    }

    fun createPasswordRequest(){
     if(isValidCreatePassDetails()){

     }
    }

    /*
   * Validate Methods
   * */
    private fun isValidCreatePassDetails(): Boolean {
        return when {
            password.get().toString().trim().isEmpty() -> {
                errorHandler.value = CreatePassErrorHandler.EMPTY_PASSWORD
                false
            }

            confirmPassword.get().toString().trim().isEmpty() -> {
                errorHandler.value = CreatePassErrorHandler.EMPTY_CONFIRM_PASSWORD
                false
            }

            password.get().toString().trim() != confirmPassword.get().toString().trim() -> {
                errorHandler.value = CreatePassErrorHandler.MISMATCH_PASSWORD
                false
            }
            else -> {
                true
            }
        }
    }
}