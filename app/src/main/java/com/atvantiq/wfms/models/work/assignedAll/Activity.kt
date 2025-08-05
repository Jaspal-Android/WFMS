package com.atvantiq.wfms.models.work.assignedAll


import com.google.gson.annotations.SerializedName

data class Activity(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)