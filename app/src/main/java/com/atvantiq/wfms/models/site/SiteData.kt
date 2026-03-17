package com.atvantiq.wfms.models.site


import com.google.gson.annotations.SerializedName

data class SiteData(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("site_id")
    val siteId: String,
    @SerializedName("po")
    val po: List<Po>?,
    var selectedPo: Po? = null
){
    override fun toString(): String {
        return name // or companyName, etc.
    }
}