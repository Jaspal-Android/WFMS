package com.atvantiq.wfms.models.inventory


import com.google.gson.annotations.SerializedName

data class InventoryData(
    @SerializedName("id")       val id: Long,
    @SerializedName("name")     val name: String,
    @SerializedName("unit")     val unit: String,
    @SerializedName("quantity") val availableQuantity: Double
)