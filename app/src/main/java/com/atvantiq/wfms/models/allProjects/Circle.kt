package com.atvantiq.wfms.models.allProjects


import com.google.gson.annotations.SerializedName

data class Circle(
    @SerializedName("code")
    val code: String?,
    @SerializedName("id")
    val id: Long?,
    @SerializedName("name")
    val name: String?
)