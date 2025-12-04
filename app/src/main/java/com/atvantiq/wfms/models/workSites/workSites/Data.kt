package com.atvantiq.wfms.models.workSites.workSites


import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("approval_status")
    val approvalStatus: ApprovalStatus,
    @SerializedName("employee")
    val employee: Employee,
    @SerializedName("work_sites")
    val workSites: List<WorkSite>
)