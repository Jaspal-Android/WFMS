package com.atvantiq.wfms.models.workSites.workSites


import com.google.gson.annotations.SerializedName

data class WorkSiteData(
    @SerializedName("attendance_logs")
    val attendanceLogs: AttendanceLogs?,
    @SerializedName("employee")
    val employee: Employee?,
    @SerializedName("work_sites")
    val workSites: List<WorkSite>?
)