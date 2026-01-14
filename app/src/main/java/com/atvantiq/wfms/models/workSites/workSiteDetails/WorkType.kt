package com.atvantiq.wfms.models.workSites.workSiteDetails


import com.atvantiq.wfms.models.workSites.workSites.Status
import com.google.gson.annotations.SerializedName

data class WorkType(
    @SerializedName("activities")
    val activities: List<Activity?>?,
    @SerializedName("admin")
    val admin: Admin?,
    @SerializedName("employee_remarks")
    val employeeRemarks: String?,
    @SerializedName("id")
    val id: Long?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("ops")
    val ops: Ops?,
    @SerializedName("pm")
    val pm: Pm?,
    @SerializedName("status")
    val status: Status?,
    @SerializedName("work_ended_at")
    val workEndedAt: String?,
    @SerializedName("work_started_at")
    val workStartedAt: String?
)