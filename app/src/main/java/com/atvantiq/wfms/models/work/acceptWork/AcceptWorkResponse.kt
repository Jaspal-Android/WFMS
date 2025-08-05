package com.atvantiq.wfms.models.work.acceptWork


import com.google.gson.annotations.SerializedName

data class AcceptWorkResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: AcceptWorkData,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)