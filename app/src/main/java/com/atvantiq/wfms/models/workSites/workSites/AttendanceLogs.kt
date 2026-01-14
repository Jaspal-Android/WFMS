package com.atvantiq.wfms.models.workSites.workSites


import com.google.gson.annotations.SerializedName

data class AttendanceLogs(
    @SerializedName("action")
    val action: String?,
    @SerializedName("approval_status")
    val approvalStatus: Int?
)