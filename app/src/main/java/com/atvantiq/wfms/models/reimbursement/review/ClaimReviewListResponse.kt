package com.atvantiq.wfms.models.reimbursement.review

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

/** `GET /claim/all`: the admin claims list. */
data class ClaimReviewListResponse(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("data")
    @JsonAdapter(ClaimReviewPageDeserializer::class)
    val data: ClaimReviewPage?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("success")
    val success: Boolean?
)
