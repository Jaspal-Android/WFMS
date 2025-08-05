package com.atvantiq.wfms.models.work.selfAssign


import com.google.gson.annotations.SerializedName

data class Result(
    @SerializedName("details")
    val details: Details,
    @SerializedName("message")
    val message: String
)