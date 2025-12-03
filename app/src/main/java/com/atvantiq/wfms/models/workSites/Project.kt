package com.atvantiq.wfms.models.workSites


import com.google.gson.annotations.SerializedName

data class Project(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String
)