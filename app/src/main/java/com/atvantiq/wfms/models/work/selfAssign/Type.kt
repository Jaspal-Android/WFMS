package com.atvantiq.wfms.models.work.selfAssign


import com.google.gson.annotations.SerializedName

data class Type(
    @SerializedName("activity")
    val activity: List<Activity>,
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)