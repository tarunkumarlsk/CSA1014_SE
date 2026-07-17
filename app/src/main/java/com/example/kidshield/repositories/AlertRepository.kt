package com.example.kidshield.repositories

import android.util.Log
import com.example.kidshield.models.AlertResponse
import com.example.kidshield.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlertRepository(
    private val apiService: ApiService,
    private val childId: Int
) {
    private val _alerts = MutableStateFlow<List<AlertResponse>>(emptyList())
    val alerts: StateFlow<List<AlertResponse>> get() = _alerts

    private var isPolling = false
    private val pollingCoroutineScope = CoroutineScope(Dispatchers.IO)

    fun startPolling() {
        if (isPolling) return
        isPolling = true

        pollingCoroutineScope.launch {
            while (isPolling) {
                fetchAlerts()
                // Fetch alerts every 10 seconds
                delay(10 * 1000L)
            }
        }
    }

    fun stopPolling() {
        isPolling = false
    }

    private suspend fun fetchAlerts() {
        try {
            val response = apiService.getAlerts(childId)
            if (response.isSuccessful && response.body() != null) {
                _alerts.value = response.body()!!
            } else {
                Log.e("AlertRepository", "Failed to fetch alerts: \${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("AlertRepository", "Error fetching alerts", e)
        }
    }
}
