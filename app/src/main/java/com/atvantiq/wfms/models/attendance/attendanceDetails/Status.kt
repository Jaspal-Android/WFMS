package com.atvantiq.wfms.models.attendance.attendanceDetails


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Status(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("label")
    val label: String?
):Parcelable