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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (summary: String, description: String?, start: Calendar, end: Calendar) -> Unit
) {
    var summary by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Hold the start and end times in Calendar objects directly
    var startCal by remember { mutableStateOf(Calendar.getInstance()) }
    var endCal by remember {
        mutableStateOf((Calendar.getInstance().clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 1) })
    }

    // State for controlling the visibility of the pickers
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Start Date Button
                    DateTimePickerButton(
                        text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(startCal.time),
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                    // Start Time Button
                    DateTimePickerButton(
                        text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(startCal.time),
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // End Date Button
                    DateTimePickerButton(
                        text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(endCal.time),
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                    // End Time Button
                    DateTimePickerButton(
                        text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(endCal.time),
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(summary, description.ifEmpty { null }, startCal, endCal)
                },
                // Add a check to ensure the end time is after the start time.
                enabled = summary.isNotBlank() && endCal.after(startCal)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    // Start Date Picker
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            onDateSet = { year, month, day ->
                startCal = (startCal.clone() as Calendar).apply { set(year, month, day) }
                // Also update end date to match if it's before the new start date
                if (endCal.before(startCal)) {
                    endCal = startCal.clone() as Calendar
                }
            }
        )
    }

    // Start Time Picker
    if (showStartTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            onTimeSet = { hour, minute ->
                startCal = (startCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
            }
        )
    }

    // End Date Picker
    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            onDateSet = { year, month, day ->
                endCal = (endCal.clone() as Calendar).apply { set(year, month, day) }
            }
        )
    }

    // End Time Picker
    if (showEndTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            onTimeSet = { hour, minute ->
                endCal = (endCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
            }
        )
    }
}
/**
 * A helper composable for launching Date and Time pickers.
 */
@Composable
fun DateTimePickerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSet: (year: Int, month: Int, dayOfMonth: Int) -> Unit
) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedCal = Calendar.getInstance().apply { timeInMillis = millis }
                        onDateSet(
                            selectedCal.get(Calendar.YEAR),
                            selectedCal.get(Calendar.MONTH),
                            selectedCal.get(Calendar.DAY_OF_MONTH)
                        )
                    }
                    onDismissRequest()
                }
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onTimeSet: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState()
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Time") },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSet(timePickerState.hour, timePickerState.minute)
                    onDismissRequest()
                }
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    )
}