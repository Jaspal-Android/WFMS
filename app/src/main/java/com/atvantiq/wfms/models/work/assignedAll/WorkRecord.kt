package com.atvantiq.wfms.models.work.assignedAll


import com.google.gson.annotations.SerializedName

data class WorkRecord(
    @SerializedName("assigned_by")
    val assignedBy: AssignedBy,
    @SerializedName("circle")
    val circle: Circle,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("employee")
    val employee: Any?,
    @SerializedName("id")
    val id: Int,
    @SerializedName("progress")
    val progress: List<Progres>?,
    @SerializedName("project")
    val project: Project,
    @SerializedName("site")
    val site: Site,
    @SerializedName("status")
    var status: String,
    @SerializedName("type")
    val type: List<Type>,
    @SerializedName("updated_at")
    val updatedAt: String
)