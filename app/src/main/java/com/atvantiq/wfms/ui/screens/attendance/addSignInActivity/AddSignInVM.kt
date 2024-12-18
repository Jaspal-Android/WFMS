package com.atvantiq.wfms.ui.screens.attendance.addSignInActivity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class AddSignInVM(application: Application) : AndroidViewModel(application) {
    /*Variables Init*/
    var clickEvents  = MutableLiveData<AddSignInClickEvents>()

    /*Methods declaration*/
    fun onCameraClick(){
        clickEvents.value = AddSignInClickEvents.ON_CAMERA_CLICK
    }

    fun onSaveClick(){
        clickEvents.value = AddSignInClickEvents.ON_SAVE_CLICK
    }

    fun onCancelClick(){
        clickEvents.value = AddSignInClickEvents.ON_CANCEL_CLICK
    }
}