package com.atvantiq.wfms.models.type


import com.google.gson.annotations.SerializedName

data class TypeData(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String
){
    override fun toString(): String {
        return name // or companyName, etc.
    }
}