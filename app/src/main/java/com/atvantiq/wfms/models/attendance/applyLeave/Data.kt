package com.atvantiq.wfms.models.attendance.applyLeave


import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("employee_id")
    val employeeId: Long,
    @SerializedName("employee_name")
    val employeeName: String,
    @SerializedName("leave_id")
    val leaveId: Long
)