package com.atvantiq.wfms.models.type


import com.google.gson.annotations.SerializedName

data class TypeData(
    @SerializedName("activities")
    val activities: List<Activity?>?,
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String?
)