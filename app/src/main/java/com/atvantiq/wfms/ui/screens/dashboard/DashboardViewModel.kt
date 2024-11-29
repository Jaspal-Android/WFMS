package com.atvantiq.wfms.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    // variables initializations
    var clickEvents = MutableLiveData<DashboardClickEvents>()

    // Methods
    fun onAnnouncementsClicks(){
        clickEvents.value = DashboardClickEvents.onAnnouncementsClicks
    }
}