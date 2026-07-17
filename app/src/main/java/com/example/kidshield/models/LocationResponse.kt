package com.example.kidshield.models

import com.google.gson.annotations.SerializedName

data class LocationResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("child") val childId: Int,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timestamp") val timestamp: String
)
