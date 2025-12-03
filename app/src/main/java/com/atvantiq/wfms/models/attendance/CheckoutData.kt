package com.atvantiq.wfms.models.attendance


import com.google.gson.annotations.SerializedName

data class CheckoutData(
    @SerializedName("attendance_id")
    val attendanceId: Long,
    @SerializedName("day_progress")
    val dayProgress: Boolean
)