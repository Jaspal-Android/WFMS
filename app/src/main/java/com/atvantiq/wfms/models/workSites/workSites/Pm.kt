package com.atvantiq.wfms.models.workSites.workSites


import com.google.gson.annotations.SerializedName

data class Pm(
    @SerializedName("remarks")
    val remarks: String,
    @SerializedName("status")
    val status: Int
)