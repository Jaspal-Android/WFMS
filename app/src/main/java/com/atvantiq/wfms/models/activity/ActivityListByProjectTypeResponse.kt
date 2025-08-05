package com.atvantiq.wfms.models.activity


import com.google.gson.annotations.SerializedName

data class ActivityListByProjectTypeResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: List<ActivityData>,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)