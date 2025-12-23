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
    @SerializedName("status")
    val status: Status,
    @SerializedName("type")
    val type: List<Type>
)