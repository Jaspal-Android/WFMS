package com.atvantiq.wfms.models.client


import com.google.gson.annotations.SerializedName

data class AddedBy(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)