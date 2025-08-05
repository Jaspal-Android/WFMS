package com.atvantiq.wfms.models.attendance.checkInStatus


import com.google.gson.annotations.SerializedName

data class CheckInStatusResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)