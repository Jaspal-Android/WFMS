package com.atvantiq.wfms.models.site


import com.google.gson.annotations.SerializedName

data class SiteListByProjectResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: List<SiteData>,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)