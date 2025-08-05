// app/src/main/java/com/atvantiq/wfms/services/LocationTrackingService.kt
package com.atvantiq.wfms.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.network.ApiService
import com.atvantiq.wfms.ui.screens.SplashActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class LocationTrackingService : Service() {

    companion object {
        private const val CHANNEL_ID = "location_service_channel"
        private const val NOTIFICATION_ID = 12345
        private const val LOCATION_UPDATE_INTERVAL = 10000L // 10 seconds
    }

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var isServiceRunning = false

    @Inject
    lateinit var api: ApiService // Your API service interface

    override fun onCreate() {
        super.onCreate()
        setupService()
    }

    private fun setupService() {
        try {
            createNotificationChannel()
            initializeLocationTracking()
        } catch (e: Exception) {
            Log.e("LocationService", "Error setting up service", e)
            stopSelf()
        }
    }

    private fun initializeLocationTracking() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    if (isServiceRunning) {
                        sendLocationToServer(location)
                    }
                }
            }
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
            isServiceRunning = true
            startLocationUpdates()
            return START_STICKY
        } catch (e: Exception) {
            Log.e("LocationService", "Error starting service", e)
            stopSelf()
            return START_NOT_STICKY
        }
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, SplashActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Tracking Active")
            .setContentText("Your location is being tracked for attendance")
            .setSmallIcon(R.drawable.ic_loc)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Changed to HIGH
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)

        // For older devices
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }


    private fun startLocationUpdates() {
        if (!isServiceRunning) return

        val locationRequest = LocationRequest.Builder(LOCATION_UPDATE_INTERVAL)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        if (checkLocationPermission()) {
            try {
                fusedLocationClient?.requestLocationUpdates(
                    locationRequest,
                    locationCallback ?: return,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                Log.e("LocationService", "Security exception: ${e.message}")
                stopSelf()
            }
        } else {
            Log.e("LocationService", "Location permission not granted")
            stopSelf()
        }
    }

    private fun checkLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun sendLocationToServer(location: Location) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(
                    "LocationTrackingService",
                    "Sending location: ${location.latitude}, ${location.longitude}"
                )
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isServiceRunning = false
        try {
            fusedLocationClient?.removeLocationUpdates(locationCallback ?: return)
            locationCallback = null
            fusedLocationClient = null
        } catch (e: Exception) {
            Log.e("LocationService", "Error cleaning up service", e)
        }
        super.onDestroy()
    }
}