package com.atvantiq.wfms.models.activity


import com.google.gson.annotations.SerializedName

data class ActivityData(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String
){
    override fun toString(): String {
        return name // or companyName, etc.
    }
}