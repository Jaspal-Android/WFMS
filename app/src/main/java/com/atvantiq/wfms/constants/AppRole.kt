package com.atvantiq.wfms.constants

/** The app role worked out from `role` on `GET /employee/me`, trimmed and compared case-insensitively. */
enum class AppRole {
    EMPLOYEE,
    PM,
    OPS,
    ADMIN,

    /** Any other role: the least access, with no approvals. */
    OTHER;

    /** Attendance, work and claim approvals are gated by the role, not by a permission. */
    val canApprove: Boolean
        get() = this == PM || this == OPS || this == ADMIN

    companion object {
        fun from(role: String?): AppRole {
            val value = role?.trim().orEmpty()
            return when {
                value.equals(ValConstants.ROLE_EMPLOYEE, ignoreCase = true) -> EMPLOYEE
                value.equals(ValConstants.ROLE_PM, ignoreCase = true) -> PM
                value.equals(ValConstants.ROLE_OPS, ignoreCase = true) -> OPS
                value.equals(ValConstants.ROLE_Admin, ignoreCase = true) -> ADMIN
                else -> OTHER
            }
        }
    }
}
