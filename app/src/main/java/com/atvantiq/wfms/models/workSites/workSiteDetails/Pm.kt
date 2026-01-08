package com.atvantiq.wfms.models.workSites.workSiteDetails


import com.google.gson.annotations.SerializedName

data class Pm(
    @SerializedName("approved_at")
    val approvedAt: String?,
    @SerializedName("remarks")
    val remarks: String?,
    @SerializedName("status")
    val status: Int?
)