package com.atvantiq.wfms.models.work.workDetailByDate


import com.atvantiq.wfms.models.work.assignedAll.WorkRecord
import com.atvantiq.wfms.models.work.workAssigned.Site
import com.google.gson.annotations.SerializedName

data class WorkDetailsByDateResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val data: List<Site>,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)