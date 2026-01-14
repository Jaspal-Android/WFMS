package com.atvantiq.wfms.models.workSites.workSites


import com.google.gson.annotations.SerializedName

data class WorkSite(
    @SerializedName("circle")
    val circle: Circle?,
    @SerializedName("emp_marked_latitude")
    val empMarkedLatitude: Double?,
    @SerializedName("emp_marked_longitude")
    val empMarkedLongitude: Double?,
    @SerializedName("end_time")
    val endTime: String?,
    @SerializedName("id")
    val id: Long?,
    @SerializedName("project")
    val project: Project?,
    @SerializedName("site")
    val site: Site?,
    @SerializedName("start_time")
    val startTime: String?,
    @SerializedName("status")
    val status: Status?,
    @SerializedName("visit_number")
    val visitNumber: Int?
)