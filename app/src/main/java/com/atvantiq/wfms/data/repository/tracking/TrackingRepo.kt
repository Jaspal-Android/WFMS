package com.atvantiq.wfms.data.repository.tracking
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.models.location.SendLocationResponse
import com.atvantiq.wfms.network.ApiService
import com.google.gson.JsonObject
import com.atvantiq.wfms.data.prefs.PrefKeys
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TrackingRepo @Inject constructor(private val apiService: ApiService, private val prefMain: SecurePrefMain) :ITrackingRepo {

    override suspend fun sendLocation(params: JsonObject): SendLocationResponse  = apiService.sendLocation(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        params = params
    )
}