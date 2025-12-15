package com.example.a501_final_project

import android.util.Log
import com.example.a501_final_project.payment.Payment
import com.example.a501_final_project.chores.Chore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository : IRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Get the current authenticated user's ID
     * @return String? the user ID, or null if not logged in
     */
    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }


    /**
     * Get household information from Firestore (SUSPEND VERSION - new)
     */
     override suspend fun getHouseholdSuspend(householdID: String): Map<String, Any> {
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
    override suspend fun getUserSuspend(userId: String): Map<String, Any> {
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
    override suspend fun getUserWithoutIdSuspend(): Pair<String, Map<String, Any>> {
        val currentUserId = getCurrentUserId() ?: throw Exception("No current user logged in")
        Log.d("FirestoreRepository", "Current user ID: $currentUserId")

        val userData = getUserSuspend(currentUserId)

        Log.d("FirestoreRepository", "Successfully loaded user without ID")
        return Pair(currentUserId, userData)
    }

    /**
     * Checks if a user document exists in the 'users' collection.
     */
    override suspend fun checkUserExists(userId: String): Boolean {
        return try {
            val document = db.collection("users").document(userId).get().await()
            document.exists()
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error checking if user exists", e)
            false
        }
    }

    /**
     * check if a user belongs to a household
     */
    override suspend fun isUserInHousehold(userId: String): Boolean {
        return try {
            val document = db.collection("users").document(userId).get().await()
            document.exists() && document.getString("household_id") != null
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error checking if user is in household", e)
            false
        }
    }

    /**
     * Creates a new user document in the 'users' collection.
     */
    override suspend fun saveNewUser(userId: String, name: String, venmoUsername: String) {
        try {
            val user = mapOf(
                "name" to name,
                "venmoUsername" to venmoUsername,
                "household_id" to null // New users don't have a household yet
            )
            db.collection("users").document(userId).set(user).await()
            Log.d("FirestoreRepository", "New user saved with ID: $userId")
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error saving new user", e)
            throw e // Re-throw for the ViewModel to handle
        }
    }

    /**
     * Get the household ID for a user (SUSPEND VERSION)
     */
    override suspend fun getHouseholdIdForUserSuspend(userId: String): String {
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
    override suspend fun getHouseholdWithoutIdSuspend(): Pair<String, Map<String, Any>> {
        val currentUserId = getCurrentUserId() ?: throw Exception("No current user logged in")
        Log.d("FirestoreRepository", "Current user ID: $currentUserId")

        val householdId = getHouseholdIdForUserSuspend(currentUserId)
        val householdData = getHouseholdSuspend(householdId)

        Log.d("FirestoreRepository", "Successfully loaded household without ID")
        return Pair(householdId, householdData)
    }

    /**
     * Gets the Google Calendar ID from the current user's household data. (SUSPEND VERSION)
     */
    override suspend fun getHouseholdCalendarIdAndPendingMembersSuspend(): Map<String, Any?> {
        // Re-use the existing function that gets the whole household document
        val (householdId, householdData) = getHouseholdWithoutIdSuspend()
        // Return the 'calendar_id' field, or throw an exception if it's missing
        return mapOf(
            "household_id" to householdId,
            "calendar_id" to householdData["calendar_id"],
            "pending_members" to householdData["pending_members"]
        )
    }

    // add member to list of those that need to be added to household calendar
    override suspend fun addPendingMemberToHousehold(householdId: String, newUserEmail: String) {
        try {
            val householdRef = db.collection("households").document(householdId)
            // Atomically add the new user's email to the 'pending_members' array.
            householdRef.update("pending_members", FieldValue.arrayUnion(newUserEmail)).await()
            Log.d("FirestoreRepository", "Added $newUserEmail to pending members for household $householdId")
        } catch(e: Exception) {
            Log.e("FirestoreRepository", "Failed to add pending member", e)
            throw e
        }
    }

    override suspend fun removePendingMember(householdId: String, emailToRemove: String) {
        if (emailToRemove.isEmpty()) {
            Log.w("FirestoreRepository", "No email to remove: [$emailToRemove]")
            return
        }
        if (householdId.isEmpty()) {
            Log.w("FirestoreRepository", "No household ID to remove from: [$householdId]")
            return
        }
        Log.d("FirestoreRepository", "Removing $emailToRemove from pending members for household $householdId")
        try {
            val householdRef = db.collection("households").document(householdId)
            // Atomically remove the email from the 'pending_members' array.
            householdRef.update("pending_members", FieldValue.arrayRemove(emailToRemove)).await()
            Log.d("FirestoreRepository", "Removed $emailToRemove from pending members.")
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Failed to remove pending member", e)
            throw e
        }
    }

    override suspend fun getHouseholdCalendarNameWithoutIdSuspend(): String {
        val (_, householdData) = getHouseholdWithoutIdSuspend()
        return householdData["calendar"] as? String
            ?: throw Exception("Household has no calendar name")
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
     * Create a new household
     * @param householdData: Map<String, Any>, the household data
     * @param onSuccess: (String) -> Unit, callback with the new household ID
     * @param onFailure: (Exception) -> Unit, callback on failure
     */
    /**
     * Create a new household (SUSPEND VERSION)
     * @param householdData: Map<String, Any>, the household data
     * @return String: The ID of the newly created household
     */
    override suspend fun createHouseholdSuspend(householdData: Map<String, Any>): String {
        return try {
            val documentReference = db.collection("households")
                .add(householdData)
                .await() // Use .await() to wait for the result
            Log.d("FirestoreRepository", "Created household ${documentReference.id}")
            documentReference.id // Return the new ID
        } catch (exception: Exception) {
            Log.e("FirestoreRepository", "Error creating household", exception)
            throw exception // Re-throw the exception to be caught by the ViewModel
        }
    }

    /**
     * Update household with new resident and payment information (SUSPEND VERSION)
     * @param householdId: String, the household ID
     * @param residentData: Map<String, Any>, the new resident data
     * @param paymentsData: List<Map<String, Any>>, updated payments list
     */
    override suspend fun addResidentToHouseholdSuspend(
        householdId: String,
        residentData: Map<String, Any>,
        paymentsData: List<Map<String, Any>>,
    ) {
        try {
            val householdRef = db.collection("households").document(householdId)

            val updateMap = mapOf(
                "recurring_payments" to paymentsData,
                "residents" to FieldValue.arrayUnion(residentData)
            )

            householdRef.update(updateMap).await()
            Log.d("FirestoreRepository", "Successfully added resident to household")
        } catch(exception: Exception) {
            Log.e("FirestoreRepository", "Failed to add resident to household", exception)
            throw exception // Re-throw exception for the ViewModel to catch
        }
    }

    override suspend fun addNewPaymentToHousehold(householdId: String, newPaymentData: Map<String, Any>) {
        try {
            val householdRef = db.collection("households").document(householdId)
            // Atomically add the new payment map to the 'payments' array
            householdRef.update("payments", FieldValue.arrayUnion(newPaymentData)).await()
            Log.d("FirestoreRepository", "Successfully added new payment to household $householdId")
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error adding new payment to household", e)
            throw e // Re-throw for the ViewModel to handle
        }
    }

    override suspend fun updateUserHouseholdIdSuspend(userId: String, householdId: String) {
        Log.d("FirestoreRepository", "Trying to update user [$userId] household ID to [$householdId]")
        try {
            val userRef = db.collection("users").document(userId)
            userRef.update("household_id", householdId).await()
            Log.d("FirestoreRepository", "Successfully updated user's household ID")
        } catch (exception: Exception) {
            Log.e("FirestoreRepository", "Failed to update user's household ID", exception)
            throw exception
        }
    }

    override suspend fun markChoreAsCompletedSuspend(choreId: String, householdId: String) {
        try {
            val choreRef = db.collection("households").document(householdId)
            val document = choreRef.get().await()
            if (!document.exists()) {
                throw Exception("Household document not found")
            }

            // Get the current list of chores as a mutable list of maps
            val choresList = document.get("chores") as? List<Map<String, Any>> ?: emptyList()
            val mutableChores = choresList.map { it.toMutableMap() }.toMutableList()

            val choreIndex = mutableChores.indexOfFirst { it["choreID"] == choreId } // Note: "choreID" must match the case in your Firestore document
            if (choreIndex != -1) {
                // Found the chore, now update its 'completed' field
                mutableChores[choreIndex]["completed"] = true
            } else {
                // Chore not found in the array, log it and maybe throw an error
                Log.w("FirestoreRepository", "Chore with ID '$choreId' not found in the array.")
                return // Or throw Exception("Chore not found")
            }

            choreRef.update("chores", mutableChores).await()

            Log.d("FirestoreRepository", "Successfully marked chore as completed")
        } catch (exception: Exception){
            Log.e("FirestoreRepository", "Failed to mark chore as completed", exception)
            throw exception
        }
    }


    /**
     * function to update assignments of payment assignments to firebase
     */
    override suspend fun updatePaymentAssignments(newPayments: List<Payment>) { // appends, not overwrite
        if (newPayments.isEmpty()) return

        val db = FirebaseFirestore.getInstance()
        val householdId = getHouseholdIdForUserSuspend(newPayments.first().payFromId) // get householdId since not stored in  paymenbt

        val paymentMap = newPayments.map{ payment ->
            mapOf(
                "id" to payment.id,
                "pay_to" to payment.payToId,
//                "payToVenmoUsername" to payment.payToVenmoUsername,
                "pay_from" to payment.payFromId,
                "amount" to payment.amount,
                "memo" to payment.memo,
                "due_date" to payment.dueDate,
                "date_paid" to payment.datePaid,
                "paid" to payment.paid,
                "recurring" to payment.recurring, // is this jsut true...?
                "recurring_payment_id" to payment.instanceOf
            )

        }

        db.collection("households")
            .document(householdId)
            .update("payments", FieldValue.arrayUnion(*paymentMap.toTypedArray())) // to update rather than overwrite
    }


    override suspend fun markPaymentAsCompletedSuspend(paymentId: String, householdId: String) {
        try {
            val paymentRef = db.collection("households").document(householdId)
            val document = paymentRef.get().await()
            if (!document.exists()) {
                throw Exception("Household document not found")
            }

            // Get the current list of chores as a mutable list of maps
            val paymentsList = document.get("payments") as? List<Map<String, Any>> ?: emptyList()
            val mutablePayments = paymentsList.map { it.toMutableMap() }.toMutableList()
            Log.d("FirestoreRepository", "Mutable payments: $mutablePayments")
            val paymentIdAsLong = paymentId.toLongOrNull()
            if (paymentIdAsLong == null) {
                Log.e("FirestoreRepository", "Invalid paymentId format, cannot convert to Long: '$paymentId'")
                throw Exception("Invalid paymentId (null or incorrect format)")
            }

            val paymentIndex = mutablePayments.indexOfFirst {
                (it["id"] as? Long) == paymentIdAsLong
            }
            if (paymentIndex != -1) {
                // Found the chore, now update its 'completed' field
                mutablePayments[paymentIndex]["paid"] = true
            } else {
                // Chore not found in the array, log it and maybe throw an error
                Log.w("FirestoreRepository", "Payment with ID '$paymentIndex' not found in the array.")
                return // Or throw Exception("Chore not found")
            }

            paymentRef.update("payments", mutablePayments).await()

            Log.d("FirestoreRepository", "Successfully marked payment as completed")
        } catch (exception: Exception){
            Log.e("FirestoreRepository", "Failed to mark payment as completed", exception)
            throw exception
        }
    }


    /**
     * function for updatign firebase with chore updates
     */

    override suspend fun updateChoreAssignmentsSuspend(updatedChores: List<Chore>) {
        val db = FirebaseFirestore.getInstance()
        val householdId = updatedChores.first().householdID

        val choreMaps = updatedChores.map { chore ->
            mapOf(
                "choreID" to chore.choreID,
                "assignedToId" to chore.assignedToId,
                "due_date" to chore.dueDate,
                "completed" to chore.completed,
                "date_completed" to chore.dateCompleted,
                "recurring_chore_id" to chore.instanceOf.toIntOrNull()
            )
        }

        db.collection("households")
            .document(householdId)
            .update("chores", choreMaps)
    }

}