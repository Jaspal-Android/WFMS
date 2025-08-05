package com.atvantiq.wfms.models.type


import com.google.gson.annotations.SerializedName

data class TypeData(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)