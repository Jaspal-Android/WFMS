package com.atvantiq.wfms.models.workSites.workSites


import com.google.gson.annotations.SerializedName

data class Ops(
    @SerializedName("remarks")
    val remarks: String,
    @SerializedName("status")
    val status: Int
)