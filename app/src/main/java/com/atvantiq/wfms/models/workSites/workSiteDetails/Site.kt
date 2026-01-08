package com.atvantiq.wfms.models.workSites.workSiteDetails


import com.google.gson.annotations.SerializedName

data class Site(
    @SerializedName("id")
    val id: Long?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("site_id")
    val siteId: String?,
    @SerializedName("status")
    val status: Status?
)