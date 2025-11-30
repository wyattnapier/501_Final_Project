package com.example.a501_final_project.payment

import androidx.lifecycle.ViewModel
import com.example.a501_final_project.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// custom data object created here for now
data class PaymentUser(
    val username: String,
    val email: String,
    val venmoUsername: String,
)

/**
 * payment class should include id for maintenance
 * payTo tracks who the payment should go to
 * payFrom tracks who is sending the payment
 * amount indicates the amount the payment is for
 * memo stores the memo message for the payment
 * paid indicates if payment has been paid
 * date ?
 */
data class Payment(
    val id: Int,
    val payTo: String, // data types may change depending on how we represent this?
    val payFrom: String,
    val amount: Double,
    val memo: String,
    var paid: Boolean,
    val recurring: Boolean
)

class PaymentViewModel(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    // TODO: GET FROM DB AND ADD BETTER NAME TO VARIABLE
    val users = listOf(
        PaymentUser("alice_username", "william.henry.harrison@example-pet-store.com", "alice_venmo"),
        PaymentUser("tiffany_username", "william.henry.harrison@example-pet-store.com", "tiffany_venmo"),
        PaymentUser("john_username", "william.henry.moody@my-own-personal-domain.com", "john_venmo")
    )

    // pay viewmodel portion
    // TODO: GET FROM DATABASE
    private val _paymentsList = MutableStateFlow<List<Payment>>(listOf(
        Payment(
            0,
            "tiffany_username",
            "alice_username",
            85.50,
            "Dinner",
            paid = false,
            recurring = false
        ),
        Payment(1, "alice_username", "Wyatt", 15.50, "Dinner", paid = false, recurring = false),
        Payment(2, "tiffany_username", "Wyatt", 25.00, "Utilities", paid = false, recurring = true),
        Payment(3, "john_username", "Wyatt", 100.25, "Rent", paid = false, recurring = false),
        Payment(4, "Wyatt", "john_username", 100.25, "Rent", paid = true, recurring = false),
        Payment(5, "john_username", "Wyatt", 100.25, "Rent", paid = true, recurring = false),
    )
    )

    var paymentsList: StateFlow<List<Payment>> = _paymentsList.asStateFlow()

    // forcing a past payment, otherwise empty
    private val _pastPayments = MutableStateFlow<List<Payment>>(listOf(
        Payment(0, "Wyatt", "alice_username", 85.50, "Dinner", paid = true, recurring = false),
    ))
    val pastPayments: StateFlow<List<Payment>> = _pastPayments
    private val _showPastPayments = MutableStateFlow(false)
    val showPastPayments: StateFlow<Boolean> = _showPastPayments.asStateFlow()


    /**
     * function to add payments to active payments
     * this function should be used more frequently than add chore since
     * chores are set at creating while payment is constantly changing (either recurring or one time)?
     *
     * hypothetically when someone inputs payment information, they can select who has to pay?
     * so the payFrom may be a list and we add some number of payments based on the calculation
     *
     * eventually we can determine weighting of payments for things like room size?
     */
    fun addPayment(payTo: String, payFrom: List<String>, amount: Double, memo: String, recurring: Boolean) {
        val amountPerPerson = amount / payFrom.size
        for (name in payFrom) {
            val newPayment = Payment(
                id = paymentsList.value.size + 1,
                payTo = payTo,
                payFrom = name,
                amount = amountPerPerson,
                memo = memo,
                paid = false,
                recurring = recurring
            )
            _paymentsList.value += newPayment
        }
    }

    /**
     * function to remove payment, either to get rid of it or upon completion
     */
    fun removePayment(payment: Payment) {
        _paymentsList.value = _paymentsList.value.filter { it != payment }
    }


    /**
     * function to mark a payment as paid
     */
    fun completePayment(payment: Payment) {
        // payment should already be in list so we can access
        val updatedPayment = payment.copy(paid = true)

        // Add to pastPayments
        _pastPayments.value += updatedPayment

        // Remove from active paymentList
        _paymentsList.value = _paymentsList.value.filter { it != payment }
    }

    /**
     * function to get payments assigned to specified person
     */
    fun getPaymentsFor(person: String): List<Payment> {
        return paymentsList.value.filter { it.payTo == person }
    }

    /**
     * function to get payments from a specific person
     * this will be used for payments they need to make
     */
    fun getPaymentsFrom(person: String): List<Payment> {
        return paymentsList.value.filter { it.payFrom == person }
    }

    /**
     * function to get venmo user name of a user
     */
    fun getVenmoUsername(person: String): String? {
        val user: PaymentUser? = users.find { it.username == person }
        return user?.venmoUsername // TODO: should return an error message if null
    }

    /**
     * function to toggle if past payments are shown
     */
    fun toggleShowPastPayments() {
        _showPastPayments.value = !_showPastPayments.value
    }
}