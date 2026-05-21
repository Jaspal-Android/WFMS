package com.atvantiq.wfms.models.reimbursement.allClaims


import com.google.gson.annotations.SerializedName

data class Status(
    @SerializedName("code")
    val code: String?
)