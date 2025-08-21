package com.atvantiq.wfms.models.attendance.attendanceDetails


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Record(
    @SerializedName("can_hr_mark_attendance")
    val canHrMarkAttendance: Boolean,
    @SerializedName("checkin")
    val checkin: Checkin,
    @SerializedName("checkout")
    val checkout: Checkout,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("employee_id")
    val employeeId: Int,
    @SerializedName("id")
    val id: Long,
    @SerializedName("status")
    val status: Int,
    @SerializedName("work_hours")
    val workHours: String
):Parcelable