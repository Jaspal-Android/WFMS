package com.atvantiq.wfms.models.activity


import com.google.gson.annotations.SerializedName

data class ActivityData(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)