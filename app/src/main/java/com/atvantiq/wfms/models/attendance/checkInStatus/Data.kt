package com.atvantiq.wfms.models.attendance.checkInStatus


import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("attendance_id")
    val attendanceId: Any?,
    @SerializedName("checked_in")
    val checkedIn: Boolean,
    @SerializedName("checkin_time")
    val checkinTime: Any?
)