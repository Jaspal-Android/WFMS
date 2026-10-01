package com.atvantiq.wfms.ui.screens.admin.ui.attendanceApproval

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.AppRole
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.models.attendance.approve.AttendanceApproveResponse
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceApprovalStatus
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceRecord
import com.atvantiq.wfms.models.attendance.attendanceDetails.canBeMarkedBy
import com.atvantiq.wfms.models.attendance.attendanceDetails.initialDecision
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.ui.screens.admin.ui.approvals.MonthlyAttendanceListVM
import com.google.gson.JsonObject
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Attendance Approval (spec 6): the month's records, and the decision form shared by the
 * Attendance Decision sheet and the Attendance Review screen.
 */
@HiltViewModel
class AttendanceApprovalVM @Inject constructor(
    application: Application,
    attendanceRepo: IAttendanceRepo,
    prefMain: SecurePrefMain
) : MonthlyAttendanceListVM(application, attendanceRepo) {

    val role: AppRole = PrefMethods.getAppRole(prefMain)

    /** The record being decided, and the form's status and remarks. */
    var decisionRecord: AttendanceRecord? = null
        private set
    val decisionStatus = ObservableField<AttendanceApprovalStatus>()
    val decisionRemarks = ObservableField<String>()

    /** A decision that can't be sent, as a message to show. One-shot. */
    val decisionError = MutableLiveData<Int?>()

    val approveResponse = MutableLiveData<ApiState<AttendanceApproveResponse>>()

    /** True while a decision is being sent; the sheet can't be closed meanwhile. */
    var isSubmitting = false
        private set

    fun canMark(record: AttendanceRecord): Boolean = record.canBeMarkedBy(role)

    /** Starts a decision: the record's status (or Present) and "Attendance reviewed by {role}". */
    fun startDecision(record: AttendanceRecord) {
        decisionRecord = record
        decisionStatus.set(record.initialDecision)
        decisionRemarks.set(
            role.approverTag?.let { getApplication<Application>().getString(R.string.attendance_reviewed_by, it) }.orEmpty()
        )
    }

    fun selectStatus(status: AttendanceApprovalStatus) = decisionStatus.set(status)

    /** `POST /attendance/approve/{id}` with `{status, remarks}`. */
    fun submitDecision() {
        if (isSubmitting) return
        val record = decisionRecord
        val status = decisionStatus.get()
        val error = when {
            !role.canApprove -> R.string.attendance_approval_not_permitted
            record?.id == null -> R.string.attendance_id_missing
            status == null || status == AttendanceApprovalStatus.SUBMITTED -> R.string.select_attendance_status
            !record.canBeMarkedBy(role) -> R.string.attendance_not_waiting
            else -> null
        }
        if (error != null) {
            decisionError.value = error
            return
        }
        val attendanceId = record?.id ?: return
        val code = status?.code ?: return
        isSubmitting = true
        val params = JsonObject().apply {
            addProperty(STATUS_KEY, code)
            addProperty(REMARKS_KEY, decisionRemarks.get().orEmpty().trim())
        }
        executeApiCall(
            apiCall = { attendanceRepo.attendanceApprove(attendanceId, params) },
            liveData = approveResponse,
            onSuccess = { response ->
                isSubmitting = false
                if (response.success == true) applyDecision(attendanceId, code)
            },
            onError = { isSubmitting = false }
        )
    }

    /**
     * The decided row updates in place, without a reload: the new status chip and "Approved by
     * {role}", after which it is read-only for this role.
     */
    fun applyDecision(attendanceId: Long, statusCode: Int) {
        val approvedBy = role.approverTag?.let { getApplication<Application>().getString(R.string.approved_by_format, it) }
        val position = records.state.value?.items?.indexOfFirst { it.id == attendanceId } ?: -1
        records.updateItem(position) { record ->
            record.status = statusCode
            if (approvedBy != null) record.action = approvedBy
        }
        decisionRecord?.takeIf { it.id == attendanceId }?.let { record ->
            record.status = statusCode
            if (approvedBy != null) record.action = approvedBy
        }
    }

    private companion object {
        const val STATUS_KEY = "status"
        const val REMARKS_KEY = "remarks"
    }
}
