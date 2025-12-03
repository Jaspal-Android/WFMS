package com.atvantiq.wfms.models.site.allSites


import com.google.gson.annotations.SerializedName

data class AddedBy(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String
)