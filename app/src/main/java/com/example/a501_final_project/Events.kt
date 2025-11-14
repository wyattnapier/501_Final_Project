package com.example.a501_final_project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn

@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    loginViewModel: LoginViewModel,
    mainViewModel: MainViewModel
) {
    val loginState by loginViewModel.uiState.collectAsState()
    val eventsByCalendar by mainViewModel.eventsByCalendar.collectAsState()
    val expandedCalendarNames by mainViewModel.expandedCalendarNames.collectAsState()
    val isLoading by mainViewModel.isLoadingCalendar.collectAsState()
    val error by mainViewModel.calendarError.collectAsState()
    val context = LocalContext.current

    // Fetch events when the user is logged in
    LaunchedEffect(loginState.isLoggedIn) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (loginState.isLoggedIn && account != null) {
            mainViewModel.fetchCalendarEvents(account, context)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            !loginState.isLoggedIn -> Text("Please sign in to see events.")
            isLoading -> CircularProgressIndicator()
            error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
            eventsByCalendar.isEmpty() -> Text("No upcoming events found in any calendars.")
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 4.dp)) {
                    // Iterate over the map of calendars and their events
                    eventsByCalendar.forEach { (calendarName, events) ->
                        // Sticky header for the calendar name
                        item {
                            CalendarHeader(
                                calendarName = calendarName,
                                isExpanded = calendarName in expandedCalendarNames,
                                onToggle = { mainViewModel.toggleCalendarSection(calendarName) }
                            )
                        }

                        // Show events only if the section is expanded
                        if (calendarName in expandedCalendarNames) {
                            items(events) { event ->
                                EventItem(event)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarHeader(
    calendarName: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = calendarName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand"
        )
    }
}

@Composable
fun EventItem(
    event: CalendarEventInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp) // Indent event items
    ) {
        Text(
            text = event.summary ?: "(No Title)",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Start: ${event.start}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "End: ${event.end}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}
