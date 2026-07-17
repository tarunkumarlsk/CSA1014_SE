package com.example.kidshield.repositories

import com.example.kidshield.models.LocationRequest
import com.example.kidshield.models.LocationResponse
import com.example.kidshield.network.RetrofitClient

class LocationRepository {

    private val apiService = RetrofitClient.instance

    /**
     * Uploads the child's current location to the Django backend.
     */
    suspend fun uploadLocation(childId: Int, latitude: Double, longitude: Double): Result<LocationResponse> {
        return try {
            val request = LocationRequest(childId, latitude, longitude)
            val response = apiService.uploadLocation(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to upload location: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the latest locations for a given child from the Django backend.
     */
    suspend fun getChildLocations(childId: Int): Result<List<LocationResponse>> {
        return try {
            val response = apiService.getLocations(childId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch location: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
