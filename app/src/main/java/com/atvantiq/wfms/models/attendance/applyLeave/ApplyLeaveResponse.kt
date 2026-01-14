package com.atvantiq.wfms.models.attendance.applyLeave


import com.google.gson.annotations.SerializedName

data class ApplyLeaveResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)