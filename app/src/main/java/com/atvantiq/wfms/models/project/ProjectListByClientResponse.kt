package com.atvantiq.wfms.models.project


import com.google.gson.annotations.SerializedName

data class ProjectListByClientResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val data: List<ProjectData>,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)