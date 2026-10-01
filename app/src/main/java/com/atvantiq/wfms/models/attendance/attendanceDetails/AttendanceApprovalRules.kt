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

/** The status a decision starts from: the record's own, or Present when it has none yet. */
val AttendanceRecord.initialDecision: AttendanceApprovalStatus
    get() = AttendanceApprovalStatus.from(status).takeIf { it != AttendanceApprovalStatus.SUBMITTED }
        ?: AttendanceApprovalStatus.PRESENT
