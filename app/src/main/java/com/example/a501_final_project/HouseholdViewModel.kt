package com.example.a501_final_project

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
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

data class PaymentDB(
    var name: String = "",
    var amount: Number = 0,
    var cycle: Number = 0,
    var payee: String = "",
    var occupiedSplit: Number = 0,
    var split: Number = 0,
    var youPay: Boolean = false
)

data class ResidentDB(
    var id: String = "",
    var payment_percents: List<Number> = listOf()
)

class HouseholdViewModel : ViewModel() {
    val db = FirebaseFirestore.getInstance()

    var existingHousehold by mutableStateOf<Boolean?>(null)

    //TODO: properly fetch the uid of the active user
    var uid = "wyatt"
    var setupStep by mutableStateOf(0)
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
                householdID = doc.id
                householdCreated = true
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.e("HouseholdViewModel", "Error creating household", e)
                errorMessage = "Failed to create household: ${e.message}"
                isLoading = false
            }
    }

    fun getHousehold(householdID: String){
        val doc = db.collection("households").document(householdID)
        doc.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    householdName = document.getString("name") ?: "Household Not Found"
                    Log.d("HouseholdViewModel", "DocumentSnapshot data: ${document.data}")
                    val paymentsList = document.get("recurring_payments") as? List<Map<String, Any>>

                    paymentsList?.let { list ->
                        paymentsFromDB.clear()
                        paymentsFromDB.addAll(
                            list.map { map ->
                                PaymentDB(
                                    name = map["name"] as? String ?: "",
                                    amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                                    cycle = (map["cycle"] as? Number)?.toDouble() ?: 0.0,
                                    payee = (map["payee"] as? String) ?: ""
                                )
                            }
                        )
                    }

                    val residentsList = document.get("residents") as? List<Map<String, Any>>

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

                    for (paymentIndex in paymentsFromDB.indices) {

                        // Sum all residents' percentages for this payment index
                        val totalTaken = residentsFromDB.sumOf { resident ->
                            resident.payment_percents.getOrNull(paymentIndex)?.toDouble() ?: 0.0
                        }

                        paymentsFromDB[paymentIndex] = paymentsFromDB[paymentIndex].copy(
                            occupiedSplit = totalTaken
                        )
                    }

                    updateID(householdID)
                    gotHousehold = true
                }
            }
    }

    fun addToHousehold(){
        val userPaymentPercents: List<Number> = paymentsFromDB.map { payment -> payment.split }
        val newResident = ResidentDB(
            id = uid,
            payment_percents = userPaymentPercents
        )

        val residentMap = mapOf(
            "id" to newResident.id,
            "payment_percents" to newResident.payment_percents
        )

        val paymentsMap = paymentsFromDB.mapIndexed { index, payment ->
            val payeeValue = when {
                payment.youPay -> uid
                payment.payee.isNullOrEmpty() -> null
                else -> payment.payee
            }
            mapOf(
                "name" to payment.name,
                "amount" to payment.amount,
                "cycle" to payment.cycle,
                "payee" to payeeValue
            )
        }

        Log.d("HouseholdViewModel", "Resident to add: $residentMap")
        Log.d("HouseholdViewModel", "Household ID: $householdID")


        val householdRef = db.collection("households").document(householdID)

        val updateMap = mapOf(
            "recurring_payments" to paymentsMap,
            "residents" to FieldValue.arrayUnion(residentMap)
        )

        householdRef.update(updateMap)
            .addOnSuccessListener {
                Log.d("HouseholdViewModel", "Successfully updated household.")
                // Do something like navigate or show Toast/Snackbar
                householdCreated = true
            }
            .addOnFailureListener { e ->
                Log.e("HouseholdViewModel", "Failed to update household", e)
                // Show error to user
                errorMessage = "Failed to create household: ${e.message}"
                isLoading = false
            }
    }
}
