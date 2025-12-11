package com.example.a501_final_project

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a501_final_project.models.LocalResident
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
): ViewModel() {
    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()
    private val _userData = MutableStateFlow<Map<String, Any>?>(null)
    val userData: StateFlow<Map<String, Any>?> = _userData.asStateFlow()

    private val _householdId = MutableStateFlow<String?>(null)
    val householdId: StateFlow<String?> = _householdId.asStateFlow()
    private val _householdData = MutableStateFlow<Map<String, Any>?>(null)
    val householdData: StateFlow<Map<String, Any>?> = _householdData.asStateFlow()

    private val _isHouseholdDataLoaded = MutableStateFlow(false)
    val isHouseholdDataLoaded: StateFlow<Boolean> = _isHouseholdDataLoaded.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _residents = MutableStateFlow<List<LocalResident>>(emptyList())
    val residents: StateFlow<List<LocalResident>> = _residents

    fun loadUserData() {
        Log.d("MainViewModel", "loadUserData() called")
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Call the new suspend function from the repository
                val (userId, data) = firestoreRepository.getUserWithoutIdSuspend()

                // Update state on success
                _userId.value = userId
                _userData.value = data
                Log.d("MainViewModel", "User information loaded: $userId - ${data["name"]}")

            } catch (exception: Exception) {
                // Update state on failure
                _errorMessage.value = "Failed to load user information: ${exception.message}"
                Log.e("MainViewModel", "Error loading user", exception)
            } finally {
                // This will run whether the try block succeeded or failed
                _isLoading.value = false
            }
        }
    }

    /**
     * Load the current user's household data using suspend functions
     * with retry logic for newly created households
     */
    fun loadHouseholdData(retryCount: Int = 0) {
        Log.d("MainViewModel", "loadHouseholdData() called (attempt ${retryCount + 1})")
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            // Don't reset to false if we're retrying
            if (retryCount == 0) {
                _isHouseholdDataLoaded.value = false
            }

            try {
                // Call the suspend function from the repository
                val (householdId, data) = firestoreRepository.getHouseholdWithoutIdSuspend()

                // Update state on success
                _householdId.value = householdId
                _householdData.value = data

                val residentsFromDb = data["residents"] as? List<Map<String, Any>>
                if (residentsFromDb != null) {
                    val residentDetailsList = mutableListOf<LocalResident>()
                    residentsFromDb.forEach { residentMap ->
                        val residentId = residentMap["id"] as? String
                        if (residentId != null) {
                            try {
                                val userData = firestoreRepository.getUserSuspend(residentId)
                                residentDetailsList.add(
                                    LocalResident(
                                        id = residentId,
                                        name = userData["name"] as? String ?: "Unknown",
                                        venmoUsername = userData["venmoUsername"] as? String ?: "Unknown"
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e("MainViewModel", "Failed to fetch details for resident $residentId", e)
                                // Add a placeholder if a user lookup fails
                                residentDetailsList.add(LocalResident(id = residentId, name = "Unknown User", venmoUsername = "")) // blank should throw an error
                            }
                        }
                    }
                    _residents.value = residentDetailsList
                    Log.d("MainViewModel", "Successfully loaded details for ${_residents.value.size} residents.")
                }

                _isHouseholdDataLoaded.value = true
                Log.d("MainViewModel", "✓ Household loaded successfully: $householdId - ${data["name"]}")

            } catch (exception: Exception) {
                Log.e("MainViewModel", "✗ Error loading household (attempt ${retryCount + 1})", exception)

                // If this is a "no household" error and we haven't retried much, retry
                if (retryCount < 3 && exception.message?.contains("household") == true) {
                    Log.d("MainViewModel", "Retrying household load after 1 second...")
                    delay(1000) // Wait 1 second before retry
                    loadHouseholdData(retryCount + 1)
                } else {
                    // Update state on failure
                    _errorMessage.value = "Failed to load household: ${exception.message}"
                    _isHouseholdDataLoaded.value = false
                }
            } finally {
                // This will run whether the try block succeeded or failed
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}