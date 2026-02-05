package com.atvantiq.wfms.models.reimbursement.allClaims


import com.google.gson.annotations.SerializedName

data class Record(
    @SerializedName("claim_id")
    val claimId: Long?,
    @SerializedName("claim_number")
    val claimNumber: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("date")
    val date: String?,
    @SerializedName("expense_category")
    val expenseCategory: String?,
    @SerializedName("expense_count")
    val expenseCount: Int?,
    @SerializedName("site_count")
    val siteCount: Int?,
    @SerializedName("status")
    val status: Status?,
    @SerializedName("total_amount")
    val totalAmount: Double?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
)