package com.example.a501_final_project

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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
     * Get household information from Firestore (CALLBACK VERSION - keep for compatibility)
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
     * Get household information from Firestore (SUSPEND VERSION - new)
     */
    suspend fun getHouseholdSuspend(householdID: String): Map<String, Any> {
        return try {
            val document = db.collection("households").document(householdID).get().await()
            if (document != null && document.exists()) {
                document.data ?: emptyMap()
            } else {
                throw Exception("Household not found")
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error getting household", e)
            throw e
        }
    }

    /**
     * Get user information from Firestore (CALLBACK VERSION - keep for compatibility)
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

    /**
     * Get user information from Firestore (SUSPEND VERSION - new)
     */
    suspend fun getUserSuspend(userId: String): Map<String, Any> {
        return try {
            val document = db.collection("users").document(userId).get().await()
            if (document != null && document.exists()) {
                document.data ?: emptyMap()
            } else {
                throw Exception("User not found")
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error getting user", e)
            throw e
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
     * Get the household ID for a user (SUSPEND VERSION)
     */
    suspend fun getHouseholdIdForUserSuspend(userId: String): String {
        return try {
            val document = db.collection("users").document(userId).get().await()
            if (document != null && document.exists()) {
                document.getString("household_id") ?: throw Exception("User has no household")
            } else {
                throw Exception("User not found")
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error getting household ID for user", e)
            throw e
        }
    }

    /**
     * Get household data for the current logged-in user (CALLBACK VERSION)
     */
    fun getHouseholdWithoutId(
        onSuccess: (householdId: String, householdData: Map<String, Any>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            onFailure(Exception("No current user logged in"))
            return
        }
        Log.d("FirestoreRepository", "Current user ID: $currentUserId")

        getHouseholdIdForUser(
            userId = currentUserId,
            onSuccess = { householdId ->
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

    /**
     * Get household data for the current logged-in user (SUSPEND VERSION)
     */
    suspend fun getHouseholdWithoutIdSuspend(): Pair<String, Map<String, Any>> {
        val currentUserId = getCurrentUserId() ?: throw Exception("No current user logged in")
        Log.d("FirestoreRepository", "Current user ID: $currentUserId")

        val householdId = getHouseholdIdForUserSuspend(currentUserId)
        val householdData = getHouseholdSuspend(householdId)

        Log.d("FirestoreRepository", "Successfully loaded household without ID")
        return Pair(householdId, householdData)
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