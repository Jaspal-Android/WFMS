package com.atvantiq.wfms.models.workSites.workSiteDetails


import com.google.gson.annotations.SerializedName

data class Ops(
    @SerializedName("approved_at")
    val approvedAt: Any?,
    @SerializedName("remarks")
    val remarks: Any?,
    @SerializedName("status")
    val status: Int?
)