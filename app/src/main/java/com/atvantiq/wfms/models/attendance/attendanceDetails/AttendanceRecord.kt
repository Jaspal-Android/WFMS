package com.atvantiq.wfms.models.attendance.attendanceDetails


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class AttendanceRecord(
    @SerializedName("action")
    val action: String?,
    @SerializedName("approval_status")
    val approvalStatus: Int?,
    @SerializedName("can_hr_mark_attendance")
    val canHrMarkAttendance: Boolean?,
    @SerializedName("checkin")
    val checkin: Checkin?,
    @SerializedName("checkout")
    val checkout: Checkout?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("employee")
    val employee: Employee?,
    @SerializedName("employee_remarks")
    val employeeRemarks: String?,
    @SerializedName("id")
    val id: Long?,
    @SerializedName("logs")
    val logs: Logs?,
    @SerializedName("status")
    val status: Int?,
    @SerializedName("work_hours")
    val workHours: String?
):Parcelable