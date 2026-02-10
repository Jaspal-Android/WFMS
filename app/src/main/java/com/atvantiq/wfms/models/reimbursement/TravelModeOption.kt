package com.atvantiq.wfms.models.reimbursement

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TravelModeOption(val label: String, val value: String) : Parcelable
