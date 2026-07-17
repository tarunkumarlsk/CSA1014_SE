package com.example.kidshield.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.kidshield.network.BackendManager
import com.google.android.gms.location.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var activeChildId: Int = -1 
    
    private val CHANNEL_ID = "LocationServiceChannel"
    private val NOTIFICATION_ID = 12345
    private val safeZones = mutableListOf<com.example.kidshield.models.SafeZone>()
    private val zoneStates = mutableMapOf<Int, Boolean>() // zoneId -> wasInside
    private var lastFetchTime: Long = 0
    private val FETCH_INTERVAL = 5 * 60 * 1000 // 5 minutes

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d("LocationService", "Location update: Lat: ${location.latitude}, Lng: ${location.longitude}")
                    
                    if (activeChildId != -1) {
                        // Periodic Fetch of Safe Zones (every 5 mins)
                        val now = System.currentTimeMillis()
                        if (now - lastFetchTime > FETCH_INTERVAL) {
                            fetchSafeZones()
                        }

                        // Upload location to backend
                        BackendManager.uploadLocation(
                            childId = activeChildId,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            callback = object : BackendManager.ApiCallback<String> {
                                override fun onSuccess(result: String) {
                                    Log.d("LocationService", "Location successfully sent.")
                                }
                                override fun onError(error: String) {
                                    Log.e("LocationService", "Failed to send location: $error")
                                }
                            }
                        )

                        // Check Geofences
                        checkGeofences(location)
                    }
                }
            }
        }
    }

    private fun fetchSafeZones() {
        if (activeChildId == -1) return
        lastFetchTime = System.currentTimeMillis()
        BackendManager.getSafeZones(activeChildId, object : BackendManager.ApiCallback<List<com.example.kidshield.models.SafeZone>> {
            override fun onSuccess(result: List<com.example.kidshield.models.SafeZone>) {
                safeZones.clear()
                safeZones.addAll(result)
                Log.d("LocationService", "Fetched ${result.size} safe zones")
            }
            override fun onError(error: String) {
                Log.e("LocationService", "Failed to fetch safe zones: $error")
            }
        })
    }

    private fun checkGeofences(location: android.location.Location) {
        if (activeChildId == -1 || safeZones.isEmpty()) return
        
        for (zone in safeZones) {
            val distanceResults = FloatArray(1)
            android.location.Location.distanceBetween(
                location.latitude, location.longitude,
                zone.latitude, zone.longitude, distanceResults
            )
            val distance = distanceResults[0]
            val isInsideNow = distance <= zone.radius
            
            // Check if we already know this zone's state
            if (!zoneStates.containsKey(zone.id)) {
                // First time checking this zone - just record state but don't alert
                // unless it is extremely important to know current location immediately
                zoneStates[zone.id] = isInsideNow
                continue
            }

            val wasInside = zoneStates[zone.id] ?: false
            if (wasInside != isInsideNow) {
                // State changed!
                val eventType = if (isInsideNow) "ENTER" else "EXIT"
                val action = if (isInsideNow) "Arrived at" else "Left"
                val message = "Child has ${action.lowercase()} ${zone.name}"
                
                Log.d("LocationService", "Geofence Transition: $message ($eventType)")

                // 1. Send Alert (for Parent Notifications)
                BackendManager.sendAlert(activeChildId, "geofence", message, object : BackendManager.ApiCallback<String> {
                    override fun onSuccess(result: String) {}
                    override fun onError(error: String) {}
                })

                // 2. Create Timeline Event (for Location History)
                val timelineEvent = com.example.kidshield.models.TimelineEvent(
                    id = 0,
                    childId = activeChildId,
                    eventName = "$action ${zone.name}",
                    locationName = zone.name,
                    eventType = eventType,
                    timestamp = "" // Server sets this
                )
                
                BackendManager.createTimelineEvent(timelineEvent, object : BackendManager.ApiCallback<com.example.kidshield.models.TimelineEvent> {
                    override fun onSuccess(result: com.example.kidshield.models.TimelineEvent) {
                        Log.d("LocationService", "Location history updated.")
                    }
                    override fun onError(error: String) {
                        Log.e("LocationService", "Timeline update failed")
                    }
                })
                
                // Update local state
                zoneStates[zone.id] = isInsideNow
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val session = com.example.kidshield.utils.SessionManager.getInstance(this)
        activeChildId = session.childId
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KidShield Tracking")
            .setContentText("Keeping your child safe...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("LocationService", "Cannot start foreground service (probably missing permissions): ${e.message}")
        }
        
        startLocationUpdates()
        fetchSafeZones()
        scanInstalledApps()
        
        return START_STICKY
    }

    private fun scanInstalledApps() {
        if (activeChildId == -1) return
        Thread {
            try {
                val pm = packageManager
                val packages = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
                val appNames = mutableListOf<String>()
                for (appInfo in packages) {
                    if (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0) {
                        appNames.add(pm.getApplicationLabel(appInfo).toString())
                    }
                }
                if (appNames.isNotEmpty()) {
                    BackendManager.syncAppInventory(activeChildId, appNames, object: BackendManager.ApiCallback<String> {
                        override fun onSuccess(result: String) {
                            Log.d("LocationService", "Bulk synced ${appNames.size} apps")
                        }
                        override fun onError(error: String) {
                            Log.e("LocationService", "Bulk sync failed: $error")
                        }
                    })
                }
            } catch (e: Exception) {
                Log.e("LocationService", "Error scanning apps: ${e.message}")
            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(10000) // 10 seconds
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
