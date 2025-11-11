package com.example.a501_final_project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.example.a501_final_project.BoxItem
import com.example.a501_final_project.ui.theme._501_Final_ProjectTheme
import com.google.android.libraries.places.api.model.LocalDate
import java.util.*

data class ChoreTemp(
    val name: String,
    val description: String,
    val assignedTo: String,
    val houseHoldID: Number,
    val userID: Number,
    val dueDate: String,
    val status: String = "Pending"
)

//TODO: remove this once viewModel is actually implemented
//class TempViewModel : ViewModel() {
//    val choreList = mutableListOf(
//        ChoreTemp(
//            name = "Wash Dishes",
//            description = "Clean all dishes, utensils, and pots used during dinner.",
//            assignedTo = "Alice",
//            houseHoldID = 1,
//            userID = 1,
//            dueDate = "November 15, 2025",
//            status = "Done"
//        ),
//        ChoreTemp(
//            name = "Vacuum Living Room",
//            description = "Vacuum the carpet and under the furniture in the living room.",
//            assignedTo = "Bob",
//            houseHoldID = 1,
//            userID = 2,
//            dueDate = "November 15, 2025",
//            status = "Done"
//        ),
//        ChoreTemp(
//            name = "Laundry",
//            description = "Wash, dry, and fold all the clothes from the laundry basket.",
//            assignedTo = "Charlie",
//            houseHoldID = 1,
//            userID = 3,
//            dueDate = "November 15, 2025",
//            status = "Done"
//        ),
//        ChoreTemp(
//            name = "Take Out Trash",
//            description = "Empty all trash bins and take the garbage out to the curb.",
//            assignedTo = "Dana",
//            houseHoldID = 1,
//            userID = 4,
//            dueDate = "November 15, 2025"
//        ),
//        ChoreTemp(
//            name = "Clean Bathroom",
//            description = "Scrub the sink, toilet, and shower, and mop the bathroom floor.",
//            assignedTo = "Eve",
//            houseHoldID = 1,
//            userID = 5,
//            dueDate = "November 15, 2025"
//        )
//    )
//    val userID = 2
//    val houseHoldID = 1
//    var showPrevChores by mutableStateOf(false)
//
//    fun markChoreComplete(chore: ChoreTemp) {
//        val index = choreList.indexOf(chore)
//        if (index != -1) {
//            val updatedChore = chore.copy(status = "Done")
//            choreList[index] = updatedChore
//        }
//    }
//}


@Composable
fun ChoresScreen(viewModel: MainViewModel, modifier: Modifier = Modifier){
    val chores by viewModel.choresList.collectAsState()
    val showPrevChores by viewModel.showPrevChores.collectAsState()
    Column(modifier = modifier.fillMaxHeight().padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)){
        if (showPrevChores) {
            PrevChores(chores, viewModel)
        }else {
            MyChoreWidget(chores, viewModel)
            RoommateChores(chores, viewModel)
        }
    }
}

@Composable
fun MyChoreWidget(chores: List<Chore>, viewModel: MainViewModel, modifier: Modifier = Modifier){
    val chore = chores.find { it.userID == viewModel.userID && it.householdID == viewModel.householdID }
    Row(modifier =  modifier
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.secondaryContainer)
        .padding(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically){
        Column(modifier = Modifier.weight(2f, true)) {
            Text("My Chore", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
            Text(chore?.name ?: "No chore assigned", fontSize = MaterialTheme.typography.bodyLarge.fontSize)
            Text(
                "Due Date: ${chore?.dueDate}",
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                color = if (chore!!.completed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Column(modifier = Modifier.weight(1f, true)) {
            Button(onClick = { viewModel.completeChore(chore!!) }) {
                Text("Mark as Complete")
            }
        }
    }
}


@Composable
fun RoommateChores(chores: List<Chore>, viewModel: MainViewModel, modifier: Modifier = Modifier){
    val roommateChores = chores.filter { it.userID != viewModel.userID && it.householdID == viewModel.householdID }

    Column(modifier =  modifier
        .fillMaxHeight()
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.secondaryContainer)
        .padding(10.dp)){
        Text("Roommate Chores", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
        LazyColumn(){
            for(chore in roommateChores) {
                item {
                    Text(chore.assignedTo + ": " + chore.name, fontSize = MaterialTheme.typography.bodyLarge.fontSize)
                    Text(text = if (chore.completed) {"Status: Completed"} else {"Status: Pending"}, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                    HorizontalDivider(
                        color = Color.LightGray,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            item {
                Row( modifier = Modifier.fillParentMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(onClick = { viewModel.toggleShowPrevChores() }) {
                        Text("See Previous Chores")
                    }
                }
            }
        }
    }
}

@Composable
fun PrevChores(chores: List<Chore>, viewModel: MainViewModel) {
    // if due date < today, display on prev tasks
    Column(modifier =  Modifier
        .fillMaxHeight()
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.secondaryContainer)
        .padding(10.dp)){
        Text("Previous Chores", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
        LazyColumn(){
            for(chore in chores) {
                item {
                    Text(chore.assignedTo + ": " + chore.name, fontSize = MaterialTheme.typography.bodyLarge.fontSize)
                    Text(text = if (chore.completed) {"Status: Completed"} else {"Status: Pending"}, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                    HorizontalDivider(
                        color = Color.LightGray,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            item {
                Row( modifier = Modifier.fillParentMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(onClick = { viewModel.toggleShowPrevChores() }) {
                        Text("Back to Current Chores")
                    }
                }

            }
        }
    }
}


//@Preview(showBackground = true)
//@Composable
//fun ChoresPreview() {
//    val previewVM = MainViewModel()
//    _501_Final_ProjectTheme {
//        ChoresScreen(previewVM)
//    }
//}
