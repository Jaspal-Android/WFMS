package com.atvantiq.wfms.models.type


import com.google.gson.annotations.SerializedName

data class Activity(
    @SerializedName("id")
    val id: Long?,
    @SerializedName("name")
    val name: String?
)