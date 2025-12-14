package com.example.a501_final_project.payment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a501_final_project.FirestoreRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID


data class Payment(
    val id: String,
    val payToId: String,
    val payToName: String?,
    val payToVenmoUsername: String?,
    val payFromId: String,
    val payFromName: String?,
    val amount: Double,
    val memo: String,
    val dueDate: String?,
    val datePaid: String?,
    var paid: Boolean,
    val recurring: Boolean,
    val instanceOf: Int?, // need to know which recurring payment it is an instance of
)

// data class for recurring payments
data class RecurringPayment(
    val amount: Double,
    val cycle: Int,
    val name: String,
    val paidById: String,
)

class PaymentViewModel(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val _paymentsList = MutableStateFlow<List<Payment>>(emptyList())
    var paymentsList: StateFlow<List<Payment>> = _paymentsList.asStateFlow()

    private val _pastPayments = MutableStateFlow<List<Payment>>(emptyList())
    val pastPayments: StateFlow<List<Payment>> = _pastPayments.asStateFlow()

    private val _allPayments = MutableStateFlow<List<Payment>>(emptyList())
    val allPayments: StateFlow<List<Payment>> = _allPayments.asStateFlow()

    private val _recurringPayments = MutableStateFlow<List<RecurringPayment>>(emptyList())
    val recurringPayments: StateFlow<List<RecurringPayment>> = _recurringPayments.asStateFlow()

    private val _roommates = MutableStateFlow<List<String>>(emptyList())
    val roommates: StateFlow<List<String>> = _roommates.asStateFlow()

    // need to trakc percentages since roommates ha sjust Ids
    private val _roommatePercents = MutableStateFlow<Map<String, List<Double>>>(emptyMap())
    val roommatePercents: StateFlow<Map<String, List<Double>>> = _roommatePercents.asStateFlow()




    private val _showPastPayments = MutableStateFlow(false)
    val showPastPayments: StateFlow<Boolean> = _showPastPayments.asStateFlow()

    private val _isPaymentsDataLoaded = MutableStateFlow(false)
    val isPaymentsDataLoaded: StateFlow<Boolean> = _isPaymentsDataLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Helper function for type conversion
    private fun Any?.toStringOrNull(): String? {
        return when (this) {
            is String -> this
            is Number -> this.toString()
            else -> null
        }
    }

    fun createNewPayment(
        payFromId: String,
        payToId: String,
        amount: Double,
        memo: String,
        dueDate: Date?
    ) {
        var newPaymentId: String? = null
        viewModelScope.launch {
            try {
                val householdId = firestoreRepository.getHouseholdIdForUserSuspend(payFromId)
                newPaymentId = System.currentTimeMillis().toString() // create new id
                val payFromName = firestoreRepository.getUserSuspend(payFromId)["name"] as? String
                val payToUserData = firestoreRepository.getUserSuspend(payToId)
                val payToName = payToUserData["name"] as? String
                val payToVenmo = payToUserData["venmoUsername"] as? String
                val dueDateTimestamp = dueDate?.let { Timestamp(it) }

                // to go in the database
                val newPaymentData = mapOf(
                    "id" to newPaymentId,
                    "pay_from" to payFromId,
                    "pay_to" to payToId,
                    "amount" to amount,
                    "memo" to memo,
                    "paid" to false,
                    "date_paid" to null,
                    "due_date" to null,
                    "due_date" to dueDateTimestamp,
                ) as Map<String, Any>

                // to be used to quickly update the ui's payment list
                val newPaymentObject = Payment(
                    id = newPaymentId,
                    payFromId = payFromId,
                    payFromName = payFromName ?: "Unknown",
                    payToId = payToId,
                    payToName = payToName ?: "Unknown",
                    payToVenmoUsername = payToVenmo,
                    amount = amount,
                    memo = memo,
                    dueDate = dueDate?.let { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(it) },
                    datePaid = null,
                    paid = false,
                    recurring = false,
                    instanceOf = null
                )
                _paymentsList.value = listOf(newPaymentObject) + _paymentsList.value // optimistic update before writing to database

                firestoreRepository.addNewPaymentToHousehold(householdId, newPaymentData)

            } catch (e: Exception) {
                Log.e("PaymentViewModel", "Failed to create new payment", e)
                if (newPaymentId != null) {
                    _paymentsList.value =
                        _paymentsList.value.filter { it.id != newPaymentId } // if firestore write fails remove new payment
                }
            }
        }
    }

    // Also, modify loadPaymentsData to allow forcing a reload
    fun loadPaymentsData(forceReload: Boolean = false) {
        if (_isLoading.value) {
            Log.w("PaymentViewModel", "Already loading, skipping duplicate call")
            return
        }
        if (_isPaymentsDataLoaded.value && !forceReload) { // Check the forceReload flag
            Log.w("PaymentViewModel", "Payments data already loaded, skipping")
            return
        }

        _isLoading.value = true
        _isPaymentsDataLoaded.value = false // Reset while loading
        Log.d("PaymentViewModel", "Starting to load payments data...")

        viewModelScope.launch {
            try {
                val (householdId, data) = firestoreRepository.getHouseholdWithoutIdSuspend()
                Log.d("PaymentViewModel", "Loaded household data: $householdId")

                // Parse residents
                val residentsAnyList = data["residents"] as? List<*>
                if (residentsAnyList == null) {
                    Log.w("PaymentViewModel", "'residents' field is null")
                    _roommates.value = emptyList()
                    _roommatePercents.value = emptyMap()
                } else {
                    val residentIds = residentsAnyList.mapNotNull { item ->
                        val residentMap = item as? Map<*, *>
                        residentMap?.get("id")?.toString()
                    }
                    val residentPercents = residentsAnyList.mapNotNull { resident ->
                        val residentMap = resident as? Map<*, *>
                        val resId = residentMap?.get("id") as? String ?: return@mapNotNull null
                        val percents = (residentMap.get("payment_percents") as? List<*>)
                            ?.mapNotNull { (it as? Number)?.toDouble() }
                            ?: emptyList()
                        resId to percents
                    }.toMap()
                    _roommates.value = residentIds
                    _roommatePercents.value = residentPercents
                    Log.d("PaymentViewModel", "Loaded ${residentIds.size} residents")
                }

                // Load recurring payment information
                val recurringPaymentsList = data["recurring_payments"] as? List<*>
                val recurringPaymentsListTemp: List<RecurringPayment>? = recurringPaymentsList?.mapIndexedNotNull { index, item ->
                    val itemAsMap = item as? Map<*, *> ?: return@mapIndexedNotNull null
                    RecurringPayment(
                        amount = itemAsMap["amount"] as? Double ?: return@mapIndexedNotNull null,
                        cycle= ((itemAsMap["cycle"] ?: itemAsMap["cycle_frequency"]) as? Number)?.toInt() ?: return@mapIndexedNotNull null,
                        name = itemAsMap["name"] as? String ?: return@mapIndexedNotNull null,
                        paidById = itemAsMap["paid_by"] as? String ?: return@mapIndexedNotNull null,
                    )
                }

                _recurringPayments.value = recurringPaymentsListTemp ?: emptyList()



                // Parse payments
                val paymentsListFromDB = data["payments"] as? List<*>
                if (paymentsListFromDB == null) {
                    Log.w("PaymentViewModel", "'payments' field is null")
                    _paymentsList.value = emptyList()
                } else {
                    Log.d("PaymentViewModel", "Processing ${paymentsListFromDB.size} payments")

                    val parsedPayments = paymentsListFromDB.mapIndexedNotNull { index, item ->
                        val itemAsMap = item as? Map<*, *> ?: return@mapIndexedNotNull null

                        Log.d("PaymentViewModel", "Processing payment $index: $itemAsMap")

                        // Get payment ID
                        val paymentId = itemAsMap["id"].toStringOrNull()
                            ?: return@mapIndexedNotNull null

                        // Get amount
                        val amount = (itemAsMap["amount"] as? Number)?.toDouble()
                            ?: return@mapIndexedNotNull null

                        // Get memo
                        val memo = itemAsMap["memo"] as? String ?: ""

                        // Get paid status
                        val paid = itemAsMap["paid"] as? Boolean ?: false

                        // Parse due date
                        val dueDateValue = itemAsMap["due_date"]
                        val dueDate = when (dueDateValue) {
                            is String -> dueDateValue
                            is com.google.firebase.Timestamp -> {
                                val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                                sdf.format(dueDateValue.toDate())
                            }
                            null -> null
                            else -> null
                        }

                        // Parse date paid
                        val datePaidValue = itemAsMap["date_paid"]
                        val datePaid = when (datePaidValue) {
                            is String -> datePaidValue
                            is com.google.firebase.Timestamp -> {
                                val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                                sdf.format(datePaidValue.toDate())
                            }
                            null -> null
                            else -> null
                        }

                        // Parse pay_from (can be String or DocumentReference)
                        val payFromId = when (val payFromValue = itemAsMap["pay_from"]) {
                            is String -> payFromValue
                            is com.google.firebase.firestore.DocumentReference -> payFromValue.id
                            else -> payFromValue.toStringOrNull()
                        } ?: return@mapIndexedNotNull null

                        // Fetch pay_from user name
                        val payFromName = try {
                            val userData = firestoreRepository.getUserSuspend(payFromId)
                            userData["name"] as? String ?: payFromId
                        } catch (e: Exception) {
                            Log.w("PaymentViewModel", "Failed to get name for user $payFromId: $e")
                            payFromId
                        }

                        // Parse pay_to (can be String or DocumentReference)
                        val payToId = when (val payToValue = itemAsMap["pay_to"]) {
                            is String -> payToValue
                            is com.google.firebase.firestore.DocumentReference -> payToValue.id
                            else -> payToValue.toStringOrNull()
                        } ?: return@mapIndexedNotNull null

                        // Fetch pay_to user data (name AND venmo username)
                        val (payToName, payToVenmoUsername) = try {
                            val userData = firestoreRepository.getUserSuspend(payToId)
                            val name = userData["name"] as? String ?: payToId
                            val venmo = userData["venmoUsername"] as? String
                            Pair(name, venmo)
                        } catch (e: Exception) {
                            Log.w("PaymentViewModel", "Failed to get data for user $payToId: $e")
                            Pair(payToId, null)
                        }

                        val instanceOf =
                            (itemAsMap["recurring_payment_id"] as? Number)?.toInt()

                        val recurring =
                            itemAsMap["recurring"] as? Boolean ?: (instanceOf != null)

                        Log.d("PaymentViewModel", "Payment $index: $payFromName pays $payToName (venmo: $payToVenmoUsername) $$amount for $memo")


                        Payment(
                            id = paymentId,
                            payFromId = payFromId,
                            payFromName = payFromName,
                            payToId = payToId,
                            payToName = payToName,
                            payToVenmoUsername = payToVenmoUsername,  // Store it here!
                            amount = amount,
                            memo = memo,
                            dueDate = dueDate,
                            datePaid = datePaid,
                            paid = paid,
                            recurring = recurring,
                            instanceOf = instanceOf,
                        )
                    }

                    // Separate into active and past payments
                    val activePayments = parsedPayments.filter { !it.paid }
                    val pastPaymentsList = parsedPayments.filter { it.paid }
                    val allPayments = parsedPayments

                    _paymentsList.value = activePayments
                    _pastPayments.value = pastPaymentsList
                    _allPayments.value = allPayments

                    Log.d("PaymentViewModel", "Successfully loaded ${activePayments.size} active and ${pastPaymentsList.size} past payments")
                }

                // call here?
                assignRecurringPayments(allPayments = _allPayments.value)

                _isPaymentsDataLoaded.value = true
                _isLoading.value = false
                Log.d("PaymentViewModel", "Load complete")

            } catch (e: Exception) {
                Log.e("PaymentViewModel", "Failed to load payments data", e)
                _isLoading.value = false
            }
        }
    }

    /**
     * function to assign recurring payment on correct cycle frequency and correct amount
     */
    suspend fun assignRecurringPayments(allPayments: List<Payment>) {
        val currentRoommates = roommates.value
        val recurringPayments = recurringPayments.value
        val currentPayments = _paymentsList.value.toMutableList()
        if (currentRoommates.isEmpty()) {
            Log.w("PaymentViewModel", "Cannot assign paymenrs, the roommates list is empty.")
            return
        }

        // today
        val df = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)

        // roommate id to names so we can build payments with name
        val idToNameMap = currentRoommates.associateWith { userId ->
            firestoreRepository.getUserSuspend(userId)["name"] as? String ?: "Unknown"
        }

        // roommate id to venmo map for building payments
        val idToVenmoMap = currentRoommates.associateWith { userId ->
            firestoreRepository.getUserSuspend(userId)["venmoUsername"] as? String
        }

        // for each recurring payment
        // create new payment instances
        // determine due date
        // build each roommates payment depending on percent
        // add to payments list
        for ((index, recurring) in recurringPayments.withIndex()) {
            // check if any instances, returns boolean
            val existing = allPayments.any {
                it.instanceOf == index
            }
            if (existing) {
                continue
            }
            // else none, so create

            // 1. Calculate due date
            val calendar = Calendar.getInstance()
            val lastPaymentDueDate = _paymentsList.value
                .filter { it.instanceOf == index }
                .maxByOrNull { it.dueDate?.let { df.parse(it)?.time } ?: 0L }
                ?.dueDate
            val today = Date()
            val baseDate = lastPaymentDueDate?.let { df.parse(it) } ?: today
            calendar.time = baseDate
            calendar.add(Calendar.DAY_OF_YEAR, recurring.cycle)
            val newDueDate = calendar.time

            val paidBy = currentRoommates.firstOrNull { it == recurring.paidById }
                ?: continue
            for (roommate in currentRoommates) {
                if (roommate != paidBy) {
                    val amount = recurring.amount * (roommatePercents.value[roommate]!![index] / 100)

                    val newPayment = Payment(
                        id = UUID.randomUUID().toString(),
                        payFromId = roommate, // pay from is who makes the payment
                        payFromName = idToNameMap[roommate],
                        payToId = paidBy,
                        payToName = idToNameMap[paidBy],
                        payToVenmoUsername = idToVenmoMap[paidBy],
                        amount = amount,
                        memo = recurring.name,
                        dueDate = df.format(newDueDate),
                        datePaid = null,
                        paid = false,
                        recurring = true,
                        instanceOf = index // since recurring ids are just their indices
                    )

                    currentPayments.add(newPayment)
                }

            }
        }

        // update UI/state
        _paymentsList.value = currentPayments

        // send to db
        viewModelScope.launch {
            try {
                val newRecurringPayments = currentPayments.filter { it.recurring }
                firestoreRepository.updatePaymentAssignments(newRecurringPayments)
                Log.d(
                    "PaymentsViewModel",
                    "Successfully assigned ${_paymentsList.value.size} payments"
                )
            } catch (e: Exception) {
                Log.e("PaymentsViewModel", "Failed to update assignments in DB", e)
            }
        }

    }

    /**
     * function to remove payment
     */
    fun removePayment(payment: Payment) {
        _paymentsList.value = _paymentsList.value.filter { it != payment }
    }

    /**
     * function to mark a payment as paid
     */
    fun completePayment(payment: Payment) {
        viewModelScope.launch {
            val updatedPayment = payment.copy(paid = true)
            _pastPayments.value += updatedPayment
            _paymentsList.value = _paymentsList.value.filter { it != payment }
            // update firestore db
            val householdId = firestoreRepository.getHouseholdIdForUserSuspend(payment.payFromId)
            firestoreRepository.markPaymentAsCompletedSuspend(payment.id, householdId)
        }
    }

    /**
     * function to get payments assigned to specified person (by ID)
     */
    fun getPaymentsFor(personId: String, payments: List<Payment>): List<Payment> {
        return payments.filter { it.payToId == personId }
    }

    /**
     * function to get payments from a specific person (by ID)
     */
    fun getPaymentsFrom(personId: String, payments: List<Payment>): List<Payment> {
        return payments.filter { it.payFromId == personId }
    }

    /**
     * function to toggle if past payments are shown
     */
    fun toggleShowPastPayments() {
        _showPastPayments.value = !_showPastPayments.value
    }

    // reset all state on logout
    fun reset() {
         _paymentsList.value = emptyList()
        _pastPayments.value = emptyList()
        _showPastPayments.value = false
        _isPaymentsDataLoaded.value = false
        _isLoading.value = false
    }
}