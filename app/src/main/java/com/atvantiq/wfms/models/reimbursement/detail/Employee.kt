package com.atvantiq.wfms.models.reimbursement.detail


import com.google.gson.annotations.SerializedName

data class Employee(
    @SerializedName("employee_id")
    val employeeId: Long?,
    @SerializedName("employee_name")
    val employeeName: String?
)