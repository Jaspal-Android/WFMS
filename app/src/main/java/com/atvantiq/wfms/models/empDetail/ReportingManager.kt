package com.atvantiq.wfms.models.empDetail


import com.google.gson.annotations.SerializedName

data class ReportingManager(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String
)