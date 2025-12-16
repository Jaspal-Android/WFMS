package com.atvantiq.wfms.ui.screens.attendance.applyLeave

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Locale

class ApplyLeaveVM(application: Application) : BaseViewModel(application) {

    // variables and methods for Apply Leave functionality can be added here

    var clickEvents = MutableLiveData<ApplyLeaveClickEvents>()
    var errorHandler = MutableLiveData<ApplyLeaveErrorHandler>()

    var leaveStartDate = ObservableField<String>().apply {
        set("")
    }
    var leaveEndDate = ObservableField<String>().apply {
        set("")
    }
    var leaveType = ObservableField<String>().apply {
        set("")
    }
    var leaveReason = ObservableField<String>().apply {
        set("")
    }
    var leaveAttachmentPath = ObservableField<String>().apply {
        set("")
    }

    fun onClickStartDate() {
        clickEvents.value = ApplyLeaveClickEvents.START_DATE_CLICK
    }
    fun onClickEndDate() {
        clickEvents.value = ApplyLeaveClickEvents.END_DATE_CLICK
    }
    fun onClickLeaveType() {
        clickEvents.value = ApplyLeaveClickEvents.LEAVE_TYPE_CLICK
    }
    fun onClickAttachment() {
        clickEvents.value = ApplyLeaveClickEvents.ATTACHMENT_CLICK
    }
    fun onClickCancelUploadImage() {
        clickEvents.value = ApplyLeaveClickEvents.CANCEL_UPLOAD_IMAGE
    }

    private fun isValidFormDetails(): Boolean {
        return when {
            leaveStartDate.get().isNullOrBlank() ->{
                errorHandler.value = ApplyLeaveErrorHandler.START_DATE_EMPTY
                false
            }
            leaveEndDate.get().isNullOrBlank() ->{
                errorHandler.value = ApplyLeaveErrorHandler.END_DATE_EMPTY
                false
            }
            leaveType.get().isNullOrBlank() ->{
                errorHandler.value = ApplyLeaveErrorHandler.LEAVE_TYPE_EMPTY
                false
            }
            leaveReason.get().isNullOrBlank() ->{
                errorHandler.value = ApplyLeaveErrorHandler.LEAVE_REASON_EMPTY
                false
            }
            !DateUtils.isStartDateBeforeEndDate(leaveStartDate.get().toString().trim(),leaveEndDate.get().toString().trim()) ->{
                errorHandler.value = ApplyLeaveErrorHandler.START_DATE_AFTER_END_DATE
                false
            }
            !DateUtils.isEndDateAfterStartDate(leaveStartDate.get().toString().trim(),leaveEndDate.get().toString().trim()) ->{
                errorHandler.value = ApplyLeaveErrorHandler.END_DATE_BEFORE_START_DATE
                false
            }
            else -> true
        }
    }

    fun onClickSubmitLeave() {
        if(isValidFormDetails()){
            // Proceed with leave submission logic
        }
    }
}