package com.atvantiq.wfms.ui.screens.reimbursement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.atvantiq.wfms.ui.screens.attendance.AttendanceClickEvents

class ReimbursementViewModel(application: Application) : AndroidViewModel(application) {

    var clickEvents = MutableLiveData<ReimbursementClickEvents>()

    private fun postClickEvent(event: ReimbursementClickEvents) {
        clickEvents.value = event
    }

    fun onCreateClaimClick() = postClickEvent(ReimbursementClickEvents.ON_CLICK_CREATE_CLAIM)
}