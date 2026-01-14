package com.atvantiq.wfms.models.attendance.attendanceDetails


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class OPS(
    @SerializedName("is_approved")
    val isApproved: Boolean?,
    @SerializedName("remarks")
    val remarks: String?,
    @SerializedName("status")
    val status: Int?
):Parcelable