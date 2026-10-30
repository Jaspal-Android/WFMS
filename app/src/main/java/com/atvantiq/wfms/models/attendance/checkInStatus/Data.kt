package com.atvantiq.wfms.models.attendance.checkInStatus


import com.google.gson.annotations.SerializedName

data class Data(
    /** Today's attendance, or null when the day hasn't been started (it stays after a check-out). */
    @SerializedName("attendance_id")
    val attendanceId: Long?,
    @SerializedName("checked_in")
    val checkedIn: Boolean,
    @SerializedName("checked_out")
    val checkedOut: Boolean,
    @SerializedName("checkin_time")
    val checkinTime: String,
    @SerializedName("checkout_time")
    val checkoutTime: String
)