package com.example.kidshield.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kidshield.models.ChildProfile
import com.example.kidshield.repositories.ChildRepository
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: ChildRepository) : ViewModel() {

    private val _children = MutableLiveData<List<ChildProfile>>()
    val children: LiveData<List<ChildProfile>> = _children

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchChildren(parentId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getChildren(parentId)
                if (result != null) {
                    _children.value = result
                } else {
                    _error.value = "Failed to load children"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
