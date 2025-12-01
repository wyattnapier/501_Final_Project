package com.example.a501_final_project.chores

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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


/**
 * chore should include an id for easy maintainence
 * title and description for what the chore is
 * dueDate to keep track of when they are due
 * assignee to indicate who it is assigned to
 * completed to indicate if chore is completed
 */
data class Chore(
    val choreID: Int,
    val userID: Number,
    val householdID: Number,
    val name: String,
    val description: String,
    var dueDate: String,
    var assignedTo: String,
    var completed: Boolean,
    var priority: Boolean,
)

class ChoresViewModel : ViewModel() {
    val roommates = listOf("Alice", "Wyatt", "Tiffany")
    private val _choresList = MutableStateFlow<List<Chore>>(
        listOf(
            Chore(
                choreID = 1,
                name = "Wash Dishes",
                description = "Clean all dishes, utensils, and pots used during dinner.",
                assignedTo = "Alice",
                householdID = 1,
                userID = 1,
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
                userID = 2,
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
                userID = 3,
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
                userID = 4,
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
                userID = 5,
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
        _choresList.value = _choresList.value.mapIndexed { index, chore ->
            chore.copy(assignedTo = roommates[index % roommates.size])
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

    fun onPhotoCaptured(choreId: Int, uri: Uri, context: Context) {
        val supabaseClient = SupabaseClientProvider.client

        //launch coroutine to store the image in supabase
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imageBytes = context.contentResolver.openInputStream(uri)?.readBytes()
                if (imageBytes == null) {
                    Log.e("ChoresViewModel", "Could not read image bytes from Uri.")
                    // Optionally revert chore status or show an error
                    return@launch
                }

                // TODO: get the actual householdID instead of this placeholder
                val path = "householdid/${choreId}.jpg"

                supabaseClient.storage.from("chore_photos").upload(path, imageBytes, upsert = true)

                var storedUrl =
                    supabaseClient.storage.from("chore_photos").createSignedUrl(path, 120.minutes)

                storedUrl = BuildConfig.SUPABASE_URL + "/storage/v1/" + storedUrl

                //store the uri for this chore so it can be accessed and displayed
                launch(Dispatchers.Main) {
                    _choreImageUris.value += (choreId to storedUrl.toUri())
                    _tempImageUri.value = null

                    Log.d("ChoresViewModel", "Stored image at: $storedUrl")
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
    suspend fun getChoreImageUri(choreId: Int): Uri? {
        // TODO: Get the actual household ID in here
        val path = "householdid/${choreId}.jpg"

        return try {
            val supabaseClient = SupabaseClientProvider.client
            var signedUrl = supabaseClient.storage.from("chore_photos").createSignedUrl(path, 120.minutes)
            Log.d("ChoresViewModel", "Signed URL for $choreId: $signedUrl")
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
        onPhotoCaptured(chore.choreID, imageUri, context)
    }
}