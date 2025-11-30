package com.example.a501_final_project.chores

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.a501_final_project.FirestoreRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.toList
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


/**
 * chore should include an id for easy maintainence
 * title and description for what the chore is
 * dueDate to keep track of when they are due
 * assignee to indicate who it is assigned to
 * completed to indicate if chore is completed
 */
data class Chore(
    val choreID: String,
    val userID: String,
    val householdID: String,
    val name: String,
    val description: String?,
    var dueDate: String,
    var dateCompleted: String?,
    var assignedTo: String,
    var completed: Boolean,
)

data class RecurringChore(
    val name: String,
    val description: String,
    val recurringChoreId: String,
    val cycleFrequency: Int
)

class ChoresViewModel(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {
    private val _roommates = MutableStateFlow<List<String>>(emptyList())
    val roommates: StateFlow<List<String>> = _roommates.asStateFlow()

    private val _choresList = MutableStateFlow<List<Chore>>(emptyList())
    var choresList: StateFlow<List<Chore>> = _choresList.asStateFlow()

    var recurringChoresList: List<RecurringChore>? = null
    private val _showPrevChores = MutableStateFlow(false)
    val showPrevChores: StateFlow<Boolean> = _showPrevChores.asStateFlow()
    // Map to store image URIs for each chore by choreId
    private val _choreImageUris = MutableStateFlow<Map<String, Uri>>(emptyMap())
    val choreImageUris: StateFlow<Map<String, Uri>> = _choreImageUris.asStateFlow()

    // Temporary URI for camera capture
    private val _tempImageUri = MutableStateFlow<Uri?>(null)
    val tempImageUri: StateFlow<Uri?> = _tempImageUri.asStateFlow()

    // helper for converting data from database to local data classes
    private fun Any?.toStringOrNull(): String? {
        return when (this) {
            is String -> this
            is Number -> this.toString()
            else -> null
        }
    }

    fun loadHouseholdData() {
        firestoreRepository.getHouseholdWithoutId(
            onSuccess = { householdId, data ->
                Log.d("ChoresViewModel", "Loaded household data: $householdId - $data")
                // get residents from data
                val residentsAnyList = data["residents"] as? List<*>
                if (residentsAnyList == null) {
                    Log.w("ChoresViewModel", "'residents' field is null or not a List.")
                    _roommates.value = emptyList()
                    return@getHouseholdWithoutId
                }
                val residentIds = residentsAnyList.mapNotNull { item ->
                    // Ensure the item in the list is a Map
                    val residentMap = item as? Map<*, *>
                    // Get the 'id' from the map and convert it to a String
                    residentMap?.get("id")?.toString()
                }
                _roommates.value = residentIds
                // get list of recurring chores
                val recurringChoresListFromDB = data["recurring_chores"] as? List<*>
                if (recurringChoresListFromDB == null) {
                    Log.w("ChoresViewModel", "'recurring_chores' field is null or not a List.")
                }
                Log.d("ChoresViewModel", "Loaded recurring chores list: $recurringChoresListFromDB")
                val recurringChoresListTemp: List<RecurringChore>? = recurringChoresListFromDB?.mapIndexedNotNull { index, item ->
                    val itemAsMap = item as? Map<*, *> ?: return@mapIndexedNotNull null
                    RecurringChore(
                        name = itemAsMap["name"] as? String ?: return@mapIndexedNotNull null,
                        description = itemAsMap["description"] as? String ?: return@mapIndexedNotNull null,
                        recurringChoreId = index.toString(),
                        cycleFrequency = itemAsMap["cycle_frequency"] as? Int ?: return@mapIndexedNotNull null
                    )
                }
                recurringChoresList = recurringChoresListTemp

                // get chores list
                val choresListFromDB = data["chores"] as? List<*>
                if (choresListFromDB == null) {
                    Log.w("ChoresViewModel", "'chores' field is null or not a List.")
                    _choresList.value = emptyList()
                    return@getHouseholdWithoutId
                }
                Log.d("ChoresViewModel", "Loaded chores list: $choresListFromDB")
                _choresList.value = choresListFromDB.mapNotNull { item ->
                    val itemAsMap = item as? Map<*, *> ?: return@mapNotNull null
                    val recurringChoreIdNum = itemAsMap["recurring_chore_id"] as? Number
                    val currentRecurringChore: RecurringChore? = recurringChoreIdNum?.let { id ->
                        recurringChoresList?.find { it.recurringChoreId == id.toString() }
                    }

                    Chore(
                        choreID = itemAsMap["chore_id"].toStringOrNull() ?: return@mapNotNull null,
                        userID = itemAsMap["user_id"].toStringOrNull() ?: return@mapNotNull null,
                        householdID = householdId,
                        completed = itemAsMap["completed"] as? Boolean ?: false,
                        name = currentRecurringChore?.name ?: "Unnamed Chore",
                        description = currentRecurringChore?.description,
                        dueDate = itemAsMap["dueDate"] as? String ?: return@mapNotNull null,
                        dateCompleted = itemAsMap["dateCompleted"] as? String,
                        assignedTo = itemAsMap["assignedTo"].toStringOrNull() ?: return@mapNotNull null,
                    )
                }
            },
            onFailure = { exception -> Log.d("ChoresViewModel", "Failed to load household data: $exception") }
        )
    }

    /**
     * function to be used when first initializing/creating the list of chores for the household
     */
    fun addChores(newChore : Chore) {
        _choresList.value += newChore
    }

    /**
     * function to assign chores to members of the household
     * eventually we should do this based on time?
     */
    fun assignChores() {
        val currentRoommates = roommates.value
        if (currentRoommates.isEmpty()) {
            Log.w("ChoresViewModel", "Cannot assign chores, the roommates list is empty.")
            return
        }

        _choresList.value = _choresList.value.mapIndexed { index, chore ->
            chore.copy(assignedTo = currentRoommates[index % currentRoommates.size])
        }
    }

    /**
     * function to mark a chore as completed
     * ideally when user completes their chore,
     * this function is called and gets passed the chore
     * this function might not be needed, but it could help with safe access/storing logic in one place
     */
    fun completeChore(completedChore: Chore) {
        _choresList.value = _choresList.value.map { chore ->
            if (chore.choreID == completedChore.choreID) chore.copy(completed = true) else chore
        }
    }
    /**
     * function to get the chore(s) assigned to the specified person
     */
    fun getChoresFor(person: String): List<Chore> {
        return _choresList.value.filter { it.assignedTo == person }
    }

    /**
     * function to toggle if we are showing previous chores or not
     */
    fun toggleShowPrevChores() {
        _showPrevChores.value = !_showPrevChores.value
    }

    fun setTempImageUri(uri: Uri?) {
        _tempImageUri.value = uri
    }

    fun onPhotoCaptured(choreId: String, uri: Uri) {
        // Save the image URI for this chore
        _choreImageUris.value = _choreImageUris.value.plus(choreId to uri)
        // Clear the temp URI
        _tempImageUri.value = null
    }

    fun clearTempImageUri() {
        _tempImageUri.value = null
    }

    fun getChoreImageUri(choreId: String): Uri? {
        return _choreImageUris.value[choreId]
    }

    // Update your existing completeChore method to accept an optional imageUri
    fun completeChoreWithPhoto(chore: Chore, imageUri: Uri) {
        onPhotoCaptured(chore.choreID, imageUri)
        completeChore(chore)
    }
}