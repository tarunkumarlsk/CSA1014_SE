package com.example.kidshield.repositories

import com.example.kidshield.models.ChildProfile
import com.example.kidshield.models.LocationResponse
import com.example.kidshield.network.ApiService

class ChildRepository(private val apiService: ApiService) {

    suspend fun getChildren(parentId: Int): List<ChildProfile>? {
        val response = apiService.getChildren(parentId)
        return if (response.isSuccessful) response.body() else null
    }

    /**
     * Fetches only the single latest location for a child via /locations/latest/.
     * Falls back to getting the list and taking the first item if the dedicated endpoint fails.
     */
    suspend fun getLatestLocation(childId: Int): LocationResponse? {
        return try {
            val response = apiService.getLatestLocation(childId)
            if (response.isSuccessful && response.body() != null) {
                response.body()
            } else {
                // Fallback: get list and take first (most recent due to -timestamp ordering)
                val listResponse = apiService.getLocations(childId)
                if (listResponse.isSuccessful && !listResponse.body().isNullOrEmpty()) {
                    listResponse.body()?.firstOrNull()
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun linkChild(code: String, name: String, deviceId: String) =
        apiService.linkChildDevice(com.example.kidshield.models.LinkDeviceRequest(code, name, deviceId))
}
