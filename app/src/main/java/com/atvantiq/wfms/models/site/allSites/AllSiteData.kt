package com.atvantiq.wfms.models.site.allSites


import com.google.gson.annotations.SerializedName

data class AllSiteData(
    @SerializedName("page")
    val page: Int,
    @SerializedName("page_size")
    val pageSize: Int,
    @SerializedName("sites")
    val sites: List<Site>,
    @SerializedName("total_count")
    val totalCount: Int,
    @SerializedName("total_pages")
    val totalPages: Int,
    @SerializedName("total_records")
    val totalRecords: Int
)