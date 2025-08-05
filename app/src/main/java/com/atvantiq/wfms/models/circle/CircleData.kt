package com.atvantiq.wfms.models.circle


import com.google.gson.annotations.SerializedName

data class CircleData(
    @SerializedName("code")
    val code: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)