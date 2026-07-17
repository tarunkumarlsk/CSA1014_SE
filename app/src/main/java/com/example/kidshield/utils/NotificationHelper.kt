package com.example.kidshield.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    private val channelId = "KidShield_Alerts"
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "KidShield Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for SOS, Geofences, and Screen Time"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showSOSNotification(message: String = "SOS Emergency Alert!") {
        showNotification("SOS ALERT", message, android.R.drawable.ic_dialog_alert)
    }

    fun showGeofenceNotification(message: String) {
        showNotification("Geofence Alert", message, android.R.drawable.ic_dialog_map)
    }

    fun showScreenTimeNotification(message: String) {
        showNotification("Screen Time Alert", message, android.R.drawable.ic_dialog_info)
    }

    private fun showNotification(title: String, message: String, icon: Int) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
