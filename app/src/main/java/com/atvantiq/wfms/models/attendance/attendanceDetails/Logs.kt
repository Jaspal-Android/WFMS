package com.atvantiq.wfms.models.attendance.attendanceDetails


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Logs(
    @SerializedName("ADMIN")
    val aDMIN: ADMIN?,
    @SerializedName("OPS")
    val oPS: OPS?,
    @SerializedName("PM")
    val pM: PM?
):Parcelable