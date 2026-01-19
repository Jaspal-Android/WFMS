package com.atvantiq.wfms.models.workSites.workSites


import com.google.gson.annotations.SerializedName

data class WorkSitesResponse(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("data")
    val `data`: WorkSiteData?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("success")
    val success: Boolean?
)