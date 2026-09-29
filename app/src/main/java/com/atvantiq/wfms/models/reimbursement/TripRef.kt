package com.atvantiq.wfms.models.reimbursement

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/** One tracked ride behind an auto-fetched KM travel entry. */
@Parcelize
data class TripRef(
    @SerializedName("start_at")
    val startAt: String?,
    @SerializedName("end_at")
    val endAt: String?
) : Parcelable
