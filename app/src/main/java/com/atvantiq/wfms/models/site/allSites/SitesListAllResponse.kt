package com.atvantiq.wfms.models.site.allSites


import com.google.gson.annotations.SerializedName

data class SitesListAllResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: AllSiteData,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)