package com.atvantiq.wfms.models.work.workAssigned


import com.google.gson.annotations.SerializedName

data class WorkAssignedResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: AssignedWorkData,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)