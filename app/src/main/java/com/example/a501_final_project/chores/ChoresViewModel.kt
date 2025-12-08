package com.example.a501_final_project.chores

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.util.unpackInt1
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a501_final_project.FirestoreRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.a501_final_project.storage.SupabaseClientProvider
import io.github.jan.supabase.storage.storage
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import androidx.core.net.toUri
import com.example.a501_final_project.BuildConfig
import java.util.UUID


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
    var assignedToId: String,
    var assignedToName: String?,
    var completed: Boolean,
    var instanceOf: String, // the recurring chore type it belongs to, safer than checking name
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
        if (_isLoading.value) {
            Log.w("ChoresViewModel", "Already loading, skipping duplicate call")
            return
        }
        if (_isChoresDataLoaded.value) {
            Log.w("ChoresViewModel", "Chores data already loaded, skipping")
            return
        }

        _isLoading.value = true
        Log.d("ChoresViewModel", "Starting to load household data...")

        // Launch a coroutine in the ViewModel scope to prevent failed results from async
        viewModelScope.launch {
            try {
                // Use the suspend version
                val (householdId, data) = firestoreRepository.getHouseholdWithoutIdSuspend()

                Log.d("ChoresViewModel", "Loaded household data: $householdId")

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
                    Log.d("ChoresViewModel", "Loaded ${residentIds.size} residents")
                }

                // Parse recurring chores
                val recurringChoresListFromDB = data["recurring_chores"] as? List<*>
                val recurringChoresListTemp: List<RecurringChore>? = recurringChoresListFromDB?.mapIndexedNotNull { index, item ->
                    val itemAsMap = item as? Map<*, *> ?: return@mapIndexedNotNull null
                    RecurringChore(
                        name = itemAsMap["name"] as? String ?: return@mapIndexedNotNull null,
                        description = itemAsMap["description"] as? String ?: return@mapIndexedNotNull null,
                        recurringChoreId = index.toString(),
                        cycleFrequency = ((itemAsMap["cycle"] ?: itemAsMap["cycle_frequency"]) as? Number)?.toInt() ?: return@mapIndexedNotNull null
                    )
                }
                recurringChoresList = recurringChoresListTemp
                Log.d("ChoresViewModel", "Loaded ${recurringChoresListTemp?.size ?: 0} recurring chores")

                // Parse chores
                val choresListFromDB = data["chores"] as? List<*>
                if (choresListFromDB == null) {
                    Log.w("ChoresViewModel", "'chores' field is null")
                    _choresList.value = emptyList()
                } else {
                    Log.d("ChoresViewModel", "Processing ${choresListFromDB.size} chores")

                    val parsedChores = choresListFromDB.mapIndexedNotNull { index, item ->
                        val itemAsMap = item as? Map<*, *> ?: return@mapIndexedNotNull null

                        // Get chore_id
                        val choreId = (itemAsMap["chore_id"] ?: itemAsMap["choreID"]).toStringOrNull()
                            ?: return@mapIndexedNotNull null

                        // Get recurring chore info
                        val recurringChoreIdNum = itemAsMap["recurring_chore_id"] as? Number
                        val currentRecurringChore: RecurringChore? = recurringChoreIdNum?.let { id ->
                            recurringChoresList?.find { it.recurringChoreId == id.toString() }
                        }

                        // Parse due date
                        val dueDateValue = itemAsMap["dueDate"] ?: itemAsMap["due_date"]
                        val dueDate = when (dueDateValue) {
                            is String -> dueDateValue
                            is com.google.firebase.Timestamp -> {
                                val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                                sdf.format(dueDateValue.toDate())
                            }
                            else -> null
                        } ?: return@mapIndexedNotNull null

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

                        // Parse assignedTo ID
                        val assignedToId = when (val assignedToIdValue = itemAsMap["assignedToId"] ?: itemAsMap["assigned_to_id"]) {
                            is String -> assignedToIdValue
                            is com.google.firebase.firestore.DocumentReference -> assignedToIdValue.id
                            else -> assignedToIdValue.toStringOrNull()
                        } ?: return@mapIndexedNotNull null

                        // NOW fetch the user's name using suspend function
                        val assignedToName = try {
                            val userData = firestoreRepository.getUserSuspend(assignedToId)
                            userData["name"] as? String ?: assignedToId
                        } catch (e: Exception) {
                            Log.w("ChoresViewModel", "Failed to get name for user $assignedToId: $e")
                            assignedToId  // Fall back to ID if name fetch fails
                        }

                        Log.d("ChoresViewModel", "Chore $index: ${currentRecurringChore?.name} assigned to $assignedToName")

                        Chore(
                            choreID = choreId,
                            householdID = householdId,
                            completed = itemAsMap["completed"] as? Boolean ?: false,
                            name = currentRecurringChore?.name ?: "Unnamed Chore",
                            description = currentRecurringChore?.description,
                            dueDate = dueDate,
                            dateCompleted = dateCompleted,
                            assignedToId = assignedToId,
                            assignedToName = assignedToName,
                            instanceOf = currentRecurringChore?.recurringChoreId ?: "Unnamed Chore"
                        )
                    }

                    _choresList.value = parsedChores
                    Log.d("ChoresViewModel", "Successfully loaded ${parsedChores.size} chores")
                }

                _isChoresDataLoaded.value = true
                _isLoading.value = false
                // assign chores upon opening
                assignChores()
                Log.d("ChoresViewModel", "Load complete - ${_choresList.value.size} chores")

            } catch (e: Exception) {
                Log.e("ChoresViewModel", "Failed to load household data", e)
                _isLoading.value = false
            }
        }
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
        val recurringChores = recurringChoresList ?: return
        if (currentRoommates.isEmpty()) {
            Log.w("ChoresViewModel", "Cannot assign chores, the roommates list is empty.")
            return
        }

