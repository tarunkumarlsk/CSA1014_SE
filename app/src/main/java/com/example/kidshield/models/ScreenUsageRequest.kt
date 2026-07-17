package com.example.kidshield.models

import com.google.gson.annotations.SerializedName

data class ScreenUsageRequest(
    @SerializedName("child") val childId: Int,
    @SerializedName("app_name") val appName: String,
    @SerializedName("usage_time") val usageTimeMinutes: Int,
    @SerializedName("date") val date: String
)
