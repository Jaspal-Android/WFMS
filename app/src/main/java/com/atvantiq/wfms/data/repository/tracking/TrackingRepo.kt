package com.atvantiq.wfms.data.repository.tracking
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.atvantiq.wfms.models.location.CustomLocationRequest
import com.atvantiq.wfms.network.ApiService
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TrackingRepo @Inject constructor(private val apiService: ApiService,private  val context: Context) {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private val employeeId = "EMP123" // Replace with actual employee ID


    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    sendLocationToServer(location)
                }
            }
        }
    }



    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(10000) // 10 seconds interval
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        }
    }

    private fun sendLocationToServer(location: Location) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("LocationTrackingService", "Sending location: ${location.latitude}, ${location.longitude}")
                /*api.sendLocation(
                    CustomLocationRequest(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = System.currentTimeMillis()
                    )
                )*/
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient?.removeLocationUpdates(locationCallback!!)
    }
}