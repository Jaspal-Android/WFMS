package com.atvantiq.wfms.models.empDetail


import com.google.gson.annotations.SerializedName

data class Permission(
    @SerializedName("access_levels")
    val accessLevels: List<AccessLevel>,
    @SerializedName("feature_id")
    val featureId: Int,
    @SerializedName("feature_name")
    val featureName: String
)