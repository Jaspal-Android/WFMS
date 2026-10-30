package com.atvantiq.wfms.models.attendance.attendanceDetails

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.atvantiq.wfms.R

/** Attendance status codes as the approval screens show them (spec 6.1); 0 or none is Submitted. */
enum class AttendanceApprovalStatus(
    val code: Int,
    @StringRes val labelRes: Int,
    @ColorRes val backgroundRes: Int,
    @ColorRes val textRes: Int
) {
    SUBMITTED(0, R.string.submitted, R.color.status_submitted_bg, R.color.status_submitted_text),
    PRESENT(1, R.string.present, R.color.status_present_bg, R.color.status_present_text),
    ABSENT(2, R.string.absent, R.color.status_absent_bg, R.color.status_absent_text),
    LEAVE(3, R.string.leave, R.color.status_leave_bg, R.color.status_leave_text),
    IDLE(4, R.string.idle, R.color.status_idle_bg, R.color.status_idle_text),
    HOLIDAY(5, R.string.holiday, R.color.status_holiday_bg, R.color.status_holiday_text),
    WORK_OFF(6, R.string.attendance_work_off, R.color.status_work_off_bg, R.color.status_work_off_text),
    ABSENT_BY_SYSTEM(7, R.string.attendance_absent_by_system, R.color.status_absent_na_bg, R.color.status_absent_na_text),
    INCOMPLETE(8, R.string.incomplete, R.color.status_incomplete_bg, R.color.status_incomplete_text);

    companion object {
        /** The eight statuses an approver can record, in the grid's order. */
        val decisions: List<AttendanceApprovalStatus> = entries.filter { it != SUBMITTED }

        fun from(code: Int?): AttendanceApprovalStatus = entries.firstOrNull { it.code == code } ?: SUBMITTED
    }
}
