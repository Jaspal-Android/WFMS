package com.atvantiq.wfms.ui.screens.admin.ui.attendanceApproval

import android.app.Application
import com.atvantiq.wfms.base.LiveEvent
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.AppRole
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.data.repository.creation.ICreationRepo
import com.atvantiq.wfms.models.allProjects.AllProjectsResponse
import com.atvantiq.wfms.models.attendance.approve.AttendanceApproveResponse
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceApprovalStatus
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceRecord
import com.atvantiq.wfms.models.attendance.attendanceDetails.canBeMarkedBy
import com.atvantiq.wfms.models.attendance.attendanceDetails.initialDecision
import com.atvantiq.wfms.models.attendance.attendanceDetails.needsProjectFor
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.ui.screens.admin.ui.approvals.MonthlyAttendanceListVM
import com.atvantiq.wfms.ui.screens.projectPicker.ProjectPicker
import com.atvantiq.wfms.ui.screens.projectPicker.ProjectPickerPurpose
import com.atvantiq.wfms.ui.screens.projectPicker.toProjectOption
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
    prefMain: SecurePrefMain,
    private val creationRepo: ICreationRepo
) : MonthlyAttendanceListVM(application, attendanceRepo) {

    val role: AppRole = PrefMethods.getAppRole(prefMain)

    /** The record being decided, and the form's status and remarks. */
    var decisionRecord: AttendanceRecord? = null
        private set
    val decisionStatus = ObservableField<AttendanceApprovalStatus>()
    val decisionRemarks = ObservableField<String>()

    /** A decision that can't be sent, as a message to show. One-shot. */
    val decisionError = LiveEvent<Int?>()

    val approveResponse = MutableLiveData<ApiState<AttendanceApproveResponse>>()

    /** True while a decision is being sent; the sheet can't be closed meanwhile. */
    var isSubmitting = false
        private set

    /** The project an Idle → Present decision is booked to. */
    val projectPicker = ProjectPicker()
    val activeProjectsResponse = MutableLiveData<ApiState<AllProjectsResponse>>()

    fun canMark(record: AttendanceRecord): Boolean = record.canBeMarkedBy(role)

    /** Starts a decision: the record's status (or Present) and "Attendance reviewed by {role}". */
    fun startDecision(record: AttendanceRecord) {
        projectPicker.close()
        decisionRecord = record
        decisionStatus.set(record.initialDecision)
        decisionRemarks.set(
            role.approverTag?.let { getApplication<Application>().getString(R.string.attendance_reviewed_by, it) }.orEmpty()
        )
    }

    fun selectStatus(status: AttendanceApprovalStatus) = decisionStatus.set(status)

    /**
     * `POST /attendance/approve/{id}` with `{status, remarks}`. Idle → Present first asks for the
     * project the day was worked on, and is sent from the picker by [submitDecisionOnProject].
     */
    fun submitDecision() {
        if (isSubmitting) return
        val record = validDecisionRecord()
        val status = decisionStatus.get()
        when {
            record == null || status == null -> Unit
            record.needsProjectFor(status) -> loadDecisionProjects()
            else -> sendDecision(record, status, projectId = null)
        }
    }

    /** The picker's Mark Present: the Idle → Present decision, booked to [projectId]. */
    fun submitDecisionOnProject(projectId: Long) {
        if (isSubmitting) return
        val record = validDecisionRecord()
        val status = decisionStatus.get()
        if (record != null && status != null && record.needsProjectFor(status)) {
            projectPicker.setSubmitting(true)
            sendDecision(record, status, projectId)
        }
    }

    /** `GET /project/all` (active only): opens the picker on the projects a day can be booked to. */
    fun loadDecisionProjects() {
        val record = decisionRecord ?: return
        projectPicker.startLoading(ProjectPickerPurpose.MarkPresent(record.employee?.name))
        executeApiCall(
            apiCall = { creationRepo.allProjects() },
            liveData = activeProjectsResponse,
            onSuccess = { response ->
                projectPicker.onResponse(
                    response.success, response.code, response.message,
                    response.data?.projects?.mapNotNull { it.toProjectOption() }
                )
            },
            onError = projectPicker::onError,
            cancelPrevious = true
        )
    }

    /** The record being decided, when this role can send a decision for it; otherwise reports why not. */
    private fun validDecisionRecord(): AttendanceRecord? {
        val record = decisionRecord
        val status = decisionStatus.get()
        val error = when {
            !role.canApprove -> R.string.attendance_approval_not_permitted
            record?.id == null -> R.string.attendance_id_missing
            status == null || status == AttendanceApprovalStatus.SUBMITTED -> R.string.select_attendance_status
            !record.canBeMarkedBy(role) -> R.string.attendance_not_waiting
            else -> null
        }
        if (error != null) decisionError.value = error
        return record.takeIf { error == null }
    }

    private fun sendDecision(record: AttendanceRecord, status: AttendanceApprovalStatus, projectId: Long?) {
        val attendanceId = record.id ?: return
        // The server doesn't require a project for Idle → Present yet, so the app never sends one without.
        if (projectId == null && record.needsProjectFor(status)) {
            decisionError.value = R.string.select_project_to_mark_present
            return
        }
        isSubmitting = true
        val params = JsonObject().apply {
            addProperty(STATUS_KEY, status.code)
            addProperty(REMARKS_KEY, decisionRemarks.get().orEmpty().trim())
            projectId?.let { addProperty(PROJECT_ID_KEY, it) }
        }
        executeApiCall(
            apiCall = { attendanceRepo.attendanceApprove(attendanceId, params) },
            liveData = approveResponse,
            onSuccess = { response ->
                isSubmitting = false
                projectPicker.setSubmitting(false)
                if (response.success == true) {
                    applyDecision(attendanceId, status.code)
                    projectPicker.close()
                }
            },
            onError = {
                isSubmitting = false
                projectPicker.setSubmitting(false)
            }
        )
    }

    /**
     * The decided row updates in place, without a reload: the new status chip and "Approved by
     * {role}", after which it is read-only for this role. Like the server, the decision is the
     * record's `approval_status`; the employee's own status is kept.
     */
    fun applyDecision(attendanceId: Long, statusCode: Int) {
        val approvedBy = role.approverTag?.let { getApplication<Application>().getString(R.string.approved_by_format, it) }
        val position = records.state.value?.items?.indexOfFirst { it.id == attendanceId } ?: -1
        records.updateItem(position) { record ->
            record.approvalStatus = statusCode
            if (approvedBy != null) record.action = approvedBy
        }
        decisionRecord?.takeIf { it.id == attendanceId }?.let { record ->
            record.approvalStatus = statusCode
            if (approvedBy != null) record.action = approvedBy
        }
    }

    private companion object {
        const val STATUS_KEY = "status"
        const val REMARKS_KEY = "remarks"
        const val PROJECT_ID_KEY = "project_id"
    }
}
