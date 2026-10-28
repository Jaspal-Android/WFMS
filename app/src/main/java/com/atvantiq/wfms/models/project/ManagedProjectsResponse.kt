package com.atvantiq.wfms.models.project

import com.atvantiq.wfms.models.allProjects.Circle
import com.google.gson.annotations.SerializedName

/**
 * `GET /project/my-projects`: the projects the signed-in PM manages today, one row per project.
 * An empty list is an answer, not an error ("No projects are assigned to you as manager.").
 */
data class ManagedProjectsResponse(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("data")
    val data: List<ManagedProject>?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("success")
    val success: Boolean?
)

data class ManagedProject(
    @SerializedName("id")
    val id: Long?,
    @SerializedName("name")
    val name: String?,
    /** The project's main circle, or null. */
    @SerializedName("circle")
    val circle: Circle?
)
