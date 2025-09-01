package com.atvantiq.wfms.models.work.endWork


import com.google.gson.annotations.SerializedName

data class EndWorkResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val data: EndWorkData,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)