package com.atvantiq.wfms.data.repository.tracking
import com.atvantiq.wfms.models.location.SendLocationResponse
import com.atvantiq.wfms.network.ApiService
import com.google.gson.JsonObject
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TrackingRepo @Inject constructor(private val apiService: ApiService) :ITrackingRepo {

    override suspend fun sendLocation(params: JsonObject): SendLocationResponse  = apiService.sendLocation(
        params = params
    )
}