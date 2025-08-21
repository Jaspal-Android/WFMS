package com.atvantiq.wfms.models.work.assignedAll

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Employee(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String
):Parcelable