package com.atvantiq.wfms.ui.screens.admin.ui.approvals

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Approvals hub: one card per approval category. It loads nothing itself. */
@HiltViewModel
class ApprovalsVM @Inject constructor(application: Application) : BaseViewModel(application) {

    val clickEvents = MutableLiveData<ApprovalsClickEvents>()

    fun onAttendanceApprovalClick() {
        clickEvents.value = ApprovalsClickEvents.ATTENDANCE_APPROVAL
    }

    fun onWorkApprovalClick() {
        clickEvents.value = ApprovalsClickEvents.WORK_APPROVAL
    }

    fun onClaimsApprovalClick() {
        clickEvents.value = ApprovalsClickEvents.CLAIMS_APPROVAL
    }
}
