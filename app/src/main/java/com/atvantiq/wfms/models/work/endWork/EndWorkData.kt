package com.atvantiq.wfms.models.work.endWork


import com.google.gson.annotations.SerializedName

data class EndWorkData(
    @SerializedName("ended_at")
    val endedAt: String,
    @SerializedName("ended_latitude")
    val endedLatitude: Double,
    @SerializedName("ended_longitude")
    val endedLongitude: Double,
    @SerializedName("progress_id")
    val progressId: Int,
    @SerializedName("remarks")
    val remarks: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("work_id")
    val workId: Long
)