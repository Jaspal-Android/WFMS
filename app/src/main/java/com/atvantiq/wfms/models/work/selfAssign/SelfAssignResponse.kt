package com.atvantiq.wfms.models.work.selfAssign


import com.google.gson.annotations.SerializedName

data class SelfAssignResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: SelfAssignData,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)