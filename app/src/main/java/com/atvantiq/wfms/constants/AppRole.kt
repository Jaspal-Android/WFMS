package com.atvantiq.wfms.constants

/**
 * The app role worked out from `role` on `GET /employee/me`, compared case-insensitively and
 * ignoring spaces, `_` and `-` (as the web app does).
 */
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

    /**
     * How an approver is named in the approval logs and texts ("Approved by PM"), or null for a
     * role that can't approve.
     */
    val approverTag: String?
        get() = when (this) {
            PM -> PM_TAG
            OPS -> OPS_TAG
            ADMIN -> ADMIN_TAG
            EMPLOYEE, OTHER -> null
        }

    companion object {
        private const val PM_TAG = "PM"
        private const val OPS_TAG = "OPS"
        private const val ADMIN_TAG = "ADMIN"

        private val IGNORED_ROLE_CHARACTERS = setOf('_', '-')

        fun from(role: String?): AppRole {
            val value = normalize(role)
            return when {
                value.equals(normalize(ValConstants.ROLE_EMPLOYEE), ignoreCase = true) -> EMPLOYEE
                value.equals(normalize(ValConstants.ROLE_PM), ignoreCase = true) -> PM
                value.equals(normalize(ValConstants.ROLE_OPS), ignoreCase = true) -> OPS
                value.equals(normalize(ValConstants.ROLE_Admin), ignoreCase = true) -> ADMIN
                else -> OTHER
            }
        }

        private fun normalize(role: String?): String =
            role.orEmpty().filterNot { it.isWhitespace() || it in IGNORED_ROLE_CHARACTERS }
    }
}
