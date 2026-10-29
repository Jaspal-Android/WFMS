package com.atvantiq.wfms.models.attendance.attendanceDetails


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/** The site, project or circle an attendance record was worked against. */
@Parcelize
data class NamedRef(
    @SerializedName("id")
    val id: Long?,
    @SerializedName("name")
    val name: String?
) : Parcelable
