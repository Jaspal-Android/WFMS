package com.atvantiq.wfms.models.empoyeeByCircle


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Data(
    @SerializedName("code")
    val code: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("id")
    val id: Long?,
    @SerializedName("name")
    val name: String?
):Parcelable