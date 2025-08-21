package com.atvantiq.wfms.models.po


import com.google.gson.annotations.SerializedName

data class PoData(
    @SerializedName("client_id")
    val clientId: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("id")
    val id: Long,
    @SerializedName("location")
    val location: String,
    @SerializedName("po_date")
    val poDate: String,
    @SerializedName("po_number")
    val poNumber: String,
    @SerializedName("project_id")
    val projectId: Int,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("valid_from")
    val validFrom: String,
    @SerializedName("valid_to")
    val validTo: String
){
    override fun toString(): String {
        return poNumber // or companyName, etc.
    }
}