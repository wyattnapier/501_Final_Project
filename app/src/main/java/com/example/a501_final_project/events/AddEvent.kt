package com.example.a501_final_project.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.text.ifEmpty

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
                        val selectedCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = millis
                        }
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