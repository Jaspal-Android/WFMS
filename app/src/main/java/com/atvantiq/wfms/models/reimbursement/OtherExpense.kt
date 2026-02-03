package com.atvantiq.wfms.models.reimbursement

data class OtherExpense(
    val entryId: String,
    val category: String,
    val amount: String,
    val receiptAttachments: List<String> = emptyList()
)