//        _choresList.value = _choresList.value.mapIndexed { index, chore ->
//            chore.copy(assignedToId = currentRoommates[index % currentRoommates.size])
//        }



        val chores = _choresList.value.toMutableList()

        // if there are no roommates then we can't do any assignments
        if (currentRoommates.isEmpty()) {
            return
        }

        // if no chores then we can't do any assignments
        if (chores.isEmpty()) {
            return
        }


        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
        val today = Calendar.getInstance() // Use fully qualified name
        val unassignedChores = chores.filter { chore ->
            chore.assignedToId.isBlank() && chore.dueDate.isNotBlank()
        }.filter{ chore ->
            try {
                val due = dateFormat.parse(chore.dueDate)
                due != null && due.after(today.time)
            } catch (e: Exception) {
                false
            }
        }

        if (unassignedChores.isEmpty()) {
            Log.d("ChoresViewModel", "no chores need to be assigned")
            return
        }

        // count how much load each roommate has
        val load = mutableMapOf<String, Int>()
        currentRoommates.forEach { roommate ->
            load[roommate] = chores.count { it.assignedToId == roommate }
        }

        // assign the chores to the least loaded
        for (chore in unassignedChores) {
            val leastLoaded = load.minBy { it.value }.key // minBy takes first instance of the min value, so "random" in that sense, but can make actually random if need
            load[leastLoaded] = load[leastLoaded]!! + 1

            val choreIdx = chores.indexOfFirst {it.choreID == chore.choreID}
            if (choreIdx != -1) {
                chores[choreIdx] = chores[choreIdx].copy(assignedToId = leastLoaded) // assign it to least loaded roommate
            }
        }

        // updates UI
        _choresList.value = chores

        // send to DB
        viewModelScope.launch {
            firestoreRepository.updateChoreAssignmentsSuspend(_choresList.value)
        }

        Log.d("ChoresViewModel", "chores assigned")
    }


    /**
     * function to mark a chore as completed
     * ideally when user completes their chore,
     * this function is called and gets passed the chore
     * this function might not be needed, but it could help with safe access/storing logic in one place
     * This function also creates a new instance of a chore of the same type to be assigned
     */
    fun completeChore(completedChore: Chore) {
        val currentList = _choresList.value.toMutableList()
        val choreIndex = currentList.indexOfFirst { it.choreID == completedChore.choreID }
        val recurringType = recurringChoresList!!.filter {
            chore -> chore.recurringChoreId == completedChore.instanceOf
        }

        val chore = currentList[choreIndex]
        currentList[choreIndex] = chore.copy(completed = true)

        if (chore.instanceOf.isNotBlank()) {
            val df = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
            val calendar = Calendar.getInstance()
            try {
                calendar.time = df.parse(chore.dueDate)!! // not null...
            } catch (e: Exception) {
                calendar.time = Date()
            }

            calendar.add(Calendar.DAY_OF_YEAR, recurringType[0].cycleFrequency)
            val newDueDate = df.format(calendar.time)
            val newChore = chore.copy( // the same previous chore (so type is preserved)
                choreID = UUID.randomUUID().toString(),
                assignedToId = "",     // unassigned
                completed = false,
                dueDate = newDueDate
            )

            currentList.add(newChore)
        }

        // update UI
        _choresList.value = currentList

//        _choresList.value = _choresList.value.map { chore ->
//            if (chore.choreID == completedChore.choreID) chore.copy(completed = true) else chore
//        }
        viewModelScope.launch {
            firestoreRepository.markChoreAsCompletedSuspend(
                completedChore.choreID,
                completedChore.householdID
            )
        }
    }
    /**
     * function to get the chore(s) assigned to the specified person
     */
    fun getChoresFor(person: String): List<Chore> {
        return _choresList.value.filter { it.assignedToId == person }
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

    fun onPhotoCaptured(choreId: String, householdID: String, uri: Uri, context: Context) {
        val supabaseClient = SupabaseClientProvider.client

        //launch coroutine to store the image in supabase
        viewModelScope.launch(Dispatchers.IO) {
            try {
                //get byte array for the image in order to store it
                val imageBytes = context.contentResolver.openInputStream(uri)?.readBytes()
                if (imageBytes == null) {
                    Log.e("ChoresViewModel", "Could not read image bytes from Uri.")
                    // Optionally revert chore status or show an error
                    return@launch
                }

                // TODO: get the actual householdID instead of this placeholder
                val path = "${householdID}/${choreId}.jpg"

                //write the byte to supabase database
                supabaseClient.storage.from("chore_photos").upload(path, imageBytes, upsert = true)

                //get signed url to display the image
                var storedUrl =
                    supabaseClient.storage.from("chore_photos").createSignedUrl(path, 120.minutes)
                storedUrl = BuildConfig.SUPABASE_URL + "/storage/v1/" + storedUrl

                //store the uri for this chore so it can be accessed and displayed
                launch(Dispatchers.Main) {
                    _choreImageUris.value = _choreImageUris.value.plus(choreId to storedUrl.toUri())
                    _tempImageUri.value = null
                }
            } catch (e: Exception) {
                Log.e("ChoresViewModel", "Error uploading image: ${e.message}", e)
            }
        }

    }

    fun clearTempImageUri() {
        _tempImageUri.value = null
    }

    // return uri for the chore image if it exists
    suspend fun getChoreImageUri(choreId: String, householdID: String): Uri? {
        val supabaseClient = SupabaseClientProvider.client
        // TODO: Get the actual household ID in here
        val path = "${householdID}/${choreId}.jpg"

        return try {
            var signedUrl = supabaseClient.storage.from("chore_photos").createSignedUrl(path, 120.minutes)
            signedUrl = BuildConfig.SUPABASE_URL + "/storage/v1/" + signedUrl
            signedUrl.toUri()
        }catch (e: Exception){
            Log.e("ChoresViewModel", "Error getting image URI: ${e.message}", e)
            null
        }
    }

    // Update your existing completeChore method to accept an optional imageUri
    fun completeChoreWithPhoto(chore: Chore, imageUri: Uri, context: Context) {
        completeChore(chore)
        onPhotoCaptured(chore.choreID, chore.householdID, imageUri, context)
    }
}