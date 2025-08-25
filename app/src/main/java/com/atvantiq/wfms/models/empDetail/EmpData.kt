package com.atvantiq.wfms.models.empDetail


import com.google.gson.annotations.SerializedName

data class EmpData(
    @SerializedName("circle")
    val circle: String?,
    @SerializedName("date_of_joining")
    val dateOfJoining: String,
    @SerializedName("designation")
    val designation: String,
    @SerializedName("dob")
    val dob: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("employee_code")
    val employeeCode: String,
    @SerializedName("employee_id")
    val employeeId: Int,
    @SerializedName("gender")
    val gender: Any?,
    @SerializedName("name")
    val name: String,
    @SerializedName("permissions")
    val permissions: List<Permission>,
    @SerializedName("reporting_manager")
    val reportingManager: ReportingManager,
    @SerializedName("role")
    val role: String,
    @SerializedName("short_name")
    val shortName: String,
    @SerializedName("team")
    val team: Any?
)