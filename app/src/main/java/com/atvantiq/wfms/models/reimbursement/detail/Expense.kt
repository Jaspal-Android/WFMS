package com.atvantiq.wfms.models.reimbursement.detail


import com.google.gson.annotations.SerializedName

data class Expense(
    @SerializedName("amount_per_site")
    val amountPerSite: Double?,
    @SerializedName("claimed_amount")
    val claimedAmount: Double?,
    @SerializedName("expense_id")
    val expenseId: Long?,
    @SerializedName("expense_type")
    val expenseType: String?
)