package com.atvantiq.wfms.models.workSites.approve


import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("approved_at")
    val approvedAt: String,
    @SerializedName("approver_id")
    val approverId: Long,
    @SerializedName("employee_id")
    val employeeId: Long,
    @SerializedName("id")
    val id: Long,
    @SerializedName("remarks")
    val remarks: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("step")
    val step: String,
    @SerializedName("work_site_id")
    val workSiteId: Long
)