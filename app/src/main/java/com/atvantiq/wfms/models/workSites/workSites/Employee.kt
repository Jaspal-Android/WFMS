package com.atvantiq.wfms.models.workSites.workSites


import com.google.gson.annotations.SerializedName

data class Employee(
    @SerializedName("circle")
    val circle: Circle,
    @SerializedName("date_of_joining")
    val dateOfJoining: String,
    @SerializedName("designation")
    val designation: String,
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("reporting_manager")
    val reportingManager: ReportingManager,
    @SerializedName("signin_time")
    val signinTime: String,
    @SerializedName("signout_time")
    val signoutTime: String,
    @SerializedName("total_working_hours")
    val totalWorkingHours: String
)