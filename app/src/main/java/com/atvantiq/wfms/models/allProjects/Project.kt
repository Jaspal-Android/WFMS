package com.atvantiq.wfms.models.allProjects


import com.google.gson.annotations.SerializedName

data class Project(
    @SerializedName("circle")
    val circle: List<Circle?>?,
    @SerializedName("client")
    val client: Client?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("created_by")
    val createdBy: CreatedBy?,
    @SerializedName("id")
    val id: Long?,
    @SerializedName("is_active")
    val isActive: Int?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("site")
    val site: Any?
)