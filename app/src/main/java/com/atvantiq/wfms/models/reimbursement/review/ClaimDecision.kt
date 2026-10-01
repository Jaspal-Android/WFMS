package com.atvantiq.wfms.models.reimbursement.review

import androidx.annotation.StringRes
import com.atvantiq.wfms.R
import com.atvantiq.wfms.models.reimbursement.detail.ClaimData
import com.google.gson.JsonArray
import com.google.gson.JsonObject

/** One expense of a claim under review, with what the approver has typed for it. */
data class ExpenseDecisionInput(
    val siteNumber: Int,
    val siteName: String?,
    val siteCode: String?,
    val expenseId: Long?,
    val expenseType: String?,
    val claimed: Double,
    var amountText: String,
    var remarks: String = "",
    val siteAmount: Double? = null
) {
    /** The typed amount, or null when it isn't a number. */
    val amount: Double? get() = amountText.trim().toDoubleOrNull()
}

/** The outcome of checking a claim decision before it is sent. */
sealed class ClaimDecision {
    data class Invalid(@StringRes val messageRes: Int) : ClaimDecision()
    data class Valid(val body: JsonObject, val approvedTotal: Double) : ClaimDecision()

    companion object {
        private const val CLAIMS = "claims"
        private const val CLAIM_ID = "claim_id"
        private const val EXPENSES = "expenses"
        private const val EXPENSE_ID = "expense_id"
        private const val AMOUNT = "amount"
        private const val REMARKS = "remarks"

        /** Every expense of [claim], prefilled with its claimed amount, numbered by site. */
        fun inputsFor(claim: ClaimData?): List<ExpenseDecisionInput> =
            claim?.sites.orEmpty().flatMapIndexed { index, site ->
                site.expenses.orEmpty().filterNotNull().map { expense ->
                    val claimed = expense.claimedAmount ?: 0.0
                    ExpenseDecisionInput(
                        siteNumber = index + 1,
                        siteName = site.siteName,
                        siteCode = site.siteCode,
                        expenseId = expense.expenseId,
                        expenseType = expense.expenseType,
                        claimed = claimed,
                        amountText = claimed.toBigDecimal().stripTrailingZeros().toPlainString(),
                        siteAmount = site.amountSite
                    )
                }
            }

        /** The sum of the amounts typed so far (unparseable ones count as 0). */
        fun total(inputs: List<ExpenseDecisionInput>): Double = inputs.sumOf { it.amount ?: 0.0 }

        /**
         * Checks the decision in the spec's order (8.2) and builds `POST /claim/approve`:
         * `{claims: [{claim_id, expenses: [{expense_id, amount, remarks?}]}]}`, remarks left out
         * when empty.
         */
        fun build(claimId: Long, inputs: List<ExpenseDecisionInput>): ClaimDecision {
            val valid = inputs.filter { it.expenseId != null }
            if (valid.isEmpty()) return Invalid(R.string.no_valid_expenses)
            if (valid.any { it.amount == null || it.amount!! < 0.0 }) return Invalid(R.string.enter_valid_approved_amounts)
            if (valid.any { it.amount!! > it.claimed }) return Invalid(R.string.approved_amount_exceeds_claimed)
            val expenses = JsonArray().apply {
                valid.forEach { input ->
                    add(JsonObject().apply {
                        addProperty(EXPENSE_ID, input.expenseId)
                        addProperty(AMOUNT, input.amount)
                        input.remarks.trim().takeIf { it.isNotEmpty() }?.let { addProperty(REMARKS, it) }
                    })
                }
            }
            val body = JsonObject().apply {
                add(CLAIMS, JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty(CLAIM_ID, claimId)
                        add(EXPENSES, expenses)
                    })
                })
            }
            return Valid(body, total(valid))
        }
    }
}
