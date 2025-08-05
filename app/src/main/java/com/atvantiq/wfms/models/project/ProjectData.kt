package com.atvantiq.wfms.models.project


import com.google.gson.annotations.SerializedName

data class ProjectData(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)