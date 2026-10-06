package com.atvantiq.wfms.ui.screens.attendance.applyLeave

import android.app.Application
import com.atvantiq.wfms.base.LiveEvent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.base.savedField
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.models.attendance.applyLeave.ApplyLeaveResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ApplyLeaveVM @Inject constructor(
    application: Application,
    private val attendanceRepo: IAttendanceRepo,
    state: SavedStateHandle
) : BaseViewModel(application) {

    var clickEvents = LiveEvent<ApplyLeaveClickEvents>()
    var errorHandler = LiveEvent<ApplyLeaveErrorHandler>()

    // Kept in the SavedStateHandle, so the form survives Android ending the process while the user
    // is in the camera, the gallery or another app.
    val leaveStartDate = state.savedField("leaveStartDate", "")
    val leaveEndDate = state.savedField("leaveEndDate", "")
    val leaveType = state.savedField("leaveType", "")
    val leaveReason = state.savedField("leaveReason", "")
    val leaveAttachmentPath = state.savedField("leaveAttachmentPath", "")

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
            applyLeave()
        }
    }

    var applyLeaveResponse  = MutableLiveData<ApiState<ApplyLeaveResponse>>()
    fun applyLeave() {
        val path = leaveAttachmentPath.get().toString().trim()
        val attachmentPart = if (path.isNotBlank()) {
            val file = File(path)
            if (file.exists()) {
                MultipartBody.Part.createFormData("attachment", file.name, file.asRequestBody("image/*".toMediaType()))
            } else {
                null
            }
        } else {
            null
        }
        val leaveType = leaveType.get().toString().trim().toRequestBody("text/plain".toMediaType())
        val fromDate = leaveStartDate.get().toString().trim().toRequestBody("text/plain".toMediaType())
        val toDate = leaveEndDate.get().toString().trim().toRequestBody("text/plain".toMediaType())
        val reason = leaveReason.get().toString().trim().toRequestBody("text/plain".toMediaType())

        executeApiCall(
            apiCall = { attendanceRepo.applyLeave(
                leaveType = leaveType,
                fromDate = fromDate,
                toDate = toDate,
                reason = reason,
                attachment = attachmentPart
            ) },
            liveData = applyLeaveResponse,
        )
    }

    fun clearData(){
        leaveStartDate.set("")
        leaveEndDate.set("")
        leaveType.set("")
        leaveReason.set("")
        leaveAttachmentPath.set("")
    }
}