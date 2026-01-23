package com.atvantiq.wfms.models.reimbursement

data class DAExpense(
    val entryId: String,
    val amount: String,
    val receiptAttachment: String?
)