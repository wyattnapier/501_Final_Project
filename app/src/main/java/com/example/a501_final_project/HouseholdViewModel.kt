package com.example.a501_final_project

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

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

class HouseholdViewModel : ViewModel() {
    //TODO: properly fetch the uid of the active user
    var uid = "alice"
    var setupStep by mutableStateOf(0)
    var householdName by mutableStateOf("")
    var choreInputs = mutableStateListOf(ChoreInput())
        private set

    var paymentInputs = mutableStateListOf(PaymentInput())
        private set

    var calendarName by mutableStateOf("")
        private set

    fun incrementStep(){
        setupStep++
    }

    fun decrementStep(){
        setupStep--
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

    fun createHousehold() {
        val db = FirebaseFirestore.getInstance()

        val recurring_chores = choreInputs.mapIndexed { index, chore ->
            mapOf(
                "name" to chore.name,
                "description" to chore.description,
                "cycle" to chore.cycle
            )
        }

        val recurring_payments = paymentInputs.mapIndexed { index, payment ->
            mapOf(
                "name" to payment.name,
                "amount" to payment.amount,
                "cycle" to payment.cycle,
                if(payment.youPay) "payee" to uid else "payee" to null
            )
        }

        val payment_split = paymentInputs.map{it.split}
2
        val residents = listOf(
            mapOf(
                "id" to uid,
                "payment_percents" to payment_split
            )
        )

        val fullHouseholdObject = mapOf(
            "name" to householdName,
            "recurring_chores" to recurring_chores,
            "recurring_payments" to recurring_payments,
            "calendar" to calendarName,
            "residents" to residents
        )


        Log.d("HouseholdViewModel", "Household created with name: $householdName")

        db.collection("households")
            .add(fullHouseholdObject)
            .addOnSuccessListener { doc ->
                Log.d("HouseholdViewModel", "Created household ${doc.id}")
            }
            .addOnFailureListener { e ->
                Log.e("HouseholdViewModel", "Error creating household", e)
            }
    }
}
