package com.atvantiq.wfms.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

class DashboardViewModel (
    private var application: Application,
) : AndroidViewModel(application) {


    // variables initializations
    var clickEvents = MutableLiveData<DashboardClickEvents>()

    // Methods
    fun onAnnouncementsClicks(){
        clickEvents.value = DashboardClickEvents.onAnnouncementsClicks
    }
}