package com.atvantiq.wfms.models.attendance.attendanceDetails

import com.atvantiq.wfms.constants.AppRole

private const val BY_PREFIX = "by "

/**
 * Whether [role] can still mark this record (spec 6.1): the role approves, the record has an id,
 * the role's own log isn't approved and the action doesn't already say "by {role}" (any case).
 */
fun AttendanceRecord.canBeMarkedBy(role: AppRole): Boolean {
    val tag = role.approverTag ?: return false
    if (id == null) return false
    val ownLogApproved = when (role) {
        AppRole.PM -> logs?.pM?.isApproved
        AppRole.OPS -> logs?.oPS?.isApproved
        AppRole.ADMIN -> logs?.aDMIN?.isApproved
        AppRole.EMPLOYEE, AppRole.OTHER -> null
    }
    return ownLogApproved != true && !action.orEmpty().contains(BY_PREFIX + tag, ignoreCase = true)
}

/**
 * The record's status now: the reviewers' decision when there is one (not Submitted), otherwise
 * the employee's own status.
 */
val AttendanceRecord.currentStatus: AttendanceApprovalStatus
    get() = approvalStatus?.takeIf { it != AttendanceApprovalStatus.SUBMITTED.code }
        ?.let(AttendanceApprovalStatus::from)
        ?: AttendanceApprovalStatus.from(status)

/**
 * Marking an Idle day Present must name the project it was worked on; no other change sends one
 * (the server only keeps it for Idle → Present).
 */
fun AttendanceRecord.needsProjectFor(decision: AttendanceApprovalStatus?): Boolean =
    decision == AttendanceApprovalStatus.PRESENT && currentStatus == AttendanceApprovalStatus.IDLE

/**
 * The status a decision starts from: the one the record shows ([currentStatus]), or Present when
 * it is still Submitted.
 */
val AttendanceRecord.initialDecision: AttendanceApprovalStatus
    get() = currentStatus.takeIf { it != AttendanceApprovalStatus.SUBMITTED }
        ?: AttendanceApprovalStatus.PRESENT
