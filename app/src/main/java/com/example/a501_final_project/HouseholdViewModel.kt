package com.example.a501_final_project

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

    var householdName by mutableStateOf("")
    var choreInputs = mutableStateListOf(ChoreInput())
        private set

    var paymentInputs = mutableStateListOf(PaymentInput())
        private set


    fun createHousehold(uid: String) {
//        val db = Firebase.firestore

//        val householdRef = db.collection("households").document()
//        householdRef.set(
//            mapOf(
//                "name" to householdName,
//                "ownerId" to uid,
//                "createdAt" to FieldValue.serverTimestamp()
//            )
//        )
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
}
