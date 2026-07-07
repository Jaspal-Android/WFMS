package com.atvantiq.wfms.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.atvantiq.wfms.BuildConfig
import com.atvantiq.wfms.R
import com.atvantiq.wfms.ui.screens.SplashActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM"
        private const val CHANNEL_ID = "wfms_general_notifications_v2"
        private const val CHANNEL_NAME = "WFMS Notifications"
        private const val CHANNEL_DESC = "WFMS Notifications"
    }

    /**
     * Handles incoming FCM messages and displays notifications.
     * Avoids duplicate notifications if both data and notification payloads are present.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (BuildConfig.DEBUG) Log.d(TAG, "FCM message received")

        // Prefer data payload if present to avoid duplicate notifications
        if (remoteMessage.data.isNotEmpty()) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Message data payload received")
            val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "Notification"
            val message = remoteMessage.data["body"] ?: remoteMessage.data["message"] ?: remoteMessage.notification?.body ?: ""
            showNotification(title, message, remoteMessage.data)
        } else if (remoteMessage.notification != null) {
            val notification = remoteMessage.notification
            if (BuildConfig.DEBUG) Log.d(TAG, "Notification payload received")
            showNotification(
                title = notification?.title ?: "Notification",
                message = notification?.body ?: "",
                data = emptyMap()
            )
        }
    }

    override fun onNewToken(token: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "FCM token refreshed")
        // Optionally send token to your server
    }

    /**
     * Displays a notification with the provided title, message, and data.
     */
    private fun showNotification(title: String, message: String, data: Map<String, String>) {
        createNotificationChannelIfNeeded()

        val intent = Intent(this, SplashActivity::class.java).apply {
            action = SplashActivity.ACTION_PUSH_NOTIFICATION
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            for ((key, value) in data) {
                putExtra(key, value)
            }
        }
        val requestCode = stableNotificationId(data, title, message)
        val pendingIntent = PendingIntent.getActivity(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_24)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(requestCode, notificationBuilder.build())
    }

    /**
     * Creates the notification channel if needed (Android O+).
     */
    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun stableNotificationId(data: Map<String, String>, title: String, message: String): Int {
        val stableKey = data["notification_id"]
            ?: data["id"]
            ?: data["type"]?.let { "$it:$title:$message" }
            ?: "$title:$message"
        return (stableKey.hashCode() and Int.MAX_VALUE).takeIf { it != 0 } ?: 1
    }
}
