package com.atvantiq.wfms.ui.screens.cab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.atvantiq.wfms.ui.screens.attendance.addSignInActivity.AddSignInClickEvents

class CabViewModel(application: Application) : AndroidViewModel(application) {

    /*Variables Init*/
    var clickEvents  = MutableLiveData<CabClickEvents>()

    /*Methods declaration*/
    fun onCameraClick(){
        clickEvents.value = CabClickEvents.ON_CAMERA_CLICK
    }

    fun onSaveClick(){
        clickEvents.value = CabClickEvents.ON_SAVE_CLICK
    }

    fun onCancelClick(){
        clickEvents.value = CabClickEvents.ON_CANCEL_CLICK
    }
}