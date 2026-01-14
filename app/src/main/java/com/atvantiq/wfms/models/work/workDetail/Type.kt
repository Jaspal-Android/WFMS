package com.atvantiq.wfms.models.work.workDetail


import com.google.gson.annotations.SerializedName

data class Type(
    @SerializedName("assigned_at")
    val assignedAt: String?,
    @SerializedName("id")
    val id: Long?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("status")
    val status: Status?
)