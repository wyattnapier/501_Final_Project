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
import com.example.a501_final_project.chores.ChoresViewModel
import com.example.a501_final_project.events.CalendarViewType
import com.example.a501_final_project.events.EventsViewModel
import com.example.a501_final_project.events.UpcomingEventsWidget
import com.example.a501_final_project.login_register.UserPreferences
import com.example.a501_final_project.payment.PaymentViewModel
import com.example.a501_final_project.chores.ChoreWidget
import com.example.a501_final_project.payment.UpcomingPaymentsWidget

/**
 * composable for the home screen
 * creates the 4 boxes as previews of the main screens
 * TODO: fill in the the screens with real content (build composables for each widget)
 */
@Composable
fun HomeScreen(
    navController: NavController,
    mainViewModel: MainViewModel,
    eventsViewModel: EventsViewModel,
    paymentViewModel: PaymentViewModel,
    choresViewModel: ChoresViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }

    val showPayments by prefs.showPayments.collectAsState(initial = true)
    val showChores by prefs.showChores.collectAsState(initial = true)
    val showEvents by prefs.showEvents.collectAsState(initial = true)

    val currentUserId by mainViewModel.userId.collectAsState()
    val currentPaymentsForUser = (
            paymentViewModel.getPaymentsFor(currentUserId ?: "") +
                    paymentViewModel.getPaymentsFrom(currentUserId ?: "")
            ).filter { !it.paid }
    val events by eventsViewModel.events.collectAsState()

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
                    events = events.sortedBy { it.startDateTime?.value },
                    onCardClick = { eventsWidgetCardOnClick(navController, eventsViewModel) },
                    onEventClick = { eventsWidgetEventOnClick(navController, eventsViewModel) },
                    eventsViewModel = eventsViewModel,
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
                UpcomingPaymentsWidget(
                    paymentViewModel = paymentViewModel,
                    onCardClick = { navigateToScreen(navController, Screen.Pay) },
                    currentPaymentsForUser = currentPaymentsForUser,
                    currentUserId = currentUserId,
                    modifier = Modifier.fillMaxSize()
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
                ChoreWidget(
                    mainViewModel = mainViewModel,
                    choresViewModel = choresViewModel,
                    onCardClick = { navigateToScreen(navController, Screen.Chores)},
                    Modifier.fillMaxSize()
                )
            }
        }

        // default message if no widgets are enabled
        if (!showPayments && !showChores && !showEvents) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "Welcome home :)\nYou've disabled all home screen widgets. Go to settings if you'd like to enable them!",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
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
    eventsViewModel: EventsViewModel
) {
    eventsViewModel.setCalendarView(CalendarViewType.AGENDA)
    navigateToScreen(navController, Screen.Calendar)
}

fun eventsWidgetEventOnClick(
    navController: NavController,
    eventsViewModel: EventsViewModel
) {
    eventsViewModel.setCalendarView(CalendarViewType.THREE_DAY)
    navigateToScreen(navController, Screen.Calendar)
}