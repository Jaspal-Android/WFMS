package com.atvantiq.wfms.models.loginResponse


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Permission(
    @SerializedName("access_levels")
    val accessLevels: List<AccessLevel>,
    @SerializedName("feature_id")
    val featureId: Long,
    @SerializedName("feature_name")
    val featureName: String
):Parcelable