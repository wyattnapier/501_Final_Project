package com.example.a501_final_project.chores

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.a501_final_project.FirestoreRepository
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
    val choreID: Int,
    val userID: String,
    val householdID: Number,
    val name: String,
    val description: String,
    var dueDate: String,
    var assignedTo: String,
    var completed: Boolean,
    var priority: Boolean,
)

class ChoresViewModel(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {
    private val _roommates = MutableStateFlow<List<String>>(emptyList())
    val roommates: StateFlow<List<String>> = _roommates.asStateFlow()
    // TODO: GET FROM DB
    //val roommates = listOf("Alice", "Wyatt", "Tiffany") // TODO: replace with actual names from db

    // TODO: GET FROM DB
    private val _choresList = MutableStateFlow<List<Chore>>(
        listOf(
            Chore(
                choreID = 1,
                name = "Wash Dishes",
                description = "Clean all dishes, utensils, and pots used during dinner.",
                assignedTo = "Alice",
                householdID = 1,
                userID = "oLt5u6tuVlXWUmlh6MWEbur1Sup2",
                dueDate = "November 15, 2025",
                completed = true,
                priority = true
            ),
            Chore(
                choreID = 2,
                name = "Vacuum Living Room",
                description = "Vacuum the carpet and under the furniture in the living room.",
                assignedTo = "Bob",
                householdID = 1,
                userID = "2",
                dueDate = "November 15, 2025",
                completed = false,
                priority = true
            ),
            Chore(
                choreID = 3,
                name = "Laundry",
                description = "Wash, dry, and fold all the clothes from the laundry basket.",
                assignedTo = "Charlie",
                householdID = 1,
                userID = "3",
                dueDate = "November 15, 2025",
                completed = true,
                priority = true,
            ),
            Chore(
                choreID = 4,
                name = "Take Out Trash",
                description = "Empty all trash bins and take the garbage out to the curb.",
                assignedTo = "Dana",
                householdID = 1,
                userID = "4",
                dueDate = "November 15, 2025",
                completed = false,
                priority = true,
            ),
            Chore(
                choreID = 5,
                name = "Clean Bathroom",
                description = "Scrub the sink, toilet, and shower, and mop the bathroom floor.",
                assignedTo = "Eve",
                householdID = 1,
                userID = "5",
                dueDate = "November 15, 2025",
                completed = false,
                priority = true,
            )
        )
    )

    var choresList: StateFlow<List<Chore>> = _choresList.asStateFlow()
    private val _showPrevChores = MutableStateFlow(false)
    val showPrevChores: StateFlow<Boolean> = _showPrevChores.asStateFlow()
    // Map to store image URIs for each chore by choreId
    private val _choreImageUris = MutableStateFlow<Map<Int, Uri>>(emptyMap())
    val choreImageUris: StateFlow<Map<Int, Uri>> = _choreImageUris.asStateFlow()

    // Temporary URI for camera capture
    private val _tempImageUri = MutableStateFlow<Uri?>(null)
    val tempImageUri: StateFlow<Uri?> = _tempImageUri.asStateFlow()

    init {
        loadResidentsList()
    }

    fun loadResidentsList() {
        firestoreRepository.getHouseholdResidentsWithoutId(
            onSuccess = { residents ->
                Log.d("ChoresViewModel", "Loaded residents: $residents")
                _roommates.value = emptyList()
                _roommates.value = (residents.map { it.toString() })
            },
            onFailure = { exception ->
                // Handle failure
                Log.d("ChoresViewModel", "Failed to load residents: $exception")
            }
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
     * function to change priority fo a chore.
     * currently making chores that are due sooner higher priority
     * Should call this function when we load in chores/onResume()?
     */
    fun changePriority(chore : Chore) {
        val dateFormat =
            SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH) // e.g. "November 15, 2024"
        val today = Calendar.getInstance() // Use fully qualified name
        val due = Calendar.getInstance() // Use fully qualified name
        val dueDate: Date = dateFormat.parse(chore.dueDate) ?: return
        due.time = dueDate
        val diffMillis = due.timeInMillis - today.timeInMillis
        val daysUntilDue = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        chore.priority = daysUntilDue in 0..5
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

    fun onPhotoCaptured(choreId: Int, uri: Uri) {
        // Save the image URI for this chore
        _choreImageUris.value += (choreId to uri)
        // Clear the temp URI
        _tempImageUri.value = null
    }

    fun clearTempImageUri() {
        _tempImageUri.value = null
    }

    fun getChoreImageUri(choreId: Int): Uri? {
        return _choreImageUris.value[choreId]
    }

    // Update your existing completeChore method to accept an optional imageUri
    fun completeChoreWithPhoto(chore: Chore, imageUri: Uri) {
        onPhotoCaptured(chore.choreID, imageUri)
        completeChore(chore)
    }
}