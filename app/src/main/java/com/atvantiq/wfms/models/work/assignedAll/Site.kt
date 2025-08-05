package com.atvantiq.wfms.models.work.assignedAll


import com.google.gson.annotations.SerializedName

data class Site(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)