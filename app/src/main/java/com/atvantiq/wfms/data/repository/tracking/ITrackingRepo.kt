package com.atvantiq.wfms.data.repository.tracking

import com.atvantiq.wfms.models.location.SendLocationResponse
import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.Header

interface ITrackingRepo {
    suspend fun sendLocation(params: JsonObject):SendLocationResponse
}