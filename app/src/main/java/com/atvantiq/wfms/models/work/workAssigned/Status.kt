package com.atvantiq.wfms.models.work.workAssigned


import com.google.gson.annotations.SerializedName

data class Status(
    @SerializedName("code")
    var code: Int,
    @SerializedName("label")
    val label: String
)