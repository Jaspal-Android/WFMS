package com.atvantiq.wfms.models.site


import com.google.gson.annotations.SerializedName

data class Po(
    @SerializedName("id")
    val id: Long?,
    @SerializedName("po_number")
    val poNumber: String?
)