package com.example.a501_final_project

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.util.Calendar
import java.util.Date


// custom data object created here for now

/**
 * chore should include an id for easy maintainence
 * title and description for what the chore is
 * dueDate to keep track of when they are due
 * assignee to indicate who it is assigned to
 * completed to indicate if chore is completed
 */
data class Chore(
    val id: Int,
    val title: String,
    val description: String,
    var dueDate: Date, // trying this, can change later
    var assignee: String,
    var completed: Boolean,
    var priority: Boolean,
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

/** our ViewModel to hold our business logic and data.
 * For now this will be one ViewModel that all of the screens can access.
 * One this grows, we may break it down into ViewModels per screen,
 * but since Home view needs all this data we are doing one for now.
 **/

class MainViewModel : ViewModel() {

    // dummy household members....
    val roommates = listOf("Alice", "Wyatt", "Tiffany")

    // chores viewmodel portion
    val choresList : SnapshotStateList<Chore> = mutableStateListOf()

    // functions to help us maintain chores
    /**
     * function to be used when first initializing/creating the list of chores for the household
     */
    fun addChores(newChore : Chore) {
        choresList.add(newChore)
    }

    /**
     * function to assign chores to members of the household
     * eventually we should do this based on time?
     */
    fun assignChores() {
        // depending on how we want chores ot work/be assigned, btu currently round robin
        choresList.forEachIndexed { index, chore ->
            chore.assignee = roommates[index % roommates.size]
        }
    }

    /**
     * function to mark a chore as completed
     * ideally when user completes their chore,
     * this function is called and gets passed the chore
     * this function might not be needed, but it could help with safe access/storing logic in one place
     */
    fun completeChore(completedChore : Chore) {
        completedChore.completed = true // hypothetically if they pass the chore object in then we can jut do this...? since itll already be an object in the list
    }

    /**
     * function to get the chore(s) assigned to the specified person
     */
    fun getChoresFor(person: String): List<Chore> {
        return choresList.filter { it.assignee == person }
    }

    /**
     * function to change priority fo a chore.
     * currently making chores that are due sooner higher priority
     * Should call this function when we load in chores/onResume()?
     */
    fun changePriority(chore : Chore) {
        val today = Calendar.getInstance()
        val due = Calendar.getInstance()
        due.time = chore.dueDate  // chore.dueDate should be a Date

        val diffMillis = due.timeInMillis - today.timeInMillis
        val daysUntilDue = diffMillis / (1000 * 60 * 60 * 24)

        chore.priority = daysUntilDue in 0..5
    }


    /***********************************************************************************************************************************************************************/

    // pay viewmodel portion
    val paymentList : SnapshotStateList<Payment> = mutableStateListOf()
    val pastPaymentList : SnapshotStateList<Payment> = mutableStateListOf()

    // functions to help us maintain payments

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
//    fun addPayment(newPayment: Payment) {
//        paymentList.add(newPayment)
//    }
    fun addPayment(payTo: String, payFrom: List<String>, amount: Double, memo: String, recurring: Boolean) {
        val amountPerPerson = amount / payFrom.size
        for (name in payFrom) {
            val newPayment = Payment(
                id = paymentList.size + 1,
                payTo = payTo,
                payFrom = name,
                amount = amountPerPerson,
                memo = memo,
                paid = false,
                recurring = recurring
            )
            paymentList.add(newPayment)
        }
    }

    /**
     * function to remove payment, either to get rid of it or upon completion
     */
    fun removePayment(payment: Payment) {
        paymentList.remove(payment)
    }


    /**
     * function to mark a payment as paid
     */
    fun completePayment(payment: Payment) {
        // payment should already be in list so we can access
        payment.paid = true
        pastPaymentList.add(payment)
        paymentList.remove(payment)
    }

    /**
     * function to get payments assigned to specified person
     */
    fun getPaymentsFor(person: String): List<Payment> {
        return paymentList.filter { it.payTo == person }
    }

}