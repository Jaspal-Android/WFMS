package com.atvantiq.wfms.models.forgotPassword


import com.google.gson.annotations.SerializedName

data class ForgotPasswordResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: Any?,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)