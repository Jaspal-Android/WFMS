package com.atvantiq.wfms.models.work.selfAssign


import com.google.gson.annotations.SerializedName

data class Site(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("site_id")
    val siteId: String
)