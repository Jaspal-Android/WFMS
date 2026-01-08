package com.atvantiq.wfms.models.work.workAssigned


import com.google.gson.annotations.SerializedName

data class Site(
    @SerializedName("circle")
    val circle: Circle,
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("project")
    val project: Project,
    @SerializedName("site_id")
    val siteId: String,
    @SerializedName("work_site_id")
    val workSiteId: Long,
    @SerializedName("status")
    var status: Status,
    @SerializedName("type")
    val type: List<Type>
)