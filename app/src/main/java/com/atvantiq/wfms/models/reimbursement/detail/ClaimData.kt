package com.atvantiq.wfms.models.reimbursement.detail


import com.google.gson.annotations.SerializedName

data class ClaimData(
    @SerializedName("claim_category")
    val claimCategory: String?,
    @SerializedName("claim_purpose")
    val claimPurpose: String?,
    @SerializedName("date")
    val date: String?,
    @SerializedName("employee")
    val employee: Employee?,
    @SerializedName("id")
    val id: Long?,
    @SerializedName("remarks")
    val remarks: Any?,
    @SerializedName("sites")
    val sites: List<Site>?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("total_amount")
    val totalAmount: Double?,
    @SerializedName("travelling_from")
    val travellingFrom: String?,
    @SerializedName("travelling_to")
    val travellingTo: String?
)