package com.atvantiq.wfms.data.repository.tracking
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.models.location.CustomLocationRequest
import com.atvantiq.wfms.models.location.SendLocationResponse
import com.atvantiq.wfms.network.ApiService
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.JsonObject
import com.ssas.jibli.data.prefs.PrefKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TrackingRepo @Inject constructor(private val apiService: ApiService, private val prefMain: SecurePrefMain) :ITrackingRepo {

    override suspend fun sendLocation(params: JsonObject): SendLocationResponse  = apiService.sendLocation(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        params = params
    )
}