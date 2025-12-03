package com.atvantiq.wfms.models.loginResponse


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class AccessLevel(
    @SerializedName("access")
    val access: String,
    @SerializedName("access_id")
    val accessId: Long
):Parcelable