package com.example.a501_final_project

/**
 * Interface defining the contract for our data repository.
 * This allows for dependency inversion, making our ViewModels more testable
 * by allowing us to inject a fake or mock repository during tests.
 */
interface IRepository {
    /**
     * Gets the current authenticated user's ID from Firebase Auth.
     */
    fun getCurrentUserId(): String?

    /**
     * Asynchronously retrieves a specific household document from Firestore.
     */
    suspend fun getHouseholdSuspend(householdID: String): Map<String, Any>

    /**
     * Asynchronously retrieves a specific user document from Firestore.
     */
    suspend fun getUserSuspend(userId: String): Map<String, Any>

    /**
     * Asynchronously gets the current user's data along with their ID.
     */
    suspend fun getUserWithoutIdSuspend(): Pair<String, Map<String, Any>>

    /**
     * Asynchronously finds the household ID associated with a specific user.
     */
    suspend fun getHouseholdIdForUserSuspend(userId: String): String

    /**
     * Asynchronously gets the current user's household data along with the household's ID.
     */
    suspend fun getHouseholdWithoutIdSuspend(): Pair<String, Map<String, Any>>

    /**
     * Asynchronously gets the calendar name from the current user's household data.
     */
    suspend fun getHouseholdCalendarNameWithoutIdSuspend(): String

    /**
     * Asynchronously creates a new household document in Firestore.
     * @return The ID of the newly created household.
     */
    suspend fun createHouseholdSuspend(householdData: Map<String, Any>): String

    /**
     * Asynchronously adds a new resident to a household's residents array.
     */
    suspend fun addResidentToHouseholdSuspend(
        householdId: String,
        residentData: Map<String, Any>,
        paymentsData: List<Map<String, Any>>,
    )

    /**
     * Asynchronously updates a user document to include their household ID.
     */
    suspend fun updateUserHouseholdIdSuspend(userId: String, householdId: String)

    /**
     * Asynchronously finds a chore in a household's chores array and marks it as completed.
     */
    suspend fun markChoreAsCompletedSuspend(choreId: String, householdId: String)

    /**
     * Asynchronously finds a payment in a household's payments array and marks it as paid.
     */
    suspend fun markPaymentAsCompletedSuspend(paymentId: String, householdId: String)

    /**
     * Asynchronously adds a pending member to household
     */
    suspend fun addPendingMemberToHousehold(householdId: String, newUserEmail: String)

    /**
     * Asynchronously removes a pending member from a household
     */
    suspend fun removePendingMember(householdId: String, emailToRemove: String)

    /**
     * Asynchronously checks if a user document exists in Firestore.
     */
    suspend fun checkUserExists(userId: String): Boolean

    /**
     * Asynchronously checks if a user has a valid household_id in their document.
     */
    suspend fun isUserInHousehold(userId: String): Boolean

    /**
     * Asynchronously creates a new user document in Firestore.
     */
    suspend fun saveNewUser(userId: String, name: String, venmoUsername: String)

    /**
     * Asynchronously gets the calendar ID and pending members from the user's household.
     */
    suspend fun getHouseholdCalendarIdAndPendingMembersSuspend(): Map<String, Any?>

    /**
     * Asynchronously adds a new payment to a household's payments array.
     */
    suspend fun addNewPaymentToHousehold(householdId: String, newPaymentData: Map<String, Any>)
}
