package com.atvantiq.wfms.models.work.startWork


import com.google.gson.annotations.SerializedName

data class StartWorkData(
    @SerializedName("photo_path")
    val photoPath: String,
    @SerializedName("progress_id")
    val progressId: Int,
    @SerializedName("started_at")
    val startedAt: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("work_id")
    val workId: Int
)