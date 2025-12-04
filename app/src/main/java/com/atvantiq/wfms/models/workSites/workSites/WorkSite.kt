package com.atvantiq.wfms.models.workSites.workSites


import com.google.gson.annotations.SerializedName

data class WorkSite(
    @SerializedName("circle")
    val circle: Circle,
    @SerializedName("end_time")
    val endTime: String,
    @SerializedName("id")
    val id: Long,
    @SerializedName("ops")
    val ops: Ops,
    @SerializedName("pm")
    val pm: Pm,
    @SerializedName("project")
    val project: Project,
    @SerializedName("site")
    val site: Site,
    @SerializedName("start_time")
    val startTime: String
)