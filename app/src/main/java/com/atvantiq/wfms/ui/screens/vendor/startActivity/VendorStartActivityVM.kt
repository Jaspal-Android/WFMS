package com.atvantiq.wfms.ui.screens.vendor.startActivity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class VendorStartActivityVM(application: Application) : AndroidViewModel(application) {
    /*Variables Init*/
    var clickEvents  = MutableLiveData<VendorStartActivityClickEvents>()

    /*Methods declaration*/
    fun onCameraClick(){
        clickEvents.value = VendorStartActivityClickEvents.ON_CAMERA_CLICK
    }

    fun onSaveClick(){
        clickEvents.value = VendorStartActivityClickEvents.ON_SAVE_CLICK
    }

    fun onCancelClick(){
        clickEvents.value = VendorStartActivityClickEvents.ON_CANCEL_CLICK
    }
}