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
        onSuccess: (userId: String, userData: Map<String, Any>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            onFailure(Exception("No current user"))
            return
        }

        getUser(
            userId = currentUserId,
            onSuccess = { userData ->
                Log.d("FirestoreRepository", "Successfully loaded user without ID $userData")
                onSuccess(currentUserId, userData)
            },
            onFailure = {exception ->
                Log.e("FirestoreRepository", "Error loading user", exception)
                onFailure(exception)
            }
        )
    }

    /**
     * Get the household ID for a user
     * @param userId: String, the user's ID
     * @param onSuccess: (String) -> Unit, callback with household ID
     * @param onFailure: (Exception) -> Unit, callback on failure
     */
    fun getHouseholdIdForUser(
        userId: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val householdId = document.getString("household_id")
                    if (householdId != null) {
                        onSuccess(householdId)
                    } else {
                        onFailure(Exception("User has no household"))
                    }
                } else {
                    onFailure(Exception("User not found"))
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreRepository", "Error getting household ID for user", exception)
                onFailure(exception)
            }
    }

    /**
     * Get household data for the current logged-in user
     * Chains: getCurrentUserId -> getHouseholdIdForUser -> getHousehold
     * @param onSuccess: (Map<String, Any>) -> Unit, callback with household data
     * @param onFailure: (Exception) -> Unit, callback on failure
     */
    fun getHouseholdWithoutId(
        onSuccess: (householdId: String, householdData: Map<String, Any>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Step 1: Get current user ID
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            onFailure(Exception("No current user logged in"))
            return
        }
        Log.d("FirestoreRepository", "Current user ID: $currentUserId")

        // Step 2: Get household ID for this user
        getHouseholdIdForUser(
            userId = currentUserId,
            onSuccess = { householdId ->
                // Step 3: Get household data using the household ID
                getHousehold(
                    householdID = householdId,
                    onSuccess = { householdData ->
                        Log.d("FirestoreRepository", "Successfully loaded household without ID: $householdData")
                        onSuccess(householdId, householdData)
                    },
                    onFailure = { exception ->
                        Log.e("FirestoreRepository", "Error loading household", exception)
                        onFailure(exception)
                    }
                )
            },
            onFailure = { exception ->
                Log.e("FirestoreRepository", "Error getting household ID", exception)
                onFailure(exception)
            }
        )
    }

    // used to get and add events to shared calendar
    fun getHouseholdCalendarNameWithoutId(
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getHouseholdWithoutId(
            onSuccess = { _, householdData ->
                val calendarName = householdData["calendar"] as? String
                if (calendarName != null) {
                    onSuccess(calendarName)
                } else {
                    onFailure(Exception("Household has no calendar name"))
                }
            },
            onFailure = onFailure
        )
    }

    // used to get list of residents in apartment
    fun getHouseholdResidentsWithoutId(
        onSuccess: (List<Any?>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getHouseholdWithoutId(
            onSuccess = { _, householdData ->
                val residents = householdData["residents"] as? List<Any?>
                if (residents != null && residents.isNotEmpty()) {
                    onSuccess(residents)
                } else {
                    onFailure(Exception("Household has no residents"))
                }
            },
            onFailure = onFailure
        )
    }
}