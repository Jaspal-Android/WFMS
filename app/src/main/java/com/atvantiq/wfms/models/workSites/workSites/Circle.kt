package com.atvantiq.wfms.models.workSites.workSites


import com.google.gson.annotations.SerializedName

data class Circle(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String
)