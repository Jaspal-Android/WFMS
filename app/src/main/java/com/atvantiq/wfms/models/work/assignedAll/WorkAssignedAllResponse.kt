package com.atvantiq.wfms.models.work.assignedAll


import com.google.gson.annotations.SerializedName

data class WorkAssignedAllResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)