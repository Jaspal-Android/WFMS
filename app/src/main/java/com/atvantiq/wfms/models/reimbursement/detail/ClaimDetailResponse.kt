package com.atvantiq.wfms.models.reimbursement.detail


import com.google.gson.annotations.SerializedName

data class ClaimDetailResponse(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("data")
    val `data`: ClaimData?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("success")
    val success: Boolean?
)