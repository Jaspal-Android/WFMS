package com.atvantiq.wfms.models.loginResponse


import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("user")
    val user: User
)