package com.example.kidshield.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.kidshield.ParentMainActivity
import com.example.kidshield.R
import com.example.kidshield.models.AlertResponse
import com.example.kidshield.network.BackendManager
import com.example.kidshield.utils.SessionManager
import kotlinx.coroutines.*

/**
 * Service for Parent app to poll for alerts from children in the background.
 */
class ParentAlertService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var isPolling = false
    private val CHANNEL_ID = "parent_alerts_channel"
    private val NOTIFICATION_ID = 900
    private var lastAlertId = -1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createPersistentNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isPolling) {
            isPolling = true
            startPolling()
        }
        return START_STICKY
    }

    private fun startPolling() {
        serviceScope.launch {
            while (isPolling) {
                try {
                    val parentId = SessionManager.getInstance(applicationContext).parentId
                    if (parentId != -1) {
                        pollAlerts(parentId)
                    }
                } catch (e: Exception) {
                    Log.e("ParentAlertService", "Polling error: \${e.message}")
                }
                delay(30000) // Poll every 30 seconds
            }
        }
    }

    private fun pollAlerts(parentId: Int) {
        BackendManager.getChildren(parentId, object : BackendManager.ApiCallback<List<com.example.kidshield.models.ChildProfile>> {
            override fun onSuccess(children: List<com.example.kidshield.models.ChildProfile>) {
                children.forEach { child ->
                    BackendManager.getAlerts(child.id, object : BackendManager.ApiCallback<List<AlertResponse>> {
                        override fun onSuccess(alerts: List<AlertResponse>) {
                            val unreadAlerts = alerts.filter { !it.read }
                            if (unreadAlerts.isNotEmpty()) {
                                handleNewAlerts(unreadAlerts)
                            }
                        }
                        override fun onError(error: String) {}
                    })
                }
            }
            override fun onError(error: String) {}
        })
    }

    private fun handleNewAlerts(alerts: List<AlertResponse>) {
        val mostRecent = alerts.maxByOrNull { it.id } ?: return
        if (mostRecent.id <= lastAlertId) return
        
        lastAlertId = mostRecent.id
        showNotification(mostRecent)
    }

    private fun showNotification(alert: AlertResponse) {
        val intent = Intent(this, ParentMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("KidShield Alert: \${alert.alertType.replace('_', ' ').capitalize()}")
            .setContentText(alert.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(alert.id, notification)
    }

    private fun createPersistentNotification(): Notification {
        val intent = Intent(this, ParentMainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("KidShield Protection Active")
            .setContentText("Monitoring your child's safety...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Safety Alerts"
            val descriptionText = "Notifications for SOS, geofence, and time requests"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                
                // Ensure default sound uri is set for the channel
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()
                setSound(soundUri, audioAttributes)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
