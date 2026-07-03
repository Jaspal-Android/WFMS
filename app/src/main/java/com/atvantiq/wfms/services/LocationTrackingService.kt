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
import com.atvantiq.wfms.BuildConfig
import com.atvantiq.wfms.R
import com.atvantiq.wfms.data.prefs.PrefKeys
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.tracking.ITrackingRepo
import com.atvantiq.wfms.data.tracking.LocationEventQueue
import com.atvantiq.wfms.data.tracking.QueuedLocationEvent
import com.atvantiq.wfms.network.ApiService
import com.atvantiq.wfms.ui.screens.SplashActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class LocationTrackingService : Service() {

    companion object {
        const val CHANNEL_ID = "location_service_channel_v2"
        const val NOTIFICATION_ID = 12345
        private const val LOCATION_UPDATE_INTERVAL = 15 * 60 * 1000L
    }

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var isServiceRunning = false
    private var isUpdatingLocation = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var api: ApiService // Your API service interface

    @Inject
    lateinit var trackingService: ITrackingRepo

    @Inject
    lateinit var prefMain: SecurePrefMain

    @Inject
    lateinit var locationEventQueue: LocationEventQueue

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
            if (!prefMain.get(PrefKeys.IS_TRACKING_ACTIVE, false)) {
                stopSelf()
                return START_NOT_STICKY
            }
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
        val notificationIntent = Intent(this, SplashActivity::class.java).apply {
            action = SplashActivity.ACTION_LOCATION_NOTIFICATION
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.location_tracking_active))
            .setContentText(getString(R.string.your_location_tracked))
            .setSmallIcon(R.drawable.ic_loc)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)

        // For older devices
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            channel.enableLights(false)
            channel.enableVibration(false)
            channel.setSound(null, null)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }


    private fun startLocationUpdates() {
        if (!isServiceRunning || isUpdatingLocation) return

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
                isUpdatingLocation = true
            } catch (e: SecurityException) {
                Log.e("LocationService", "Security exception: ${e.message}")
                stopSelf()
            }
        } else {
            Log.e("LocationService", "Location permission not granted")
            stopSelf()
        }
    }

    private fun checkLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun sendLocationToServer(location: Location) {
        serviceScope.launch {
            locationEventQueue.enqueue(
                QueuedLocationEvent(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    recordedAtMillis = System.currentTimeMillis(),
                    accuracyMeters = if (location.hasAccuracy()) location.accuracy else null
                )
            )
            flushQueuedLocations()
        }
    }

    private suspend fun flushQueuedLocations() {
        val queuedLocations = locationEventQueue.peekAll()
        var syncedCount = 0

        for (event in queuedLocations) {
            try {
                val params = JsonObject().apply {
                    addProperty("latitude", event.latitude)
                    addProperty("longitude", event.longitude)
                }
                trackingService.sendLocation(params)
                syncedCount++
            } catch (e: Exception) {
                if (syncedCount > 0) {
                    locationEventQueue.removeSynced(syncedCount)
                }
                Log.e("LocationService", "Queued location sync paused after failure", e)
                return
            }
        }

        if (syncedCount > 0) {
            locationEventQueue.removeSynced(syncedCount)
            if (BuildConfig.DEBUG) {
                Log.d("LocationService", "Synced $syncedCount queued location event(s)")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isServiceRunning = false
        isUpdatingLocation = false
        try {
            locationCallback?.let { callback ->
                fusedLocationClient?.removeLocationUpdates(callback)
            }
            locationCallback = null
            fusedLocationClient = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            serviceScope.cancel()
        } catch (e: Exception) {
            Log.e("LocationService", "Error cleaning up service", e)
        }
        super.onDestroy()
    }
}
