package com.atvantiq.wfms.models.attendance.attendanceRemarks


import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("attendance_id")
    val attendanceId: Long,
    @SerializedName("employee_remarks")
    val employeeRemarks: Any?,
    @SerializedName("status")
    val status: Int
)