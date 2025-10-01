package com.atvantiq.wfms.models.attendance.checkInStatus


import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("attendance_id")
    val attendanceId: Long,
    @SerializedName("checked_in")
    val checkedIn: Boolean,
    @SerializedName("checked_out")
    val checkedOut: Boolean,
    @SerializedName("checkin_time")
    val checkinTime: String,
    @SerializedName("checkout_time")
    val checkoutTime: String
)