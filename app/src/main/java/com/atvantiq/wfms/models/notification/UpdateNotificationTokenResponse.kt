package com.atvantiq.wfms.models.notification


import com.google.gson.annotations.SerializedName

data class UpdateNotificationTokenResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: Any?,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)