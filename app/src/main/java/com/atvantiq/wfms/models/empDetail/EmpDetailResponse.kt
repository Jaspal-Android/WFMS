package com.atvantiq.wfms.models.empDetail


import com.google.gson.annotations.SerializedName

data class EmpDetailResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: EmpData,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)