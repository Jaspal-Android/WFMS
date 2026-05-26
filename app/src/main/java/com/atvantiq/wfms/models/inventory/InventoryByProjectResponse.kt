package com.atvantiq.wfms.models.inventory


import com.google.gson.annotations.SerializedName

data class InventoryByProjectResponse(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("data")
    val `data`: List<InventoryData>?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("success")
    val success: Boolean?
)