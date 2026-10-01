package com.atvantiq.wfms.constants

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.atvantiq.wfms.R

/**
 * A status chip read from free text ignoring case: a Work Approval record's `action` or a claim's
 * `status`. "approved" → Approved, "reject" → Rejected, "submit" → Submitted, "pending" → Pending;
 * anything else shows the text itself ([labelRes] null).
 */
enum class ApprovalTextStatus(@StringRes val labelRes: Int?, @ColorRes val backgroundRes: Int, @ColorRes val textRes: Int) {
    APPROVED(R.string.approved, R.color.status_present_bg, R.color.status_present_text),
    REJECTED(R.string.rejected, R.color.status_absent_bg, R.color.status_absent_text),
    SUBMITTED(R.string.submitted, R.color.status_submitted_bg, R.color.status_submitted_text),
    PENDING(R.string.pending, R.color.status_incomplete_bg, R.color.status_incomplete_text),
    OTHER(null, R.color.status_unmarked_bg, R.color.status_unmarked_text);

    companion object {
        private const val APPROVED_WORD = "approved"
        private const val REJECTED_WORD = "reject"
        private const val SUBMITTED_WORD = "submit"
        private const val PENDING_WORD = "pending"

        fun from(text: String?): ApprovalTextStatus {
            val value = text.orEmpty()
            return when {
                value.contains(APPROVED_WORD, ignoreCase = true) -> APPROVED
                value.contains(REJECTED_WORD, ignoreCase = true) -> REJECTED
                value.contains(SUBMITTED_WORD, ignoreCase = true) -> SUBMITTED
                value.contains(PENDING_WORD, ignoreCase = true) -> PENDING
                else -> OTHER
            }
        }
    }
}
