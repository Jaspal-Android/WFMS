package com.atvantiq.wfms.models.reimbursement

data class DAExpense(
    val entryId: String,
    val amount: String,
    val receiptAttachments: List<String> = emptyList()

)