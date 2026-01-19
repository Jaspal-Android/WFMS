package com.atvantiq.wfms.models.workSites.workSiteDetails


import com.google.gson.annotations.SerializedName

data class Status(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("label")
    val label: String?
)