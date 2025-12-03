package com.atvantiq.wfms.ui.screens.admin

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel

class SharedDashboardVM(application: Application) : BaseViewModel(application) {

    var clickEvents = MutableLiveData<SharedDashClickEvents>()

    fun onLogoutClick(){
        clickEvents.value = SharedDashClickEvents.LOGOUT_CLICK
    }

    fun onSitesClick(){
        clickEvents.value = SharedDashClickEvents.OPEN_SITES_CLICK
    }

    fun onSitesApprovalsClick(){
        clickEvents.value = SharedDashClickEvents.OPEN_SITES_APPROVALS_CLICK
    }

    fun onClaimApprovalsClick(){
        clickEvents.value = SharedDashClickEvents.OPEN_CLAIM_APPROVALS_CLICK
    }

    fun onProfileClick(){
        clickEvents.value = SharedDashClickEvents.OPEN_PROFILE_CLICK
    }

}