package com.atvantiq.wfms.models.reimbursement.delete


import com.google.gson.annotations.SerializedName

data class DeleteClaimResponse(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("success")
    val success: Boolean?
)
