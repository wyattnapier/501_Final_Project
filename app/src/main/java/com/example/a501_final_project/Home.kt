package com.example.a501_final_project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.a501_final_project.events.UpcomingEventsWidget

/**
 * composable for the home screen
 * creates the 4 boxes as previews of the main screens
 * TODO: fill in the the screens with real content (build composables for each widget)
 */
@Composable
fun HomeScreen(
    navController: NavController,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }

    val showPayments by prefs.showPayments.collectAsState(initial = true)
    val showChores by prefs.showChores.collectAsState(initial = true)
    val showEvents by prefs.showEvents.collectAsState(initial = true)

    val eventsByCalendar by mainViewModel.eventsByCalendar.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        if (showEvents) {
            Row(
                modifier = Modifier
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                UpcomingEventsWidget(
                    events = eventsByCalendar.values.flatten().sortedBy { it.startDateTime?.value },
                    onCardClick = { eventsWidgetCardOnClick(navController, mainViewModel) },
                    onEventClick = { eventsWidgetEventOnClick(navController, mainViewModel) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (showPayments) {
            Row(
                modifier = Modifier
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BoxItem(
                    "Payment",
                    MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { navigateToScreen(navController, Screen.Pay) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (showChores) {
            Row(
                modifier = Modifier
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BoxItem(
                    "Chores",
                    MaterialTheme.colorScheme.tertiaryContainer,
                    onClick = { navigateToScreen(navController, Screen.Chores) }
                )
            }
        }
    }
}

@Composable
fun RowScope.BoxItem(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(weight = 1f)
            .fillMaxSize()
            .padding(4.dp)
            .background(
                color = color,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

fun eventsWidgetCardOnClick(
    navController: NavController,
    mainViewModel: MainViewModel
) {
    mainViewModel.setCalendarView(CalendarViewType.AGENDA)
    navigateToScreen(navController, Screen.Calendar)
}

fun eventsWidgetEventOnClick(
    navController: NavController,
    mainViewModel: MainViewModel
) {
    mainViewModel.setCalendarView(CalendarViewType.THREE_DAY)
    navigateToScreen(navController, Screen.Calendar)
}