package com.atvantiq.wfms.models.reimbursement.create


import com.google.gson.annotations.SerializedName

data class CreateClaimResponse(
    @SerializedName("code")
    val code: Int?,
   /* @SerializedName("data")
    val `data`: ApproveWorkData?,*/
    @SerializedName("message")
    val message: String?,
    @SerializedName("success")
    val success: Boolean?
)