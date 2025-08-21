package com.atvantiq.wfms.models.work.selfAssign


import com.google.gson.annotations.SerializedName

data class Employee(
    @SerializedName("assigned_by")
    val assignedBy: AssignedBy,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("site")
    val site: List<Site>,
    @SerializedName("status")
    val status: String,
    @SerializedName("type")
    val type: List<Type>
)