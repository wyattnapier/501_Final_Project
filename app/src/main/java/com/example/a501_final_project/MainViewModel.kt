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

    // Initialize user ID when ViewModel is created
    init {
        loadCurrentUserId()
    }

    fun loadCurrentUserId() {
        val id = firestoreRepository.getCurrentUserId()
        _userId.value = id
        Log.d("MainViewModel", "User ID: $id")
    }
}