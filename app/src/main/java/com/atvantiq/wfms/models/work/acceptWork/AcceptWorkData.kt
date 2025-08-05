package com.atvantiq.wfms.models.work.acceptWork


import com.google.gson.annotations.SerializedName

data class AcceptWorkData(
    @SerializedName("status")
    val status: String,
    @SerializedName("work_id")
    val workId: Int
)