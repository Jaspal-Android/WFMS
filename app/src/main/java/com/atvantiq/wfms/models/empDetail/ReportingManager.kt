package com.atvantiq.wfms.models.empDetail


import com.google.gson.annotations.SerializedName

data class ReportingManager(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)