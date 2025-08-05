package com.atvantiq.wfms.models.work.selfAssign


import com.google.gson.annotations.SerializedName

data class Circle(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)