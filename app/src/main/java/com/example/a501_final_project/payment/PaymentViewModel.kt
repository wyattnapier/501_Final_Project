package com.example.a501_final_project.payment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a501_final_project.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale


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
    val recurring: Boolean
)

class PaymentViewModel(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val _paymentsList = MutableStateFlow<List<Payment>>(emptyList())
    var paymentsList: StateFlow<List<Payment>> = _paymentsList.asStateFlow()

    private val _pastPayments = MutableStateFlow<List<Payment>>(emptyList())
    val pastPayments: StateFlow<List<Payment>> = _pastPayments.asStateFlow()

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

    fun loadPaymentsData() {
        if (_isLoading.value) {
            Log.w("PaymentViewModel", "Already loading, skipping duplicate call")
            return
        }
        if (_isPaymentsDataLoaded.value) {
            Log.w("PaymentViewModel", "Payments data already loaded, skipping")
            return
        }

        _isLoading.value = true
        Log.d("PaymentViewModel", "Starting to load payments data...")

        viewModelScope.launch {
            try {
                val (householdId, data) = firestoreRepository.getHouseholdWithoutIdSuspend()
                Log.d("PaymentViewModel", "Loaded household data: $householdId")

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
                            recurring = false
                        )
                    }

                    // Separate into active and past payments
                    val activePayments = parsedPayments.filter { !it.paid }
                    val pastPaymentsList = parsedPayments.filter { it.paid }

                    _paymentsList.value = activePayments
                    _pastPayments.value = pastPaymentsList

                    Log.d("PaymentViewModel", "Successfully loaded ${activePayments.size} active and ${pastPaymentsList.size} past payments")
                }

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
     * function to add payments to active payments
     */
    fun addPayment(payTo: String, payFrom: List<String>, amount: Double, memo: String, recurring: Boolean) {
        val amountPerPerson = amount / payFrom.size
        for (name in payFrom) {
            val newPayment = Payment(
                id = (paymentsList.value.size + 1).toString(),
                payToId = payTo,
                payToName = null,  // TODO: Fetch name
                payToVenmoUsername = null,  // TODO: Fetch venmo username
                payFromId = name,
                payFromName = null,  // TODO: Fetch name
                amount = amountPerPerson,
                memo = memo,
                dueDate = null,
                datePaid = null,
                paid = false,
                recurring = recurring
            )
            _paymentsList.value += newPayment
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
    fun getPaymentsFor(personId: String): List<Payment> {
        return paymentsList.value.filter { it.payToId == personId }
    }

    /**
     * function to get payments from a specific person (by ID)
     */
    fun getPaymentsFrom(personId: String): List<Payment> {
        return paymentsList.value.filter { it.payFromId == personId }
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