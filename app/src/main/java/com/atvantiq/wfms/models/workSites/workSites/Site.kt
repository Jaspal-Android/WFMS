package com.atvantiq.wfms.models.workSites.workSites


import com.google.gson.annotations.SerializedName

data class Site(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("site_id")
    val siteId: String
)