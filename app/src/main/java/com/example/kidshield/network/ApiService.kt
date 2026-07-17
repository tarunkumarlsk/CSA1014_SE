package com.example.kidshield.network

import com.example.kidshield.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Authentication ---

    @POST("auth/register/")
    suspend fun registerParent(@Body request: RegisterRequest): Response<Any>

    @POST("auth/login/")
    suspend fun loginParent(@Body request: LoginRequest): Response<LoginResponse>

    @GET("parent/{id}/")
    suspend fun getParentProfile(@Path("id") parentId: Int): Response<ParentProfile>

    @PATCH("parent/{id}/")
    suspend fun updateParentProfile(
        @Path("id") parentId: Int,
        @Body request: UpdateParentProfileRequest
    ): Response<ParentProfile>

    // --- Device Pairing ---

    @POST("pairing/generate/")
    suspend fun generatePairingCode(@Body request: PairingRequest): Response<PairingResponse>

    @POST("pairing/link/")
    suspend fun linkChildDevice(@Body request: LinkDeviceRequest): Response<LinkDeviceResponse>

    @POST("pairing/check/")
    suspend fun checkChildByCode(@Body request: LinkDeviceRequest): Response<CheckChildResponse>

    @PATCH("children/{id}/")
    suspend fun updateChild(
        @Path("id") id: Int,
        @Body request: UpdateChildRequest
    ): Response<com.example.kidshield.models.ChildProfile>

    // --- Tracking & Monitoring ---

    @POST("locations/")
    suspend fun uploadLocation(@Body request: LocationRequest): Response<LocationResponse>

    @GET("locations/")
    suspend fun getLocations(@Query("child_id") childId: Int): Response<List<LocationResponse>>

    @GET("locations/latest/")
    suspend fun getLatestLocation(@Query("child_id") childId: Int): Response<LocationResponse>

    @POST("alerts/")
    suspend fun sendAlert(@Body request: AlertRequest): Response<AlertResponse>

    @GET("alerts/")
    suspend fun getAlerts(@Query("child_id") childId: Int): Response<List<AlertResponse>>

    @PATCH("alerts/{id}/mark-read/")
    suspend fun markAlertRead(@Path("id") alertId: Int): Response<Any>

    @POST("alerts/{id}/grant-time/")
    suspend fun grantTime(@Path("id") alertId: Int, @Body body: Map<String, Int>): Response<Map<String, Any>>

    @PATCH("alerts/mark-all-read/")
    suspend fun markAllAlertsRead(@Body body: Map<String, Int>): Response<Any>

    @POST("screen-usage/")
    suspend fun uploadScreenUsage(@Body request: ScreenUsageRequest): Response<ScreenUsageResponse>

    @GET("screen-usage/")
    suspend fun getScreenUsage(
        @Query("child_id") childId: Int,
        @Query("date") date: String? = null
    ): Response<List<ScreenUsageResponse>>

    @GET("children/")
    suspend fun getChildren(@Query("parent_id") parentId: Int): Response<List<com.example.kidshield.models.ChildProfile>>

    @GET("children/{id}/")
    suspend fun getChildById(@Path("id") childId: Int): Response<com.example.kidshield.models.ChildProfile>

    @DELETE("children/{id}/")
    suspend fun deleteChild(@Path("id") childId: Int): Response<Unit>

    @DELETE("children/{id}/unlink/")
    suspend fun unlinkChild(@Path("id") childId: Int): Response<Map<String, String>>

    @GET("pairing/status/")
    suspend fun checkChildStatus(@Query("child_id") childId: Int): Response<Map<String, Any>>

    // --- Safe Zones ---

    @GET("safe-zones/")
    suspend fun getSafeZones(@Query("child_id") childId: Int): Response<List<SafeZone>>

    @POST("safe-zones/")
    suspend fun createSafeZone(@Body request: SafeZoneRequest): Response<SafeZone>

    @DELETE("safe-zones/{id}/")
    suspend fun deleteSafeZone(@Path("id") id: Int): Response<Unit>

    // --- Timeline ---

    @GET("timeline-events/")
    suspend fun getTimelineEvents(@Query("child_id") childId: Int): Response<List<TimelineEvent>>

    @POST("timeline-events/")
    suspend fun createTimelineEvent(@Body event: TimelineEvent): Response<TimelineEvent>

    // --- Screen Time Limits ---

    @GET("screen-time-limits/")
    suspend fun getScreenTimeLimits(@Query("child_id") childId: Int): Response<List<ScreenTimeLimitResponse>>

    @POST("screen-time-limits/")
    suspend fun setScreenTimeLimit(@Body request: ScreenTimeLimitRequest): Response<ScreenTimeLimitResponse>

    // --- AI Safety Score ---

    @GET("ai-score/")
    suspend fun getAISafetyScore(@Query("child_id") childId: Int): Response<AISafetyScore>
    @POST("screen-usage/bulk-sync/")
    suspend fun syncInventory(@Body request: com.example.kidshield.models.InventorySyncRequest): retrofit2.Response<Void>

    // --- App Blocking (Parental Controls) ---

    @GET("blocked-apps/")
    suspend fun getBlockedApps(@Query("child_id") childId: Int): Response<List<String>>

    @POST("blocked-apps/")
    suspend fun blockApp(@Body body: Map<String, String>): retrofit2.Response<Void>

    @DELETE("blocked-apps/unblock/")
    suspend fun unblockApp(
        @Query("child_id") childId: Int,
        @Query("app_name") appName: String
    ): retrofit2.Response<Void>
}
