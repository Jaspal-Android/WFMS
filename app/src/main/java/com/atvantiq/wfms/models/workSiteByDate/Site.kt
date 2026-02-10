package com.atvantiq.wfms.models.workSiteByDate


import com.google.gson.annotations.SerializedName

data class Site(
    @SerializedName("circle_code")
    val circleCode: String?,
    @SerializedName("circle_id")
    val circleId: Long?,
    @SerializedName("circle_name")
    val circleName: String?,
    @SerializedName("project_id")
    val projectId: Long?,
    @SerializedName("project_name")
    val projectName: String?,
    @SerializedName("site_address")
    val siteAddress: String?,
    @SerializedName("site_id")
    val siteId: Long?,
    @SerializedName("site_name")
    val siteName: String?
)