package com.atvantiq.wfms.models.empDetail


import com.google.gson.annotations.SerializedName

data class AccessLevel(
    @SerializedName("access")
    val access: String,
    @SerializedName("access_id")
    val accessId: Long
)