package com.atvantiq.wfms.models.work.startWork


import com.google.gson.annotations.SerializedName

data class StartWorkResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: StartWorkData,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)