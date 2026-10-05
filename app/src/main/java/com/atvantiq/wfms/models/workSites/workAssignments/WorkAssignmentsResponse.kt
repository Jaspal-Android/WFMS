package com.atvantiq.wfms.models.workSites.workAssignments

import com.google.gson.annotations.SerializedName

/** `GET /work/all`: the work assignments Work Approval lists, page by page. */
data class WorkAssignmentsResponse(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("data")
    val data: WorkAssignmentsPage?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("success")
    val success: Boolean?
)

/**
 * One page. [totalRecords] counts the assignments matching the filters; the open / in-progress /
 * completed tallies are not shown yet.
 */
data class WorkAssignmentsPage(
    @SerializedName("page")
    val page: Int?,
    @SerializedName("page_size")
    val pageSize: Int?,
    @SerializedName("total_records")
    val totalRecords: Int?,
    @SerializedName("total_pages")
    val totalPages: Int?,
    @SerializedName("records")
    val records: List<WorkAssignment>?
)
