package com.atvantiq.wfms.models.site.allSites


import com.atvantiq.wfms.models.work.assignedAll.Circle
import com.atvantiq.wfms.models.work.assignedAll.Project
import com.google.gson.annotations.SerializedName

data class Site(
    @SerializedName("added_by")
    val addedBy: AddedBy,
    @SerializedName("address")
    val address: String,
    @SerializedName("circle")
    val circle: Circle?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("id")
    val id: Long,
    @SerializedName("is_active")
    val isActive: Int,
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("name")
    val name: String,
    @SerializedName("project")
    val project: Project?,
    @SerializedName("site_id")
    val siteId: String
)