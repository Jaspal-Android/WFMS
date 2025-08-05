package com.atvantiq.wfms.models.site


import com.google.gson.annotations.SerializedName

data class SiteData(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("site_id")
    val siteId: String
)