package com.atvantiq.wfms.models.work.assignedAll


import com.google.gson.annotations.SerializedName

data class Type(
    @SerializedName("activity")
    val activity: List<Activity>,
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String
)