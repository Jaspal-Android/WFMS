package com.atvantiq.wfms.models.work.selfAssign


import com.atvantiq.wfms.models.work.assignedAll.Circle
import com.atvantiq.wfms.models.work.assignedAll.Project
import com.google.gson.annotations.SerializedName

data class SelfAssignData(
    @SerializedName("circle")
    val circle: Circle,
    @SerializedName("client")
    val client: Client,
    @SerializedName("project")
    val project: Project,
    @SerializedName("results")
    val results: List<Result>
)