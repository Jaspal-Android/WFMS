package com.atvantiq.wfms.models.workSites


import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("employee")
    val employee: Employee,
    @SerializedName("work_sites")
    val workSites: List<WorkSite>
)