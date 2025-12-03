package com.atvantiq.wfms.models.attendance


import com.google.gson.annotations.SerializedName

data class CheckInOutResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: CheckoutData,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)