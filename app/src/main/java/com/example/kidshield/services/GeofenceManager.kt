package com.example.kidshield.services

import android.location.Location
import android.util.Log
import com.example.kidshield.network.ApiService
import com.example.kidshield.models.GeofenceAlertRequest
import com.example.kidshield.models.AlertRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SafeZone(
    val latitude: Double,
    val longitude: Double,
    val radius: Float // in meters
)

class GeofenceManager(
    private val apiService: ApiService,
    private val childId: Int
) {
    private var safeZone: SafeZone? = null

    fun setSafeZone(zone: SafeZone) {
        safeZone = zone
    }

    fun onLocationUpdated(currentLocation: Location) {
        val zone = safeZone ?: return

        val results = FloatArray(1)
        Location.distanceBetween(
            currentLocation.latitude, currentLocation.longitude,
            zone.latitude, zone.longitude,
            results
        )
        val distance = results[0]

        if (distance > zone.radius) {
            triggerGeofenceAlert()
        }
    }

    private fun triggerGeofenceAlert() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = GeofenceAlertRequest(
                    childId = childId,
                    alertType = "geofence",
                    message = "Child left safe zone"
                )
                // Map to existing generic AlertRequest format
                val alertReq = AlertRequest(
                    childId = request.childId,
                    alertType = request.alertType,
                    message = request.message
                )
                val response = apiService.sendAlert(alertReq)
                if (response.isSuccessful) {
                    Log.d("GeofenceManager", "Geofence alert sent successfully")
                } else {
                    Log.e("GeofenceManager", "Failed to send geofence alert: \${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("GeofenceManager", "Error sending alert", e)
            }
        }
    }
}
