package com.example.a501_final_project.events

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.a501_final_project.CalendarEventInfo
import com.example.a501_final_project.CalendarViewType
import com.example.a501_final_project.LoginViewModel
import com.example.a501_final_project.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

private class EventData() : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@EventData
}

@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    loginViewModel: LoginViewModel,
    mainViewModel: MainViewModel
) {
    val loginState by loginViewModel.uiState.collectAsState()
    val eventsByCalendar by mainViewModel.eventsByCalendar.collectAsState()
    val isLoading by mainViewModel.isLoadingCalendar.collectAsState()
    val error by mainViewModel.calendarError.collectAsState()
    val viewType by mainViewModel.calendarViewType.collectAsState()
    var selectedEvent by remember { mutableStateOf<CalendarEventInfo?>(null) }
    val leftDay by mainViewModel.leftDayForThreeDay.collectAsState()
    val calendarDataDateRangeStart by mainViewModel.calendarDataDateRangeStart.collectAsState()
    val calendarDataDateRangeEnd by mainViewModel.calendarDataDateRangeEnd.collectAsState()
    val canDecrement = leftDay.after(calendarDataDateRangeStart)
    val lastIncrementingDay = (leftDay.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 3) } // left day + 2 is last visible day in 3 day view
    val canIncrement = lastIncrementingDay.before(calendarDataDateRangeEnd)
    val context = LocalContext.current
    var showAddEventDialog by remember { mutableStateOf(false) }


    Column(modifier = modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showAddEventDialog = true }, // Open the dialog
                enabled = loginState.isLoggedIn
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add event",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            CalendarViewSwitcher(
                selectedView = viewType,
                onViewSelected = { mainViewModel.setCalendarView(it) }
            )

            IconButton(
                onClick = {
                    if (loginState.isLoggedIn) {
                        mainViewModel.fetchCalendarEvents(context)
                    }
                },
                enabled = !isLoading && loginState.isLoggedIn
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                !loginState.isLoggedIn -> Text("Please sign in to see events.")
                isLoading && eventsByCalendar.isEmpty() -> CircularProgressIndicator()
                error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                eventsByCalendar.isEmpty() && !isLoading -> Text("No upcoming events found.")
                else -> {
                    val allEvents = eventsByCalendar.values.flatten().sortedBy { it.startDateTime?.value }
                    when (viewType) {
                        CalendarViewType.AGENDA -> AgendaView(mainViewModel)
                        CalendarViewType.THREE_DAY -> ThreeDayView(
                            leftDay = leftDay,
                            events = allEvents,
                            onEventClick = { selectedEvent = it },
                            onIncrementDay = { mainViewModel.incrementThreeDayView() },
                            onDecrementDay = { mainViewModel.decrementThreeDayView() },
                            canIncrement = canIncrement,
                            canDecrement = canDecrement
                        )
                        CalendarViewType.MONTH -> MonthCalendarView(
                            events = allEvents,
                            calendarDataDateRangeStart = calendarDataDateRangeStart,
                            calendarDataDateRangeEnd = calendarDataDateRangeEnd,
                            onDaySelected = { clickedDay ->
                                mainViewModel.onDaySelected(clickedDay)
                                mainViewModel.setCalendarView(CalendarViewType.THREE_DAY)
                            }
                        )
                    }
                }
            }
        }

        // open the calendar event details
        selectedEvent?.let { event ->
            EventDetailDialog(
                event = event,
                onDismiss = { selectedEvent = null }
            )
        }

        if (showAddEventDialog) {
            AddEventDialog(
                onDismiss = { showAddEventDialog = false },
                onConfirm = { summary, description, start, end ->
                    mainViewModel.addCalendarEvent(context, summary, description, start, end)
                    showAddEventDialog = false // Close dialog on confirm
                }
            )
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
            onClick = { onViewSelected(CalendarViewType.MONTH) },
            colors = if (selectedView == CalendarViewType.MONTH) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) { Text("Month") }
    }
}

@Composable
fun EventDetailDialog(event: CalendarEventInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = event.summary ?: "(No title)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                event.startDateTime?.let {
                    Text(
                        text = "Start: ${SimpleDateFormat("EEEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(it.value))}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                event.endDateTime?.let {
                    Text(
                        text = "End: ${SimpleDateFormat("EEEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(it.value))}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                event.summary?.let {
                    Text(
                        text = "Summary: $it", // TODO: make summary different than title
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                // TODO: add more information here such as location
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}