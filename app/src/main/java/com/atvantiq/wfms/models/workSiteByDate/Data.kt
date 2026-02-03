package com.atvantiq.wfms.models.workSiteByDate


import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("date")
    val date: String?,
    @SerializedName("sites")
    val sites: List<Site>?
)