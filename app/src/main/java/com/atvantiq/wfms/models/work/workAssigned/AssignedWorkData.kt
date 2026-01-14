package com.atvantiq.wfms.models.work.workAssigned


import com.google.gson.annotations.SerializedName

data class AssignedWorkData(
    @SerializedName("page")
    val page: Int,
    @SerializedName("page_size")
    val pageSize: Int,
    @SerializedName("results")
    val results: List<Site>,
    @SerializedName("total_count")
    val totalCount: Int,
    @SerializedName("total_pages")
    val totalPages: Int,
    @SerializedName("total_records")
    val totalRecords: Int
)