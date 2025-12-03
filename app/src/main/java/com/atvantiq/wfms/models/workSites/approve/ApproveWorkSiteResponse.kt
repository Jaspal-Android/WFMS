package com.atvantiq.wfms.models.workSites.approve


import com.google.gson.annotations.SerializedName

data class ApproveWorkSiteResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)