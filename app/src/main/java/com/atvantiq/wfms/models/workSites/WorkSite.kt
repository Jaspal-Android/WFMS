package com.atvantiq.wfms.models.workSites


import com.google.gson.annotations.SerializedName

data class WorkSite(
    @SerializedName("circle")
    val circle: Circle,
    @SerializedName("end_time")
    val endTime: String,
    @SerializedName("id")
    val id: Long,
    @SerializedName("project")
    val project: Project,
    @SerializedName("site")
    val site: Site,
    @SerializedName("start_time")
    val startTime: String,
    @SerializedName("approved_by_pm")
    val approvedByPm: Boolean,
    @SerializedName("approved_by_ops")
    val approvedByOps: Boolean
)