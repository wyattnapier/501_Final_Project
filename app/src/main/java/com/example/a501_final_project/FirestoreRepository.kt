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

    /**
     * Get user data for the current logged-in user (SUSPEND VERSION)
     */
    suspend fun getUserWithoutIdSuspend(): Pair<String, Map<String, Any>> {
        val currentUserId = getCurrentUserId() ?: throw Exception("No current user logged in")
        Log.d("FirestoreRepository", "Current user ID: $currentUserId")

        val userData = getUserSuspend(currentUserId)

        Log.d("FirestoreRepository", "Successfully loaded user without ID")
        return Pair(currentUserId, userData)
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

    /**
     * Get household calendar name (SUSPEND VERSION)
     */
    suspend fun getHouseholdCalendarNameWithoutIdSuspend(): String {
        val (_, householdData) = getHouseholdWithoutIdSuspend()
        return householdData["calendar"] as? String
            ?: throw Exception("Household has no calendar name")
    }
}