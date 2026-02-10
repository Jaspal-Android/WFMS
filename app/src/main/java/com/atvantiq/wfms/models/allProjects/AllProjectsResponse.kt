package com.atvantiq.wfms.models.allProjects


import com.google.gson.annotations.SerializedName

data class AllProjectsResponse(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("success")
    val success: Boolean?
)