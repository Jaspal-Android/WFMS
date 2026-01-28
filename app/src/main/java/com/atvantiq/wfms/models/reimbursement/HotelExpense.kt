package com.atvantiq.wfms.models.reimbursement

data class HotelExpense(
    val entryId: String,
    val amount: String,
    val receiptAttachment: String?
)