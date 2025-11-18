package com.example.a501_final_project

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

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
//        val db = Firebase.firestore

//        val householdRef = db.collection("households").document()
//        householdRef.set(
//            mapOf(
//                "name" to householdName,
//                "ownerId" to uid,
//                "createdAt" to FieldValue.serverTimestamp()
//            )
//        )
        Log.d("HouseholdViewModel", "Household created with name: $householdName")
    }
}
