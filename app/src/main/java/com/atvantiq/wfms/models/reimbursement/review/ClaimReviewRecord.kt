package com.atvantiq.wfms.models.reimbursement.review

/** A claim on the Claims Approval list (`GET /claim/all`), read leniently by [ClaimReviewPageDeserializer]. */
data class ClaimReviewRecord(
    val claimId: Long?,
    val claimNumber: String?,
    val date: String?,
    val createdAt: String?,
    val employeeId: Long?,
    val employeeName: String?,
    val employeeCode: String?,
    val expenseCategory: String?,
    val totalAmount: Double?,
    var latestApprovedAmount: Double?,
    var status: String?
)

/** One page of the Claims Approval list. */
data class ClaimReviewPage(
    val records: List<ClaimReviewRecord>,
    val totalRecords: Int?
)
