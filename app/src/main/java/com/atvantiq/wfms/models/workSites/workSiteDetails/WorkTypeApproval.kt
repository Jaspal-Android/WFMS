package com.atvantiq.wfms.models.workSites.workSiteDetails

import com.atvantiq.wfms.constants.ApprovalStatusCodes
import com.atvantiq.wfms.constants.StatusCodes
import com.atvantiq.wfms.constants.ValConstants

/**
 * Whether [role] may approve or reject this work type: it is in progress or completed, and
 * neither that role nor the admin has actioned it yet.
 */
fun WorkType.isApprovableBy(role: String): Boolean {
    val notYetActioned = when (role.lowercase()) {
        ValConstants.ROLE_PM.lowercase() ->
            pm?.status == ApprovalStatusCodes.OPEN && admin?.status == ApprovalStatusCodes.OPEN
        ValConstants.ROLE_OPS.lowercase() ->
            ops?.status == ApprovalStatusCodes.OPEN && admin?.status == ApprovalStatusCodes.OPEN
        ValConstants.ROLE_Admin.lowercase() -> admin?.status == ApprovalStatusCodes.OPEN
        else -> false
    }
    return notYetActioned && status?.code in listOf(StatusCodes.WIP, StatusCodes.COMPLETED)
}

/** The work types "Select all" may pick for [role]. */
fun List<WorkType>.approvableBy(role: String): List<WorkType> = filter { it.isApprovableBy(role) }
