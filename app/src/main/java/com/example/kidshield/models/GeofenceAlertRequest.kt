package com.example.kidshield.models

import com.google.gson.annotations.SerializedName

data class GeofenceAlertRequest(
    @SerializedName("child") val childId: Int,
    @SerializedName("alert_type") val alertType: String = "geofence",
    @SerializedName("message") val message: String = "Child left safe zone"
)
