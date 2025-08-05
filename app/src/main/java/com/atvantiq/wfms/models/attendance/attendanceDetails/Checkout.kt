package com.atvantiq.wfms.models.attendance.attendanceDetails


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Checkout(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("logitude")
    val logitude: Double,
    @SerializedName("time")
    val time: String
):Parcelable