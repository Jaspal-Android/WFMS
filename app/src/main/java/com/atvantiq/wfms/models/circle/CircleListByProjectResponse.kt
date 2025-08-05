package com.atvantiq.wfms.models.circle


import com.google.gson.annotations.SerializedName

data class CircleListByProjectResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: List<CircleData>,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)