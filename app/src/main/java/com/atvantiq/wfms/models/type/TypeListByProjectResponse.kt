package com.atvantiq.wfms.models.type


import com.google.gson.annotations.SerializedName

data class TypeListByProjectResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: List<TypeData>,
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)