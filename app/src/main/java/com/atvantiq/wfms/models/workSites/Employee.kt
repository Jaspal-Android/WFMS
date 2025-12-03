package com.atvantiq.wfms.models.workSites


import com.google.gson.annotations.SerializedName

data class Employee(
    @SerializedName("circle")
    val circle: CircleX,
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
    val reportingManager: ReportingManager
)