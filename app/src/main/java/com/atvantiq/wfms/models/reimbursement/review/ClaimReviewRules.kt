package com.atvantiq.wfms.models.reimbursement.review

import com.atvantiq.wfms.constants.AppRole

private const val APPROVED_BY_PREFIX = "approved by "
private const val REJECT_WORD = "reject"

/**
 * Whether [role] can act on a claim with [status] (spec 8.2): the role approves, the claim isn't
 * already "Approved by {role}" and isn't rejected (both ignoring case).
 */
fun canReviewClaim(status: String?, role: AppRole): Boolean {
    val tag = role.approverTag ?: return false
    val value = status.orEmpty().trim()
    return !value.equals(APPROVED_BY_PREFIX + tag, ignoreCase = true) && !value.contains(REJECT_WORD, ignoreCase = true)
}
