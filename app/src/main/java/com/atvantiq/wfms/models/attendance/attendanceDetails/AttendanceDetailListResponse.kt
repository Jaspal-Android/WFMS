package com.atvantiq.wfms.models.attendance.attendanceDetails


import com.google.gson.annotations.SerializedName

data class AttendanceDetailListResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val data: AttendanceDetailData?,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)