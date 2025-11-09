package com.example.a501_final_project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.example.a501_final_project.BoxItem
import java.util.*

data class Chore(
    val name: String,
    val description: String,
    val assignedTo: String,
    val houseHoldID: Number,
    val userID: Number,
    val dueDate: Date
)

//TODO: remove this once viewModel is actually implemented
class TempViewModel : ViewModel() {
    val choreList = listOf(
        Chore(
            name = "Wash Dishes",
            description = "Clean all dishes, utensils, and pots used during dinner.",
            assignedTo = "Alice",
            houseHoldID = 1,
            userID = 1,
            dueDate = Calendar.getInstance().apply { add(Calendar.DATE, 1) }.time
        ),
        Chore(
            name = "Vacuum Living Room",
            description = "Vacuum the carpet and under the furniture in the living room.",
            assignedTo = "Bob",
            houseHoldID = 1,
            userID = 1,
            dueDate = Calendar.getInstance().apply { add(Calendar.DATE, 2) }.time
        ),
        Chore(
            name = "Laundry",
            description = "Wash, dry, and fold all the clothes from the laundry basket.",
            assignedTo = "Charlie",
            houseHoldID = 1,
            userID = 1,
            dueDate = Calendar.getInstance().apply { add(Calendar.DATE, 3) }.time
        ),
        Chore(
            name = "Take Out Trash",
            description = "Empty all trash bins and take the garbage out to the curb.",
            assignedTo = "Dana",
            houseHoldID = 1,
            userID = 1,
            dueDate = Calendar.getInstance().apply { add(Calendar.DATE, 1) }.time
        ),
        Chore(
            name = "Clean Bathroom",
            description = "Scrub the sink, toilet, and shower, and mop the bathroom floor.",
            assignedTo = "Eve",
            houseHoldID = 1,
            userID = 1,
            dueDate = Calendar.getInstance().apply { add(Calendar.DATE, 4) }.time
        )
    )
    val userID = 2
    val houseHoldID = 1
}


@Composable
fun ChoresScreen(viewModel: TempViewModel){
    Column(){
        MyChoreWidget(viewModel)
        RoommateChores(viewModel)
    }
}

@Composable
fun MyChoreWidget(viewModel: TempViewModel){
    val chore = viewModel.choreList.find { it.userID == viewModel.userID && it.houseHoldID == viewModel.houseHoldID }
    Row(){
        Column(modifier = Modifier.weight(3f, true)) {
            Text("My Chore: ${chore?.name}", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
            Text(chore?.description ?: "", fontSize = MaterialTheme.typography.bodyMedium.fontSize)

        }
        Column(modifier = Modifier.weight(1f, true)) {
            Button(onClick = { /*TODO*/ }) {
                Text("Complete")
            }
        }
    }
}


@Composable
fun RoommateChores(viewModel: TempViewModel){

}