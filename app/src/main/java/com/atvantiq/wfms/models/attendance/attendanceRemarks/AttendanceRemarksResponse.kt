package com.atvantiq.wfms.models.attendance.attendanceRemarks


import com.google.gson.annotations.SerializedName

data class AttendanceRemarksResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)