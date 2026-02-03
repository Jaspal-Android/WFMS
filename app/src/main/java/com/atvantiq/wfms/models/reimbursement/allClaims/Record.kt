package com.atvantiq.wfms.models.reimbursement.allClaims


import com.google.gson.annotations.SerializedName

data class Record(
    @SerializedName("amount")
    val amount: Double?,
    @SerializedName("claim_status")
    val claimStatus: String?,
    @SerializedName("date")
    val date: String?,
    @SerializedName("employee")
    val employee: Employee?,
    @SerializedName("site")
    val site: Site?,
    @SerializedName("travelling_from")
    val travellingFrom: String?,
    @SerializedName("travelling_to")
    val travellingTo: String?
)