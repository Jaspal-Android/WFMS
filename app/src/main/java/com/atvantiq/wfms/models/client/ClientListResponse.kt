package com.atvantiq.wfms.models.client


import com.google.gson.annotations.SerializedName

data class ClientListResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val data: DataClientData,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)