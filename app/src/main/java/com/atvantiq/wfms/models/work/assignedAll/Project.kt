package com.atvantiq.wfms.models.work.assignedAll


import com.google.gson.annotations.SerializedName

data class Project(
    @SerializedName("client")
    val client: Int,
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)