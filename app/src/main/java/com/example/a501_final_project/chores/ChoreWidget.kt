package com.example.a501_final_project.chores

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.a501_final_project.MainViewModel
import com.example.a501_final_project.events.UpcomingEventItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onTertiaryContainer)
        ) {
            Text(
                "Loading chores data...",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        return
    }

    val today = System.currentTimeMillis()
    val myChores = choresViewModel.getUpcomingChores(chores).filter {
        it.assignedToId == currentUserId && it.householdID == currentHouseholdId
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp) // minimum height
            .clickable(onClick = { onCardClick() }),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                if (myChores.isEmpty()){
                    Box(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No chore assigned!",
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                else {
                    myChores.forEachIndexed { index, chore ->
                        ChoreItem(chore, choresViewModel)

                        if (index < myChores.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(0.8f),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChoreItem(chore: Chore, choreViewModel: ChoresViewModel, modifier: Modifier = Modifier){
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    val dueDate = Date(chore.dueDate)
    val dateStr = dateFormat.format(dueDate)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .clip(RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (chore.completed) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Chore completed",
                    tint = Color(0xFF2E7D32) // A dark, success green color
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = "Chore not completed",
                    tint = MaterialTheme.colorScheme.error // Use the theme's error color for the 'X'
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chore.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Due: $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (choreViewModel.isChoreOverdue(chore) && !chore.completed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}