package com.atvantiq.wfms.models.work.workAssigned


import com.google.gson.annotations.SerializedName

data class Project(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String
)