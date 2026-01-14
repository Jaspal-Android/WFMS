package com.atvantiq.wfms.models.work.workDetail


import com.google.gson.annotations.SerializedName

data class WorkDetailResponse(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("data")
    val `data`: WorkDetailData?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("success")
    val success: Boolean?
)