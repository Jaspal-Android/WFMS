package com.atvantiq.wfms.models.attendance.approve

import com.google.gson.annotations.SerializedName

/** `POST /attendance/approve/{id}` answers with the envelope only; `data` is empty. */
data class AttendanceApproveResponse(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("success")
    val success: Boolean?
)
