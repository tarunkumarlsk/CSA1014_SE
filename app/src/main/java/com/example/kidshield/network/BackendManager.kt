package com.example.kidshield.network

import com.example.kidshield.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Kotlin object to bridge Coroutine-based Retrofit calls to Java callbacks.
 */
object BackendManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val apiService get() = RetrofitClient.instance

    // Define generic callback interface for Java to implement
    interface ApiCallback<T> {
        fun onSuccess(result: T)
        fun onError(error: String)
    }

    @JvmStatic
    fun registerParent(email: String, pass: String, name: String?, phone: String?, callback: ApiCallback<String>) {
        scope.launch {
            try {
                val request = RegisterRequest(email, pass, name, phone, "parent")
                val response = apiService.registerParent(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback.onSuccess("Registration Successful")
                    } else {
                        callback.onError("Registration failed: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error: ${e.message}")
                }
            }
        }
    }

    @JvmStatic
    fun loginParent(email: String, pass: String, callback: ApiCallback<LoginResponse>) {
        scope.launch {
            try {
                val request = LoginRequest(email, pass)
                val response = apiService.loginParent(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Login failed: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error: ${e.message}")
                }
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun generatePairingCode(parentId: Int, childId: Int? = null, callback: ApiCallback<PairingResponse>) {
        scope.launch {
            try {
                val request = PairingRequest(parentId, childId)
                val response = apiService.generatePairingCode(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Failed to generate code: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error: ${e.message}")
                }
            }
        }
    }

    @JvmStatic
    fun linkChildDevice(code: String, childName: String, deviceId: String, callback: ApiCallback<LinkDeviceResponse>) {
        scope.launch {
             try {
                 val request = LinkDeviceRequest(code, childName, deviceId, null)
                 val response = apiService.linkChildDevice(request)
                 withContext(Dispatchers.Main) {
                     if (response.isSuccessful && response.body() != null) {
                         callback.onSuccess(response.body()!!)
                     } else {
                         callback.onError("Invalid or expired code")
                     }
                 }
             } catch (e: Exception) {
                 withContext(Dispatchers.Main) {
                     callback.onError("Network error: ${e.message}")
                 }
             }
        }
    }

    @JvmStatic
    fun checkChildByCode(code: String, deviceId: String?, deviceName: String, callback: ApiCallback<CheckChildResponse>) {
        val safeDeviceId = deviceId ?: ""
        scope.launch {
             try {
                 val request = LinkDeviceRequest(code, null, safeDeviceId, deviceName)
                 val response = apiService.checkChildByCode(request)
                 withContext(Dispatchers.Main) {
                     if (response.isSuccessful && response.body() != null) {
                         callback.onSuccess(response.body()!!)
                     } else {
                         val errorBody = response.errorBody()?.string()
                         val errorMessage = if (!errorBody.isNullOrBlank()) {
                             try {
                                 val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                                 json.get("error")?.asString ?: "Server error: \${response.code()}"
                             } catch (e: Exception) { 
                                 "Error: \${response.code()}" 
                             }
                         } else {
                             "Invalid code or device not authorized"
                         }
                         callback.onError(errorMessage)
                     }
                 }
             } catch (e: Exception) {
                 withContext(Dispatchers.Main) {
                     callback.onError("Network error: \${e.message}")
                 }
             }
        }
    }

    @JvmStatic
    fun updateChildDetails(childId: Int, name: String, dob: String, bloodGroup: String, callback: ApiCallback<com.example.kidshield.models.ChildProfile>) {
        scope.launch {
            try {
                val request = UpdateChildRequest(name, dob, bloodGroup)
                val response = apiService.updateChild(childId, request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Failed to update child details")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error: ${e.message}")
                }
            }
        }
    }
    
    @JvmStatic
    fun sendAlert(childId: Int, type: String, message: String, callback: ApiCallback<String>) {
        scope.launch {
            try {
                val request = AlertRequest(childId, type, message)
                val response = apiService.sendAlert(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback.onSuccess("Alert sent")
                    } else {
                        val errorMsg = try { response.errorBody()?.string() ?: "Unknown error" } catch(e: Exception) { "Unknown error" }
                        callback.onError("Failed to send alert (${response.code()}): $errorMsg")
                    }
                }
            } catch (e: Exception) {
                 withContext(Dispatchers.Main) {
                     // Usually silent for alerts
                     callback.onError("Network error")
                 }
            }
        }
    }

    @JvmStatic
    fun uploadLocation(childId: Int, latitude: Double, longitude: Double, callback: ApiCallback<String>) {
        scope.launch {
            try {
                val request = LocationRequest(childId, latitude, longitude)
                val response = apiService.uploadLocation(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback.onSuccess("Location uploaded")
                    } else {
                        callback.onError("Failed to upload location")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error: ${e.message}")
                }
            }
        }
    }

    @JvmStatic
    fun triggerSOS(childId: Int, callback: ApiCallback<String>) {
        scope.launch {
            try {
                val request = AlertRequest(childId, "sos", "SOS Emergency Alert!")
                val response = apiService.sendAlert(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback.onSuccess("SOS Alert Sent")
                    } else {
                        callback.onError("Failed to send SOS")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error")
                }
            }
        }
    }

    @JvmStatic
    fun getLatestLocation(childId: Int, callback: ApiCallback<LocationResponse>) {
        scope.launch {
            try {
                val response = apiService.getLocations(childId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        callback.onSuccess(response.body()!![0]) // Get most recent
                    } else {
                        callback.onError("No location found")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error")
                }
            }
        }
    }

    @JvmStatic
    fun getAlerts(childId: Int, callback: ApiCallback<List<AlertResponse>>) {
        scope.launch {
            try {
                val response = apiService.getAlerts(childId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Failed to fetch alerts")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error")
                }
            }
        }
    }

    @JvmStatic
    fun uploadScreenUsage(childId: Int, appName: String, usageTimeMinutes: Int, date: String, callback: ApiCallback<String>) {
        scope.launch {
            try {
                val request = ScreenUsageRequest(childId, appName, usageTimeMinutes, date)
                val response = apiService.uploadScreenUsage(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback.onSuccess("Screen usage uploaded")
                    } else {
                        callback.onError("Failed to upload screen usage: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error: ${e.message}")
                }
            }
        }
    }

    @JvmStatic
    fun getChildren(parentId: Int, callback: ApiCallback<List<com.example.kidshield.models.ChildProfile>>) {
        scope.launch {
            try {
                val response = apiService.getChildren(parentId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Failed to fetch children")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error")
                }
            }
        }
    }

    @JvmStatic
    fun getSafeZones(childId: Int, callback: ApiCallback<List<SafeZone>>) {
        scope.launch {
            try {
                val response = apiService.getSafeZones(childId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Failed to fetch safe zones")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error")
                }
            }
        }
    }

    @JvmStatic
    fun createSafeZone(childId: Int, name: String, lat: Double, lng: Double, radius: Float, callback: ApiCallback<SafeZone>) {
        scope.launch {
            try {
                val request = SafeZoneRequest(childId, name, lat, lng, radius)
                val response = apiService.createSafeZone(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Failed to create safe zone")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error")
                }
            }
        }
    }

    @JvmStatic
    fun deleteSafeZone(zoneId: Int, callback: ApiCallback<String>) {
        scope.launch {
            try {
                val response = apiService.deleteSafeZone(zoneId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback.onSuccess("Zone deleted")
                    } else {
                        callback.onError("Failed to delete zone")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error")
                }
            }
        }
    }

    // --- New Methods Added for App Controls, Limits, and AI Score ---

    @JvmStatic
    fun getParentProfile(parentId: Int, callback: ApiCallback<ParentProfile>) {
        scope.launch {
            try {
                val response = apiService.getParentProfile(parentId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Failed to fetch profile")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError("Network error") }
            }
        }
    }

    @JvmStatic
    fun updateParentProfile(
        parentId: Int, 
        name: String?, 
        email: String?, 
        phone: String?, 
        homeAddress: String? = null,
        homeLat: Double? = null,
        homeLng: Double? = null,
        callback: ApiCallback<ParentProfile>
    ) {
        scope.launch {
            try {
                val request = UpdateParentProfileRequest(name, email, phone, homeAddress, homeLat, homeLng)
                val response = apiService.updateParentProfile(parentId, request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Failed to update profile: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError("Network error") }
            }
        }
    }

    @JvmStatic
    fun unlinkChild(childId: Int, callback: ApiCallback<String>) {
        scope.launch {
            try {
                val response = apiService.unlinkChild(childId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback.onSuccess(response.body()?.get("message") ?: "Child unlinked")
                    } else {
                        callback.onError("Failed to unlink: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError("Network error") }
            }
        }
    }

    @JvmStatic
    fun getTimelineEvents(childId: Int, callback: ApiCallback<List<TimelineEvent>>) {
        scope.launch {
            try {
                val response = apiService.getTimelineEvents(childId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Failed to fetch timeline")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError("Network error") }
            }
        }
    }


    @JvmStatic
    fun getScreenUsage(childId: Int, callback: ApiCallback<List<ScreenUsageResponse>>) {
        scope.launch {
            try {
                val response = apiService.getScreenUsage(childId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Failed to fetch screen usage")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error: \${e.message}")
                }
            }
        }
    }

    @JvmStatic
    fun getAISafetyScore(childId: Int, callback: ApiCallback<AISafetyScore>) {
        scope.launch {
            try {
                val response = apiService.getAISafetyScore(childId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Failed to fetch AI score")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error: \${e.message}")
                }
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun setScreenTimeLimit(
        childId: Int, 
        appName: String?, 
        limitMinutes: Int, 
        bedtimeEnabled: Boolean = false, 
        schoolHoursEnabled: Boolean = false, 
        bedtimeStart: String? = "21:00",
        bedtimeEnd: String? = "07:00",
        schoolStart: String? = "08:00",
        schoolEnd: String? = "15:00",
        callback: ApiCallback<ScreenTimeLimitResponse>
    ) {
        scope.launch {
            try {
                val request = ScreenTimeLimitRequest(
                    childId, appName, limitMinutes, 
                    bedtimeEnabled, schoolHoursEnabled,
                    bedtimeStart, bedtimeEnd, schoolStart, schoolEnd
                )
                val response = apiService.setScreenTimeLimit(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Failed to set screen time limit")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error: ${e.message}")
                }
            }
        }
    }

    @JvmStatic
    fun getScreenTimeLimits(childId: Int, callback: ApiCallback<List<ScreenTimeLimitResponse>>) {
        scope.launch {
            try {
                val response = apiService.getScreenTimeLimits(childId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Failed to fetch limits")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error: \${e.message}")
                }
            }
        }
    }


    @JvmStatic
    fun markAlertAsRead(alertId: Int, callback: ApiCallback<String>) {
        scope.launch {
            try {
                val response = apiService.markAlertRead(alertId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback.onSuccess("Alert marked read")
                    } else {
                        callback.onError("Failed to mark alert read")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error: \${e.message}")
                }
            }
        }
    }

    @JvmStatic
    fun markAllAlertsAsRead(childId: Int, callback: ApiCallback<String>) {
        scope.launch {
            try {
                val requestBody = mapOf("child_id" to childId)
                val response = apiService.markAllAlertsRead(requestBody)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback.onSuccess("All alerts marked read")
                    } else {
                        callback.onError("Failed to mark all alerts read")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error: ${e.message}")
                }
            }
        }
    }

    @JvmStatic
    fun createTimelineEvent(event: TimelineEvent, callback: ApiCallback<TimelineEvent>) {
        scope.launch {
            try {
                val response = apiService.createTimelineEvent(event)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Failed to create timeline event")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error")
                }
            }
        }
    }

    @JvmStatic
    fun syncAppInventory(childId: Int, apps: List<String>, callback: ApiCallback<String>) {
        if (childId == -1) return
        scope.launch {
            try {
                val request = com.example.kidshield.models.InventorySyncRequest(childId, apps)
                val response = apiService.syncInventory(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback.onSuccess("Inventory synced")
                    } else {
                        callback.onError("Failed to sync inventory")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Network error: \${e.message}")
                }
            }
        }
    }

    @JvmStatic
    fun getBlockedApps(childId: Int, callback: ApiCallback<List<String>>) {
        scope.launch {
            try {
                val response = apiService.getBlockedApps(childId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Failed to fetch blocked apps")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError("Network error: ${e.message}") }
            }
        }
    }

    @JvmStatic
    fun blockApp(childId: Int, appName: String, callback: ApiCallback<String>) {
        scope.launch {
            try {
                val body = mapOf("child_id" to childId.toString(), "app_name" to appName)
                val response = apiService.blockApp(body)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) callback.onSuccess("Blocked")
                    else callback.onError("Failed to block")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError("Network error: ${e.message}") }
            }
        }
    }

    @JvmStatic
    fun unblockApp(childId: Int, appName: String, callback: ApiCallback<String>) {
        scope.launch {
            try {
                val response = apiService.unblockApp(childId, appName)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) callback.onSuccess("Unblocked")
                    else callback.onError("Failed to unblock")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError("Network error: ${e.message}") }
            }
        }
    }

    @JvmStatic
    fun checkChildStatus(childId: Int, callback: ApiCallback<Map<String, Any>>) {
        scope.launch {
            try {
                val response = apiService.checkChildStatus(childId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Failed to check status: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError("Network error: ${e.message}") }
            }
        }
    }
}
