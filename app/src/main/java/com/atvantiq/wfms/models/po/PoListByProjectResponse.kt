package com.atvantiq.wfms.models.po


import com.google.gson.annotations.SerializedName

data class PoListByProjectResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val data: List<PoData>,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)