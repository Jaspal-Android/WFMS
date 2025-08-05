package com.atvantiq.wfms.models.work.assignedAll


import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("page")
    val page: Int,
    @SerializedName("page_size")
    val pageSize: Int,
    @SerializedName("records")
    val records: List<WorkRecord>,
    @SerializedName("total_count")
    val totalCount: Int,
    @SerializedName("total_pages")
    val totalPages: Int,
    @SerializedName("total_records")
    val totalRecords: Int
)