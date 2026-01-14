package com.atvantiq.wfms.models.workSites.workSiteDetails


import com.atvantiq.wfms.models.workSites.workSites.Circle
import com.atvantiq.wfms.models.workSites.workSites.Project
import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("circle")
    val circle: Circle?,
    @SerializedName("project")
    val project: Project?,
    @SerializedName("site")
    val site: Site?,
    @SerializedName("work_site_id")
    val workSiteId: Long?,
    @SerializedName("work_type")
    val workType: List<WorkType>?
)