package com.atvantiq.wfms.models.reimbursement.allClaims


import com.google.gson.annotations.SerializedName

data class AllClaimsResponse(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("success")
    val success: Boolean?
)