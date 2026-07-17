package com.example.kidshield.models

import com.google.gson.annotations.SerializedName

// --- Authentication Models ---

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("role") val role: String = "parent"
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("role") val role: String,
    @SerializedName("name") val name: String,
    @SerializedName("message") val message: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone_number") val phoneNumber: String? = null
)

// --- Device Pairing Models ---

data class PairingRequest(
    @SerializedName("parent_id") val parentId: Int,
    @SerializedName("child_id") val childId: Int? = null
)

data class PairingResponse(
    @SerializedName("code") val code: String,
    @SerializedName("expires_at") val expiresAt: String
)

data class LinkDeviceRequest(
    @SerializedName("code") val code: String,
    @SerializedName("child_name") val childName: String?,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_name") val deviceName: String? = null
)

data class LinkDeviceResponse(
    @SerializedName("child_id") val childId: Int,
    @SerializedName("message") val message: String
)

data class UpdateChildRequest(
    @SerializedName("name") val name: String,
    @SerializedName("dob") val dob: String,
    @SerializedName("blood_group") val bloodGroup: String
)

// --- Tracking & Monitoring Models ---

data class LocationRequest(
    @SerializedName("child") val childId: Int,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

data class AlertRequest(
    @SerializedName("child") val childId: Int,
    @SerializedName("alert_type") val alertType: String, // "sos", "geofence", "screen_time", "app_misuse"
    @SerializedName("message") val message: String
)

data class ScreenUsageResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("child") val childId: Int,
    @SerializedName("app_name") val appName: String,
    @SerializedName("usage_time") val usageTimeMinutes: Int,
    @SerializedName("date") val date: String
)

// --- Safe Zones Models ---

data class SafeZone(
    @SerializedName("id") val id: Int,
    @SerializedName("parent") val parentId: Int?,
    @SerializedName("child") val childId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("radius") val radius: Float
)

data class SafeZoneRequest(
    @SerializedName("child") val childId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("radius") val radius: Float
)

// --- AI Safety Score ---

data class AISafetyScore(
    @SerializedName("child_id") val childId: String,
    @SerializedName("score") val score: Int,
    @SerializedName("status") val status: String,  // "Safe", "Warning", "Danger"
    @SerializedName("color") val color: String
)

// --- Screen Time Limits ---

data class ScreenTimeLimitRequest(
    @SerializedName("child") val childId: Int,
    @SerializedName("app_name") val appName: String?,  // null = overall daily limit
    @SerializedName("limit_minutes") val limitMinutes: Int,
    @SerializedName("bedtime_enabled") val bedtimeEnabled: Boolean = false,
    @SerializedName("school_hours_enabled") val schoolHoursEnabled: Boolean = false,
    @SerializedName("bedtime_start") val bedtimeStart: String? = "21:00",
    @SerializedName("bedtime_end") val bedtimeEnd: String? = "07:00",
    @SerializedName("school_start") val schoolStart: String? = "08:00",
    @SerializedName("school_end") val schoolEnd: String? = "15:00"
)

data class ScreenTimeLimitResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("child") val childId: Int,
    @SerializedName("app_name") val appName: String?,
    @SerializedName("limit_minutes") val limitMinutes: Int,
    @SerializedName("bedtime_enabled") val bedtimeEnabled: Boolean = false,
    @SerializedName("school_hours_enabled") val schoolHoursEnabled: Boolean = false,
    @SerializedName("bedtime_start") val bedtimeStart: String? = "21:00",
    @SerializedName("bedtime_end") val bedtimeEnd: String? = "07:00",
    @SerializedName("school_start") val schoolStart: String? = "08:00",
    @SerializedName("school_end") val schoolEnd: String? = "15:00"
)

data class ParentProfile(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone_number") val phone: String? = null,
    @SerializedName("home_address") val homeAddress: String? = null,
    @SerializedName("home_latitude") val homeLatitude: Double? = null,
    @SerializedName("home_longitude") val homeLongitude: Double? = null
)

data class UpdateParentProfileRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone_number") val phone: String? = null,
    @SerializedName("home_address") val homeAddress: String? = null,
    @SerializedName("home_latitude") val homeLatitude: Double? = null,
    @SerializedName("home_longitude") val homeLongitude: Double? = null
)

data class CheckChildResponse(
    @SerializedName("child_id") val childId: Int,
    @SerializedName("is_new") val isNew: Boolean,
    @SerializedName("child_name") val childName: String?,
    @SerializedName("device_name") val deviceName: String?
)

data class TimelineEvent(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("child") val childId: Int,
    @SerializedName("event_type") val eventType: String,
    @SerializedName("event_name") val eventName: String,
    @SerializedName("location_name") val locationName: String,
    @SerializedName("timestamp") val timestamp: String = ""
)

data class InventorySyncRequest(
    @SerializedName("child_id") val childId: Int,
    @SerializedName("apps") val apps: List<String>
)
