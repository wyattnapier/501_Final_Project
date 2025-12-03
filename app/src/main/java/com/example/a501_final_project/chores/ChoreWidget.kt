package com.example.a501_final_project.chores

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.a501_final_project.MainViewModel
import com.example.a501_final_project.events.UpcomingEventItem

@Composable
fun ChoreWidget(
    mainViewModel: MainViewModel,
    choresViewModel: ChoresViewModel,
    onCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
){
    val isLoadingChores by choresViewModel.isLoading.collectAsState()
    val chores by choresViewModel.choresList.collectAsState()
    val userId by mainViewModel.userId.collectAsState()
    val sharedHouseholdID by mainViewModel.householdId.collectAsState()

    val currentUserId = userId
    val currentHouseholdId = sharedHouseholdID

    // Now you can smart cast the local variables -- this should never run
    if (currentUserId == null || currentHouseholdId == null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = { onCardClick() }),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
        ) {
            Text(
                "Loading chores data...",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        return
    }

    val chore = chores.find { it.assignedToId == currentUserId && it.householdID == currentHouseholdId }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp) // minimum height
            .clickable(onClick = { onCardClick() }),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Chores",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            if (isLoadingChores) {
                Box(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Spacer(Modifier.height(4.dp))

                Text(
                    "Your chore: ${chore?.name ?: "No chore assigned"}",
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (chore?.name != null) {
                    Text(
                        chore.description ?: "",
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "Due: ${chore.dueDate}",
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        if (chore.completed) "Chore Completed :)" else "Not Completed!",
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        color = if (isChoreOverdue(chore) && !chore.completed) Color.Red else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}