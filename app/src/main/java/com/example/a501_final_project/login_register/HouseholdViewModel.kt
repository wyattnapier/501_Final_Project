package com.example.a501_final_project.login_register

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a501_final_project.FirestoreRepository
import com.example.a501_final_project.chores.RecurringChore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ChoreInput(
    var name: String = "",
    var description: String = "",
    var cycle: Number = 0
)

data class PaymentInput(
    var name: String = "",
    var amount: Number = 0,
    var split: Number = 0,
    var cycle: Number = 0,
    var youPay: Boolean = true
)

data class PaymentDB(
    var name: String = "",
    var amount: Number = 0,
    var cycle: Number = 0,
    var paid_by: String = "",
    var occupiedSplit: Number = 0,
    var split: Number = 0,
    var youPay: Boolean = false
)

data class ResidentDB(
    var id: String = "",
    var payment_percents: List<Number> = listOf()
)

class HouseholdViewModel(
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    var existingHousehold by mutableStateOf<Boolean?>(null)

    // Get current user ID from repository
    var uid by mutableStateOf("")
        private set

    var setupStep by mutableIntStateOf(0)
    var householdName by mutableStateOf("")
    var choreInputs = mutableStateListOf(ChoreInput())
        private set

    var paymentInputs = mutableStateListOf(PaymentInput())
        private set

    var calendarName by mutableStateOf("")
        private set

    var householdCreated by mutableStateOf(false)
        private set

    var householdID by mutableStateOf("")
        private set

    var errorMessage by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)
        private set

    val paymentsFromDB = mutableStateListOf<PaymentDB>()

    val residentsFromDB = mutableStateListOf<ResidentDB>()
    var gotHousehold by mutableStateOf(false)

    fun loadCurrentUserId() {
        // Load current user ID when ViewModel is created
        uid = repository.getCurrentUserId() ?: ""
        Log.d("HouseholdViewModel", "Initialized with user ID: $uid")
    }

    fun incrementStep(){
        setupStep++
    }

    fun decrementStep(){
        setupStep--
    }

    fun updateID(newID: String) {
        householdID = newID
    }

    fun updateName(newName: String) {
        householdName = newName
    }

    fun addChore() {
        choreInputs.add(ChoreInput())
    }

    fun updateChore(index: Int, update: ChoreInput){
        choreInputs[index] = update
    }

    fun addPayment() {
        paymentInputs.add(PaymentInput())
    }

    fun updatePayment(index: Int, update: PaymentInput){
        paymentInputs[index] = update
    }

    fun updateCalendar(calendar: String){
        calendarName = calendar
    }

    fun updatePaymentDB(index: Int, update: PaymentDB){
        paymentsFromDB[index] = update
    }

    fun createHousehold() {
        errorMessage = null
        isLoading = true

        val recurring_chores = choreInputs.map { chore ->
            mapOf(
                "name" to chore.name,
                "description" to chore.description,
                "cycle" to chore.cycle,
            )
        }

        val recurring_payments = paymentInputs.map { payment ->
            mapOf(
                "name" to payment.name,
                "amount" to payment.amount,
                "cycle" to payment.cycle,
                "paid_by" to if(payment.youPay) uid else null
            )
        }

        val payment_split = paymentInputs.map { it.split }

        val residents = listOf(
            mapOf(
                "id" to uid,
                "payment_percents" to payment_split
            )
        )

        // to actualy add chores?

        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
        val calendar = Calendar.getInstance()


        val initialChores = recurring_chores.mapIndexed { index, rc ->

            val daysFromNow = (rc["cycle"] as? Number)?.toInt() ?: 7
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, daysFromNow)
            val dueDate = dateFormat.format(calendar.time)

            mapOf(
                "chore_id" to UUID.randomUUID().toString(),
                "name" to rc["name"],
                "description" to rc["description"],
                "completed" to false,
                "assignedToId" to "",
                "assignedToName" to "",
                "dueDate" to dueDate, // OR generate now
                "recurring_chore_id" to index,
                "recurring" to true
            )
        }

        val fullHouseholdObject = mapOf(
            "name" to householdName,
            "recurring_chores" to recurring_chores,
            "recurring_payments" to recurring_payments,
            "calendar" to calendarName,
            "residents" to residents,
//            "chores" to emptyList<Map<String, Any>>()  // Initialize empty chores array
            "chores" to initialChores
        )

        Log.d("HouseholdViewModel", "Creating household with name: $householdName")

        viewModelScope.launch {
            try {
                // 1. Call the suspend function and directly assign the returned ID
                val newHouseholdId = repository.createHouseholdSuspend(fullHouseholdObject)

                Log.d("HouseholdViewModel", "Created household $newHouseholdId")
                householdID = newHouseholdId // Assign the ID to the ViewModel's state

                // 2. Update user document with the new household_id
                repository.updateUserHouseholdIdSuspend(uid, newHouseholdId)

                householdCreated = true

            } catch (e: Exception) {
                Log.e("HouseholdViewModel", "Error creating household", e)
                errorMessage = "Failed to create household: ${e.message}"
            } finally {
                isLoading = false // Ensure loading is always turned off
            }
        }
    }

    // This was the first addToHousehold()
    fun getHouseholdForJoining(householdId: String) {
        updateID(householdId)

        viewModelScope.launch {
            try {
                isLoading = true
                // This will throw an exception if householdID is not found
                val householdData = repository.getHouseholdSuspend(householdID)

                householdName = householdData["name"] as? String ?: "Household Not Found"
                Log.d("HouseholdViewModel", "Household data: $householdData")

                // Parse payments
                val paymentsList = householdData["recurring_payments"] as? List<Map<String, Any>>
                paymentsList?.let { list ->
                    paymentsFromDB.clear()
                    paymentsFromDB.addAll(
                        list.map { map ->
                            PaymentDB(
                                name = map["name"] as? String ?: "",
                                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                                cycle = (map["cycle"] as? Number)?.toDouble() ?: 0.0,
                                paid_by = (map["paid_by"] as? String) ?: ""
                            )
                        }
                    )
                }

                // Parse residents
                val residentsList = householdData["residents"] as? List<Map<String, Any>>
                residentsList?.let { list ->
                    residentsFromDB.clear()
                    residentsFromDB.addAll(
                        list.map { map ->
                            ResidentDB(
                                id = map["id"] as? String ?: "",
                                payment_percents = (map["payment_percents"] as? List<Number>) ?: listOf()
                            )
                        }
                    )
                }

                // Calculate occupied splits
                for (paymentIndex in paymentsFromDB.indices) {
                    val totalTaken = residentsFromDB.sumOf { resident ->
                        resident.payment_percents.getOrNull(paymentIndex)?.toDouble() ?: 0.0
                    }
                    paymentsFromDB[paymentIndex] = paymentsFromDB[paymentIndex].copy(
                        occupiedSplit = totalTaken
                    )
                }

                updateID(householdID)
                gotHousehold = true
            } catch(e: Exception) {
                errorMessage = "Failed to find household: ${e.message}"
                Log.e("HouseholdViewModel", "Error fetching household for joining", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun confirmJoinHousehold() {
        errorMessage = null
        isLoading = true

        val userPaymentPercents: List<Number> = paymentsFromDB.map { payment -> payment.split }
        val newResident = mapOf(
            "id" to uid,
            "payment_percents" to userPaymentPercents
        )

        val paymentsMap = paymentsFromDB.map { payment ->
            val paid_by_value = when {
                payment.youPay -> uid
                payment.paid_by.isEmpty() -> null
                else -> payment.paid_by
            }
            mapOf(
                "name" to payment.name,
                "amount" to payment.amount,
                "cycle" to payment.cycle,
                "paid_by" to paid_by_value
            )
        }

        Log.d("HouseholdViewModel", "Resident to add: $newResident")
        Log.d("HouseholdViewModel", "Household ID: $householdID")

        viewModelScope.launch {
            try {
                // Call the refactored suspend function
                repository.addResidentToHouseholdSuspend(
                    householdId = householdID,
                    residentData = newResident,
                    paymentsData = paymentsMap as List<Map<String, Any>>,
                )

                // Update user document with household_id
                repository.updateUserHouseholdIdSuspend(uid, householdID)

                Log.d("HouseholdViewModel", "Successfully joined household and updated user profile")
                householdCreated = true

            } catch (e: Exception) {
                Log.e("HouseholdViewModel", "Failed to join household", e)
                errorMessage = "Failed to join household: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }


    /**
     * Load household data using the repository (for when user is already in a household)
     */
    fun loadCurrentUserHousehold() {
        viewModelScope.launch {
            try {
                isLoading = true
                val (householdId, householdData) = repository.getHouseholdWithoutIdSuspend()

                householdID = householdId
                householdName = householdData["name"] as? String ?: "Unknown Household"

                Log.d("HouseholdViewModel", "Loaded household: $householdName ($householdId)")

                gotHousehold = true
                isLoading = false

            } catch (e: Exception) {
                Log.e("HouseholdViewModel", "Error loading current household", e)
                errorMessage = "Failed to load household: ${e.message}"
                isLoading = false
            }
        }
    }
}