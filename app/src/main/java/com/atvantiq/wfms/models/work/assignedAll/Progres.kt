package com.atvantiq.wfms.models.work.assignedAll


import com.google.gson.annotations.SerializedName

data class Progres(
    @SerializedName("ended_at")
    val endedAt: String,
    @SerializedName("ended_latitude")
    val endedLatitude: Double?,
    @SerializedName("ended_longitude")
    val endedLongitude: Double?,
    @SerializedName("id")
    val id: Long,
    @SerializedName("photo_path")
    val photoPath: String,
    @SerializedName("remarks")
    val remarks: String,
    @SerializedName("start_latitude")
    val startLatitude: Double?,
    @SerializedName("start_longitude")
    val startLongitude: Double?,
    @SerializedName("started_at")
    val startedAt: String,
    @SerializedName("status")
    val status: String
)