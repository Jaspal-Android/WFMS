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
import com.atvantiq.wfms.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM"
        private const val CHANNEL_ID = "default_channel_id"
        private const val CHANNEL_NAME = "Default Channel"
        private const val CHANNEL_DESC = "WFMS Notifications"
    }

    /**
     * Handles incoming FCM messages and displays notifications.
     * Avoids duplicate notifications if both data and notification payloads are present.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Prefer data payload if present to avoid duplicate notifications
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "Notification"
            val message = remoteMessage.data["body"] ?: remoteMessage.data["message"] ?: remoteMessage.notification?.body ?: ""
            showNotification(title, message, remoteMessage.data)
        } else if (remoteMessage.notification != null) {
            val notification = remoteMessage.notification
            Log.d(TAG, "Message Notification Body: ${notification?.body}")
            showNotification(
                title = notification?.title ?: "Notification",
                message = notification?.body ?: "",
                data = emptyMap()
            )
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // Optionally send token to your server
    }

    /**
     * Displays a notification with the provided title, message, and data.
     */
    private fun showNotification(title: String, message: String, data: Map<String, String>) {
        createNotificationChannelIfNeeded()

        val intent = Intent(this, getLaunchActivityClass(this)).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // Pass data to activity if needed
            for ((key, value) in data) {
                putExtra(key, value)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use a unique notification ID for each notification
        val notificationId = (System.currentTimeMillis() % 10000).toInt()

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_24)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(notificationId, notificationBuilder.build())
    }

    /**
     * Creates the notification channel if needed (Android O+).
     */
    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Helper to get the launch activity class for PendingIntent.
     */
    private fun getLaunchActivityClass(context: Context): Class<*> {
        val packageName = context.packageName
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        val className = launchIntent?.component?.className
        return Class.forName(className ?: throw IllegalStateException("Launch activity not found"))
    }
}
