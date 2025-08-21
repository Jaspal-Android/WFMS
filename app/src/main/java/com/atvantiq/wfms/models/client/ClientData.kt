package com.atvantiq.wfms.models.client


import com.google.gson.annotations.SerializedName

data class ClientData(
    @SerializedName("clients")
    val clients: List<Client>,
    @SerializedName("page")
    val page: Int,
    @SerializedName("page_size")
    val pageSize: Int,
    @SerializedName("total_count")
    val totalCount: Int,
    @SerializedName("total_pages")
    val totalPages: Int,
    @SerializedName("total_records")
    val totalRecords: Int
)