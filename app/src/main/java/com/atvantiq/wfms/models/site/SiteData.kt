package com.atvantiq.wfms.models.site


import com.google.gson.annotations.SerializedName

data class SiteData(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("site_id")
    val siteId: String
){
    override fun toString(): String {
        return name // or companyName, etc.
    }
}