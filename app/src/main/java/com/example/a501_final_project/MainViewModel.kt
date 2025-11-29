package com.example.a501_final_project

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
): ViewModel() {
    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _householdData = MutableStateFlow<Map<String, Any>?>(null)
    val householdData: StateFlow<Map<String, Any>?> = _householdData.asStateFlow()

    private val _householdId = MutableStateFlow<String?>(null)
    val householdId: StateFlow<String?> = _householdId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // TODO: get all data for user rather than just id
    fun loadCurrentUserId() {
        _isLoading.value = true
        _errorMessage.value = null

        val id = firestoreRepository.getCurrentUserId()
        _userId.value = id
    }

    /**
     * Load the current user's household data
     */
    fun loadHouseholdData() {
        _isLoading.value = true
        _errorMessage.value = null

        firestoreRepository.getHouseholdWithoutId(
            onSuccess = { householdId, data ->
                _householdId.value = householdId
                _householdData.value = data
                _isLoading.value = false
                Log.d("MainViewModel", "Household loaded: $householdId - ${data["name"]}")
            },
            onFailure = { exception ->
                _errorMessage.value = "Failed to load household: ${exception.message}"
                _isLoading.value = false
                Log.e("MainViewModel", "Error loading household", exception)
            }
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}