package com.atvantiq.wfms.ui.screens.materialReco.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.ui.screens.cab.CabClickEvents

class AddMaterialViewModel(application: Application) : AndroidViewModel(application) {

    /*Variables Init*/
    var clickEvents  = MutableLiveData<AddMaterialClickEvents>()

    /*Methods declaration*/

    fun onSaveClick(){
        clickEvents.value = AddMaterialClickEvents.ON_SAVE_CLICK
    }

    fun onCancelClick(){
        clickEvents.value = AddMaterialClickEvents.ON_CANCEL_CLICK
    }
}