package com.atvantiq.wfms.models.allProjects


import com.google.gson.annotations.SerializedName

data class CreatedBy(
    @SerializedName("id")
    val id: Long?,
    @SerializedName("name")
    val name: String?
)