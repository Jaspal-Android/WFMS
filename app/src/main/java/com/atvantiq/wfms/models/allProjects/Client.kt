package com.atvantiq.wfms.models.allProjects


import com.google.gson.annotations.SerializedName

data class Client(
    @SerializedName("added_by")
    val addedBy: Any?,
    @SerializedName("address")
    val address: Any?,
    @SerializedName("alternate_address")
    val alternateAddress: Any?,
    @SerializedName("company_name")
    val companyName: String?,
    @SerializedName("created_at")
    val createdAt: Any?,
    @SerializedName("display_name")
    val displayName: String?,
    @SerializedName("gst_number")
    val gstNumber: Any?,
    @SerializedName("id")
    val id: Long?,
    @SerializedName("is_active")
    val isActive: Any?,
    @SerializedName("state")
    val state: Any?
)