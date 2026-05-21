package com.atvantiq.wfms.models.empoyeeByCircle


import com.google.gson.annotations.SerializedName

data class EmployeeByCircleResponse(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("data")
    val `data`: List<Data>?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("success")
    val success: Boolean?
)