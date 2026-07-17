package com.example.kidshield.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kidshield.models.LocationResponse
import com.example.kidshield.repositories.ChildRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapViewModel(private val repository: ChildRepository) : ViewModel() {

    private val _location = MutableLiveData<LocationResponse?>()
    val location: LiveData<LocationResponse?> = _location

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var pollingJob: Job? = null

    // Track last known coords to avoid re-rendering identical points
    private var lastLat: Double = Double.NaN
    private var lastLng: Double = Double.NaN

    fun startLocationPolling(childId: Int) {
        // Cancel any existing job (e.g. when child selection changes)
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                try {
                    val result = repository.getLatestLocation(childId)
                    if (result != null) {
                        // Only emit if coords actually changed (prevents alternating flicker)
                        if (result.latitude != lastLat || result.longitude != lastLng) {
                            lastLat = result.latitude
                            lastLng = result.longitude
                            _location.value = result
                        }
                    }
                } catch (e: Exception) {
                    _error.value = "Error fetching location: ${e.message}"
                }
                delay(8000) // Poll every 8s — fast enough, slow enough to avoid flicker
            }
        }
    }

    fun stopLocationPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationPolling()
    }
}
