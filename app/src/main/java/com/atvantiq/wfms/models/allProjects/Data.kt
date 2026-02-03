package com.atvantiq.wfms.models.allProjects


import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("columns")
    val columns: List<String?>?,
    @SerializedName("default_columns")
    val defaultColumns: List<String?>?,
    @SerializedName("page")
    val page: Int?,
    @SerializedName("page_size")
    val pageSize: Int?,
    @SerializedName("projects")
    val projects: List<Project>?,
    @SerializedName("total_columns")
    val totalColumns: Int?,
    @SerializedName("total_count")
    val totalCount: Int?,
    @SerializedName("total_pages")
    val totalPages: Int?,
    @SerializedName("total_records")
    val totalRecords: Int?
)