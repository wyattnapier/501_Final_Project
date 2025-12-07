package com.example.a501_final_project.login_register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.a501_final_project.FirestoreRepository

/**
 * Factory for creating ViewModels with dependencies.
 * This allows us to inject mock repositories during testing.
 */
class HouseholdViewModelFactory(
    private val repository: FirestoreRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HouseholdViewModel::class.java)) {
            return HouseholdViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}