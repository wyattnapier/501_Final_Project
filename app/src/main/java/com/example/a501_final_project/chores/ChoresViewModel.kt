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

    private val _isChoresDataLoaded = MutableStateFlow(false)
    val isChoresDataLoaded: StateFlow<Boolean> = _isChoresDataLoaded.asStateFlow()
    private val _isLoading = MutableStateFlow(false)

    // helper for converting data from database to local data classes
    private fun Any?.toStringOrNull(): String? {
        return when (this) {
            is String -> this
            is Number -> this.toString()
            else -> null
        }
    }

    fun loadHouseholdData() {
        Log.d("ChoresViewModel", "========================================")
        Log.d("ChoresViewModel", "loadHouseholdData() CALLED")
        Log.d("ChoresViewModel", "_isLoading = ${_isLoading.value}")
        Log.d("ChoresViewModel", "_isChoresDataLoaded = ${_isChoresDataLoaded.value}")
        Log.d("ChoresViewModel", "Current chores count = ${_choresList.value.size}")

        if (_isLoading.value) {
            Log.w("ChoresViewModel", "⚠️ Already loading, RETURNING EARLY")
            return
        }
        if (_isChoresDataLoaded.value) {
            Log.d("ChoresViewModel", "✓ Already loaded, RETURNING EARLY")
            return
        }

        _isLoading.value = true
        Log.d("ChoresViewModel", "🔄 Starting load...")

        firestoreRepository.getHouseholdWithoutId(
            onSuccess = { householdId, data ->
                Log.d("ChoresViewModel", "========================================")
                Log.d("ChoresViewModel", "✓✓✓ SUCCESS CALLBACK ENTERED")
                Log.d("ChoresViewModel", "Household ID: $householdId")

                // Parse residents
                val residentsAnyList = data["residents"] as? List<*>
                if (residentsAnyList == null) {
                    Log.w("ChoresViewModel", "'residents' field is null")
                    _roommates.value = emptyList()
                } else {
                    val residentIds = residentsAnyList.mapNotNull { item ->
                        val residentMap = item as? Map<*, *>
                        residentMap?.get("id")?.toString()
                    }
                    _roommates.value = residentIds
                    Log.d("ChoresViewModel", "Loaded ${residentIds.size} residents: $residentIds")
                }

                // Parse recurring chores
                val recurringChoresListFromDB = data["recurring_chores"] as? List<*>
                if (recurringChoresListFromDB == null) {
                    Log.w("ChoresViewModel", "'recurring_chores' is null")
                    recurringChoresList = null
                } else {
                    val recurringChoresListTemp: List<RecurringChore>? =
                        recurringChoresListFromDB.mapIndexedNotNull { index, item ->
                            val itemAsMap = item as? Map<*, *> ?: return@mapIndexedNotNull null

                            Log.d("ChoresViewModel", "Parsing recurring chore $index: $itemAsMap")

                            val name = itemAsMap["name"] as? String
                            if (name == null) {
                                Log.w("ChoresViewModel", "Recurring chore $index has no name")
                                return@mapIndexedNotNull null
                            }

                            val description = itemAsMap["description"] as? String
                            if (description == null) {
                                Log.w(
                                    "ChoresViewModel",
                                    "Recurring chore $index has no description"
                                )
                                return@mapIndexedNotNull null
                            }

                            // IMPORTANT: Look for "cycle" not "cycle_frequency"
                            val cycleFrequency =
                                (itemAsMap["cycle"] ?: itemAsMap["cycle_frequency"]) as? Number
                            if (cycleFrequency == null) {
                                Log.w("ChoresViewModel", "Recurring chore $index has no cycle")
                                return@mapIndexedNotNull null
                            }

                            val recurringChore = RecurringChore(
                                name = name,
                                description = description,
                                recurringChoreId = index.toString(),  // Use index as ID (0, 1, 2...)
                                cycleFrequency = cycleFrequency.toInt()
                            )

                            Log.d(
                                "ChoresViewModel",
                                "✓ Parsed recurring chore $index: ${recurringChore.name}"
                            )
                            recurringChore
                        }
                    recurringChoresList = recurringChoresListTemp
                    Log.d(
                        "ChoresViewModel",
                        "Total recurring chores loaded: ${recurringChoresListTemp?.size ?: 0}"
                    )
                    Log.d("ChoresViewModel", "Recurring chores: $recurringChoresListTemp")
                }

                // Parse chores
                val choresListFromDB = data["chores"] as? List<*>
                if (choresListFromDB == null) {
                    Log.w("ChoresViewModel", "'chores' field is NULL")
                    _choresList.value = emptyList()
                } else {
                    Log.d("ChoresViewModel", "📋 Processing ${choresListFromDB.size} chores from DB")

                    val parsedChores = choresListFromDB.mapIndexedNotNull { index, item ->
                        Log.d("ChoresViewModel", "--- Processing chore $index ---")
                        val itemAsMap = item as? Map<*, *>
                        if (itemAsMap == null) {
                            Log.w("ChoresViewModel", "Chore $index is not a Map")
                            return@mapIndexedNotNull null
                        }

                        Log.d("ChoresViewModel", "Chore $index data: $itemAsMap")

                        // Get chore_id/choreID
                        val choreId = (itemAsMap["chore_id"] ?: itemAsMap["choreID"]).toStringOrNull()
                        Log.d("ChoresViewModel", "Chore $index - choreId: $choreId")
                        if (choreId == null) {
                            Log.w("ChoresViewModel", "Chore $index SKIPPED - no chore_id")
                            return@mapIndexedNotNull null
                        }

                        // Get recurring chore info
                        val recurringChoreIdNum = itemAsMap["recurring_chore_id"] as? Number
                        Log.d("ChoresViewModel", "Chore $index - recurring_chore_id: $recurringChoreIdNum")
                        val currentRecurringChore: RecurringChore? = recurringChoreIdNum?.let { id ->
                            recurringChoresList?.find { it.recurringChoreId == id.toString() }
                        }
                        Log.d("ChoresViewModel", "Chore $index - found recurring chore: ${currentRecurringChore?.name}")

                        // Parse due date
                        val dueDateValue = itemAsMap["dueDate"] ?: itemAsMap["due_date"]
                        Log.d("ChoresViewModel", "Chore $index - dueDateValue type: ${dueDateValue?.javaClass?.simpleName}, value: $dueDateValue")
                        val dueDate = when (dueDateValue) {
                            is String -> {
                                Log.d("ChoresViewModel", "Chore $index - dueDate is String: $dueDateValue")
                                dueDateValue
                            }
                            is com.google.firebase.Timestamp -> {
                                val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                                val formatted = sdf.format(dueDateValue.toDate())
                                Log.d("ChoresViewModel", "Chore $index - dueDate converted from Timestamp: $formatted")
                                formatted
                            }
                            else -> {
                                Log.w("ChoresViewModel", "Chore $index - Invalid dueDate type")
                                null
                            }
                        }

                        if (dueDate == null) {
                            Log.w("ChoresViewModel", "Chore $index SKIPPED - no valid dueDate")
                            return@mapIndexedNotNull null
                        }

                        // Parse date completed
                        val dateCompletedValue = itemAsMap["dateCompleted"] ?: itemAsMap["date_completed"]
                        val dateCompleted = when (dateCompletedValue) {
                            is String -> dateCompletedValue
                            is com.google.firebase.Timestamp -> {
                                val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                                sdf.format(dateCompletedValue.toDate())
                            }
                            null -> null
                            else -> null
                        }
                        Log.d("ChoresViewModel", "Chore $index - dateCompleted: $dateCompleted")

                        // Parse assignedTo
                        val assignedToValue = itemAsMap["assignedTo"] ?: itemAsMap["assigned_to"]
                        Log.d("ChoresViewModel", "Chore $index - assignedToValue type: ${assignedToValue?.javaClass?.simpleName}")
                        val assignedTo = when (assignedToValue) {
                            is String -> {
                                Log.d("ChoresViewModel", "Chore $index - assignedTo is String: $assignedToValue")
                                assignedToValue
                            }
                            is com.google.firebase.firestore.DocumentReference -> {
                                val id = assignedToValue.id
                                Log.d("ChoresViewModel", "Chore $index - assignedTo from DocumentReference: $id")
                                id
                            }
                            else -> {
                                val converted = assignedToValue.toStringOrNull()
                                Log.d("ChoresViewModel", "Chore $index - assignedTo converted: $converted")
                                converted
                            }
                        }

                        if (assignedTo == null) {
                            Log.w("ChoresViewModel", "Chore $index SKIPPED - no assignedTo")
                            return@mapIndexedNotNull null
                        }

                        val chore = Chore(
                            choreID = choreId,
                            householdID = householdId,
                            completed = itemAsMap["completed"] as? Boolean ?: false,
                            name = currentRecurringChore?.name ?: "Unnamed Chore",
                            description = currentRecurringChore?.description,
                            dueDate = dueDate,
                            dateCompleted = dateCompleted,
                            assignedTo = assignedTo,
                        )

                        Log.d("ChoresViewModel", "✓✓✓ Chore $index SUCCESSFULLY PARSED: ${chore.name} - ${chore.assignedTo}")
                        chore
                    }

                    Log.d("ChoresViewModel", "========================================")
                    Log.d("ChoresViewModel", "📊 Parsed ${parsedChores.size} chores out of ${choresListFromDB.size}")
                    Log.d("ChoresViewModel", "Setting _choresList.value...")
                    _choresList.value = parsedChores
                    Log.d("ChoresViewModel", "✓ _choresList.value is now: ${_choresList.value.size} chores")
                    Log.d("ChoresViewModel", "Chores: ${_choresList.value}")
                }

                Log.d("ChoresViewModel", "Setting flags...")
                _isChoresDataLoaded.value = true
                _isLoading.value = false
                Log.d("ChoresViewModel", "✓✓✓ LOAD COMPLETE - Final count: ${_choresList.value.size}")
                Log.d("ChoresViewModel", "========================================")
            },
            onFailure = { exception ->
                Log.e("ChoresViewModel", "========================================")
                Log.e("ChoresViewModel", "✗✗✗ FAILURE CALLBACK ENTERED")
                Log.e("ChoresViewModel", "Exception: $exception")
                Log.e("ChoresViewModel", "Stack trace:", exception)
                _isLoading.value = false
                Log.d("ChoresViewModel", "========================================")
            }
        )

        Log.d("ChoresViewModel", "loadHouseholdData() exiting (async operation started)")
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