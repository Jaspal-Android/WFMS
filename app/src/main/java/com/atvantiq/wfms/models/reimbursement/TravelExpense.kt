package com.atvantiq.wfms.models.reimbursement

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TravelExpense(
    val mode: String,
    val amount: String,
    val travelingWith: String?,
    val from: String,
    val to: String,
    val receiptAttachment: String?
):Parcelable