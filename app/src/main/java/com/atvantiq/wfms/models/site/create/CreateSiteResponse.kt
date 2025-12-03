package com.atvantiq.wfms.models.site.create


import com.google.gson.annotations.SerializedName

data class CreateSiteResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: CreateSiteData,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)