package com.atvantiq.wfms.models.workSites.approve


import com.google.gson.annotations.SerializedName

data class ApproveWorkSiteTypeResponse(
    @SerializedName("code")
    val code: Int?,
   /* @SerializedName("data")
    val `data`: ApproveWorkData?,*/
    @SerializedName("message")
    val message: String?,
    @SerializedName("success")
    val success: Boolean?
)