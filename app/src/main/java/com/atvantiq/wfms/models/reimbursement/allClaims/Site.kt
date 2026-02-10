package com.atvantiq.wfms.models.reimbursement.allClaims


import com.google.gson.annotations.SerializedName

data class Site(
    @SerializedName("site_code")
    val siteCode: String?,
    @SerializedName("site_id")
    val siteId: Long?,
    @SerializedName("site_name")
    val siteName: String?
)