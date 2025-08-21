package com.atvantiq.wfms.models.work.assignedAll


import com.google.gson.annotations.SerializedName

data class Site(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String
)