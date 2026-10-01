package com.atvantiq.wfms.models.reimbursement.review

import com.google.gson.annotations.SerializedName

/** `POST /claim/approve` answers with the envelope only; `data` is empty. */
data class ClaimApproveResponse(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("success")
    val success: Boolean?
)
