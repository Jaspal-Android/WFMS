package com.atvantiq.wfms.models.loginResponse


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    @SerializedName("email")
    val email: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("short_name")
    val shortName: String,
    @SerializedName("user_id")
    val userId: Int
):Parcelable