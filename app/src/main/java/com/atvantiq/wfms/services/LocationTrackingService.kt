// app/src/main/java/com/atvantiq/wfms/services/LocationTrackingService.kt
package com.atvantiq.wfms.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.atvantiq.wfms.BuildConfig
import com.atvantiq.wfms.R
import com.atvantiq.wfms.data.prefs.PrefKeys
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.tracking.ITrackingRepo
import com.atvantiq.wfms.data.tracking.LocationEventQueue
import com.atvantiq.wfms.data.tracking.QueuedLocationEvent
import com.atvantiq.wfms.data.tracking.ShiftTracker
import com.atvantiq.wfms.data.tracking.TrackPoint
import com.atvantiq.wfms.network.ApiService
import com.atvantiq.wfms.ui.screens.SplashActivity
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.utils.ThemeManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject


@AndroidEntryPoint
class LocationTrackingService : Service() {

    companion object {
        const val CHANNEL_ID = "location_service_channel_v2"
        const val NOTIFICATION_ID = 12345
        private const val LOCATION_UPDATE_INTERVAL = 15 * 60 * 1000L

        /** Pause 15 min / Resume, from the notification or the dashboard card. */
        const val ACTION_PAUSE = "com.atvantiq.wfms.action.PAUSE_TRACKING"
        const val ACTION_RESUME = "com.atvantiq.wfms.action.RESUME_TRACKING"

        private const val PAUSE_REQUEST_CODE = 1
        private const val RESUME_REQUEST_CODE = 2

        /** Asks the running service to pause or resume; the shared path for every Pause/Resume button. */
        fun sendAction(context: Context, action: String) {
            context.startService(Intent(context, LocationTrackingService::class.java).setAction(action))
        }
    }

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var isServiceRunning = false
    private var isUpdatingLocation = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Serializes enqueue + flush so overlapping location callbacks can't re-send the
    // same event or drop an unsynced one via the front-count removal.
    private val syncMutex = Mutex()

    @Inject
    lateinit var api: ApiService // Your API service interface

    @Inject
    lateinit var trackingService: ITrackingRepo

    @Inject
    lateinit var prefMain: SecurePrefMain

    @Inject
    lateinit var locationEventQueue: LocationEventQueue

    @Inject
    lateinit var shiftTracker: ShiftTracker

    private val mainHandler = Handler(Looper.getMainLooper())

    /** Ends the pause when its 15 minutes are up, even if no fix arrives. */
    private val autoResume = Runnable {
        shiftTracker.refreshPause()
        updateNotification()
    }

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
            when (intent?.action) {
                ACTION_PAUSE -> shiftTracker.pause()
                ACTION_RESUME -> shiftTracker.resume()
            }
            startForeground(NOTIFICATION_ID, buildNotification())
            scheduleAutoResume()
            isServiceRunning = true
            startLocationUpdates()
            return START_STICKY
        } catch (e: Exception) {
            Log.e("LocationService", "Error starting service", e)
            stopSelf()
            return START_NOT_STICKY
        }
    }

    /**
     * The shift notification: elapsed shift time, since when, locations accepted and km (or when a
     * pause ends), with a Pause 15 min / Resume action. Silent, ongoing, no Stop action.
     */
    private fun buildNotification(): Notification {
        val content = ShiftNotificationContent.of(shiftTracker.state.value, System.currentTimeMillis())
        val themed = ContextThemeWrapper(this, ThemeManager.getCurrentTheme(this).styleRes)
        val accent = ThemeManager.resolveColor(
            themed, if (content.isPaused) R.attr.wfmsColorWarning else R.attr.wfmsColorPrimary
        )
        val text = if (content.isPaused) {
            getString(R.string.shift_notification_paused_text, timeText(content.pausedUntilMillis))
        } else {
            getString(
                R.string.shift_notification_active_text,
                timeText(content.chronometerBaseMillis),
                resources.getQuantityString(R.plurals.locations_count, content.locations, content.locations),
                content.distanceKm
            )
        }
        val action = if (content.isPaused) ACTION_RESUME else ACTION_PAUSE
        val actionIntent = PendingIntent.getService(
            this,
            if (content.isPaused) RESUME_REQUEST_CODE else PAUSE_REQUEST_CODE,
            Intent(this, LocationTrackingService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(content.title))
            .setContentText(text)
            .setSmallIcon(content.smallIcon)
            .setColor(accent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(openDashboardIntent())
            .addAction(content.actionIcon, getString(content.actionTitle), actionIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)

        content.chronometerBaseMillis?.let { checkIn ->
            builder.setWhen(checkIn).setShowWhen(true).setUsesChronometer(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    private fun openDashboardIntent(): PendingIntent {
        val notificationIntent = Intent(this, SplashActivity::class.java).apply {
            action = SplashActivity.ACTION_LOCATION_NOTIFICATION
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun timeText(millis: Long?): String =
        millis?.let { DateUtils.formatShortTime(it) } ?: getString(R.string.not_available)

    private fun updateNotification() {
        if (!isServiceRunning) return
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun scheduleAutoResume() {
        mainHandler.removeCallbacks(autoResume)
        val pausedUntil = shiftTracker.state.value.pausedUntilMillis ?: return
        mainHandler.postDelayed(autoResume, (pausedUntil - System.currentTimeMillis()).coerceAtLeast(0L))
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
            syncMutex.withLock {
                val event = QueuedLocationEvent(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    recordedAtMillis = location.time.takeIf { it > 0 } ?: System.currentTimeMillis(),
                    accuracyMeters = if (location.hasAccuracy()) location.accuracy else null
                )
                // During a pause fixes are discarded (not queued, not uploaded); the service keeps running.
                val accepted = shiftTracker.recordFix(
                    TrackPoint(event.latitude, event.longitude, event.recordedAtMillis, event.accuracyMeters?.toDouble())
                )
                if (accepted) {
                    locationEventQueue.enqueue(event)
                    flushQueuedLocations()
                }
                updateNotification()
            }
        }
    }

    private suspend fun flushQueuedLocations() {
        val queuedLocations = locationEventQueue.peekAll()
        var syncedCount = 0

        for (event in queuedLocations) {
            try {
                trackingService.sendLocation(event.toUploadParams())
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
        mainHandler.removeCallbacks(autoResume)
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
