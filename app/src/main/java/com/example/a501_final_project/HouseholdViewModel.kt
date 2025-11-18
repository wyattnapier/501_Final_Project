package com.example.a501_final_project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class ChoreInput(
    var name: String = "",
    var description: String = "",
    var cycle: String = ""
)

class HouseholdViewModel : ViewModel() {

    var householdName by mutableStateOf("")
    var choreInputs = mutableStateListOf(ChoreInput())
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

    fun addChore() {
        choreInputs.add(ChoreInput())
    }

    fun updateChore(index: Int, update: ChoreInput){
        choreInputs[index] = update
    }
}
