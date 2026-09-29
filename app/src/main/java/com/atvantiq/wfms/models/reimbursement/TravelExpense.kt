package com.atvantiq.wfms.models.reimbursement

import android.os.Parcelable
import com.atvantiq.wfms.models.empoyeeByCircle.Data
import kotlinx.parcelize.Parcelize

@Parcelize
data class TravelExpense(
    val mode: TravelModeOption?,
    val amount: String,
    val travelingWith: List<Data>? = emptyList(),
    val from: String,
    val to: String,
    val receiptAttachments: List<String> = emptyList(),
    /* Set by the server for auto-fetched KM trips; sent back unchanged when editing. */
    val distanceKm: Double? = null,
    val distanceSource: String? = null,
    val tripRefs: List<TripRef>? = null
):Parcelable