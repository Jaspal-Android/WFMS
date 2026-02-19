package com.atvantiq.wfms.models.work.workDetail


import com.atvantiq.wfms.models.work.assignedAll.Circle
import com.atvantiq.wfms.models.work.assignedAll.Project
import com.google.gson.annotations.SerializedName

data class WorkDetailData(
    @SerializedName("circle")
    val circle: Circle?,
    @SerializedName("id")
    val id: Long?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("project")
    val project: Project?,
    @SerializedName("site_id")
    val siteId: String?,
    @SerializedName("status")
    val status: Status?,
    @SerializedName("type")
    val type: List<Type>?,
    @SerializedName("work_site_id")
    val workSiteId: Long?,
    @SerializedName("can_restart")
    var canRestart: Boolean?
)