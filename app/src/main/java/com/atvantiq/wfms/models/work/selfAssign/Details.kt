package com.atvantiq.wfms.models.work.selfAssign


import com.google.gson.annotations.SerializedName

data class Details(
    @SerializedName("employee")
    val employee: Employee,
    @SerializedName("id")
    val id: Long
)