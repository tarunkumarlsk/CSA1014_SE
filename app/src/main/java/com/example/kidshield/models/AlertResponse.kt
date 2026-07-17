package com.example.kidshield.models

import com.google.gson.annotations.SerializedName

data class AlertResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("child") val childId: Int,
    @SerializedName("alert_type") val alertType: String,
    @SerializedName("message") val message: String,
    @SerializedName("read") var read: Boolean,
    @SerializedName("created_at") val createdAt: String
)
