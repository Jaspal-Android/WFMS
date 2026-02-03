package com.atvantiq.wfms.models.workSiteByDate


import com.google.gson.annotations.SerializedName

data class WorkSiteByDateResponse(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("success")
    val success: Boolean?
)