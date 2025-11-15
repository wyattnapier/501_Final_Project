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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import java.util.Locale
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.text.format

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
    val viewType by mainViewModel.calendarViewType.collectAsState()

    // Fetch events when the user is logged in
    LaunchedEffect(loginState.isLoggedIn) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (loginState.isLoggedIn && account != null) {
            mainViewModel.fetchCalendarEvents(account, context, days = 14)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // NEW: View Switcher
        CalendarViewSwitcher(
            selectedView = viewType,
            onViewSelected = { mainViewModel.setCalendarView(it) }
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                !loginState.isLoggedIn -> Text("Please sign in to see events.")
                isLoading && eventsByCalendar.isEmpty() -> CircularProgressIndicator()
                error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                eventsByCalendar.isEmpty() -> Text("No upcoming events found.")
                else -> {
                    // NEW: Switch between different calendar layouts
                    val allEvents = eventsByCalendar.values.flatten().sortedBy { it.startDateTime?.value }
                    when (viewType) {
                        CalendarViewType.AGENDA -> AgendaView(mainViewModel) // Your original collapsible list
                        CalendarViewType.THREE_DAY -> ThreeDayView(events = allEvents)
                        CalendarViewType.FOURTEEN_DAY -> FourteenDayAgendaView(events = allEvents)
                    }
                }
            }
        }
    }
}

// The buttons to switch views
@Composable
fun CalendarViewSwitcher(
    selectedView: CalendarViewType,
    onViewSelected: (CalendarViewType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { onViewSelected(CalendarViewType.AGENDA) },
            colors = if (selectedView == CalendarViewType.AGENDA) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) { Text("Agenda") }

        Button(
            onClick = { onViewSelected(CalendarViewType.THREE_DAY) },
            colors = if (selectedView == CalendarViewType.THREE_DAY) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) { Text("3 Day") }

        Button(
            onClick = { onViewSelected(CalendarViewType.FOURTEEN_DAY) },
            colors = if (selectedView == CalendarViewType.FOURTEEN_DAY) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) { Text("14 Day") }
    }
}

// This is your original collapsible list view
@Composable
fun AgendaView(mainViewModel: MainViewModel) {
    val eventsByCalendar by mainViewModel.eventsByCalendar.collectAsState()
    val expandedCalendarNames by mainViewModel.expandedCalendarNames.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        eventsByCalendar.forEach { (calendarName, events) ->
            item {
                CalendarHeader(
                    calendarName = calendarName,
                    isExpanded = calendarName in expandedCalendarNames,
                    onToggle = { mainViewModel.toggleCalendarSection(calendarName) }
                )
            }
            if (calendarName in expandedCalendarNames) {
                items(events) { event ->
                    // Make sure to format the DateTime object for display
                    val startStr = event.startDateTime?.let { SimpleDateFormat("MMM d, h:mm a",
                        Locale.getDefault()).format(java.util.Date(it.value)) } ?: "N/A"
                    val endStr = event.endDateTime?.let { SimpleDateFormat("MMM d, h:mm a",
                        Locale.getDefault()).format(java.util.Date(it.value)) } ?: "N/A"
                    EventItem(event.summary ?: "(No Title)", startStr, endStr)
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
fun EventItem(summary: String, start: String, end: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(text = summary, style = MaterialTheme.typography.titleMedium)
        Text(text = "Start: $start", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = "End: $end", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun FourteenDayAgendaView(events: List<CalendarEventInfo>) {
    val groupedEvents = events.groupBy {
        // Group by day
        val cal = Calendar.getInstance()
        cal.timeInMillis = it.startDateTime?.value ?: 0
        cal.get(Calendar.DAY_OF_YEAR)
    }

    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 8.dp)) {
        groupedEvents.forEach { (_, dayEvents) ->
            val day = dayEvents.first().startDateTime?.value
            if (day != null) {
                item {
                    Text(
                        text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(java.util.Date(day)),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)
                    )
                }
                items(dayEvents) { event ->
                    val startStr = event.startDateTime?.let { SimpleDateFormat("h:mm a", Locale.getDefault()).format(
                        java.util.Date(it.value)) } ?: ""
                    val endStr = event.endDateTime?.let { SimpleDateFormat("h:mm a", Locale.getDefault()).format(
                        java.util.Date(it.value)) } ?: ""
                    Text(
                        text = "${event.summary ?: "(No title)"} ($startStr - $endStr)",
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                }
            }
        }
    }
}

// Placeholder for the 3-Day View (the most complex part)
@Composable
fun ThreeDayView(events: List<CalendarEventInfo>) {
    // This is a very complex component. A full implementation is beyond a simple
    // response, but here is a simplified placeholder structure.
    Text(
        "3-Day View (Visual Layout) is a complex component and would require a custom Layout composable to draw events on a timeline. This is a placeholder.",
        modifier = Modifier.padding(16.dp)
    )
    // A real implementation would involve:
    // 1. A custom Layout() composable.
    // 2. Drawing horizontal lines for each hour.
    // 3. Calculating the y-position and height of each event based on its start/end time.
    // 4. Placing each event as a measured item in the layout.
    // 5. Handling overlapping events.
}
