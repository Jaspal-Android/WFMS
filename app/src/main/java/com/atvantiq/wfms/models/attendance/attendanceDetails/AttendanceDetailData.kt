package com.atvantiq.wfms.models.attendance.attendanceDetails


import com.google.gson.annotations.SerializedName

data class AttendanceDetailData(
    @SerializedName("page")
    val page: Int,
    @SerializedName("page_size")
    val pageSize: Int,
    @SerializedName("records")
    val records: List<Record>,
    @SerializedName("total_count")
    val totalCount: Int,
    @SerializedName("total_pages")
    val totalPages: Int,
    @SerializedName("total_records")
    val totalRecords: Int
)