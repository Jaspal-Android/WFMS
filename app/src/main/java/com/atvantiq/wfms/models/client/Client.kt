package com.atvantiq.wfms.models.client

import com.google.gson.annotations.SerializedName

data class Client(
    @SerializedName("added_by")
    val addedBy: AddedBy,
    @SerializedName("address")
    val address: String,
    @SerializedName("alternate_address")
    val alternateAddress: String?,
    @SerializedName("company_name")
    val companyName: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("gst_number")
    val gstNumber: String,
    @SerializedName("id")
    val id: Long,
    @SerializedName("is_active")
    val isActive: Int,
    @SerializedName("state")
    val state: String
){
    override fun toString(): String {
        return companyName // or companyName, etc.
    }
}