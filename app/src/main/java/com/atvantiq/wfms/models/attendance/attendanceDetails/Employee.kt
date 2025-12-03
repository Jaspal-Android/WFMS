package com.atvantiq.wfms.models.attendance.attendanceDetails


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Employee(
    @SerializedName("code")
    val code: String,
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String
):Parcelable