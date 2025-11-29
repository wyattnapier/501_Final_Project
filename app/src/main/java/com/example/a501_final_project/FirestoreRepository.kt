package com.example.a501_final_project

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Get the current authenticated user's ID
     * @return String? the user ID, or null if not logged in
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Get household information from Firestore
     * @param householdID: String, the ID of the household to fetch
     * @param onSuccess: (Map<String, Any>) -> Unit, a function to call on success
     * @param onFailure: (Exception) -> Unit, a function to call on failure
     */
    fun getHousehold(
        householdID: String,
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("households").document(householdID)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    onSuccess(document.data ?: emptyMap())
                } else {
                    onFailure(Exception("Household not found"))
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreRepository", "Error getting household", exception)
                onFailure(exception)
            }
    }

    /**
     * Get user information from Firestore
     * @param userId: String, the ID of the user to fetch
     * @param onSuccess: (Map<String, Any>) -> Unit, a function to call on success
     * @param onFailure: (Exception) -> Unit, a function to call on failure
     */
    fun getUser(
        userId: String,
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    onSuccess(document.data ?: emptyMap())
                } else {
                    onFailure(Exception("User not found"))
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreRepository", "Error getting user", exception)
                onFailure(exception)
            }
    }

    fun getUserWithoutId(
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserId = getCurrentUserId()
        if (currentUserId != null) {
            val user = getUser(
                userId = currentUserId,
                onSuccess = onSuccess,
                onFailure = onFailure
            )
            Log.d("FirestoreRepository", "User: $user")
            return user
        } else {
            onFailure(Exception("No current user"))
        }
    }

    /**
     * Get household ID from Firestore
     * @param userId: String, the ID of the user to fetch the household ID for
     * @param onSuccess: (String) -> Unit, a function to call on success
     * @param onFailure: (Exception) -> Unit, a function
     */
    fun getHouseholdId(
        userId: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        return
    }

    /**
     * get household data without needing household ID
     */
    /*
    fun getHouseholdWithoutId(
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // 1 - get user id
        // 2 - get household id (using user id)
        // 3 - get household data (by household id)
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            onFailure(Exception("No current user"))
            return
        }

        val householdId = getHouseholdId(currentUserId, onSuccess, onFailure)
        if (householdId == null) {
            onFailure(Exception("No household ID found"))
            return
        }

        val household = getHousehold(householdId, onSuccess, onFailure)
        if (household == null) {
            onFailure(Exception("No household found"))
        } else {
            onSuccess(household)
        }

        return
    }
    */
}