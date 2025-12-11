package com.atvantiq.wfms.models.loginResponse

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    @SerializedName("email")
    val email: String?,
    @SerializedName("first_name")
    val firstName: String?,
    @SerializedName("last_name")
    val lastName: String?,
    @SerializedName("official_location")
    val officialLocation: OfficialLocation?,
    @SerializedName("permissions")
    val permissions: List<Permission>?,
    @SerializedName("role")
    val role: String?,
    @SerializedName("role_id")
    val roleId: Long?,
    @SerializedName("short_name")
    val shortName: String?,
    @SerializedName("user_id")
    val userId: Long?
) : Parcelable
