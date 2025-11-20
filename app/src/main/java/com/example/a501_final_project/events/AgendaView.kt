package com.example.a501_final_project.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.a501_final_project.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AgendaView(mainViewModel: MainViewModel) {
    val eventsByCalendar by mainViewModel.eventsByCalendar.collectAsState()
    val expandedCalendarNames by mainViewModel.expandedCalendarNames.collectAsState()

    LaunchedEffect(eventsByCalendar) {
        if (eventsByCalendar.size == 1) {
            mainViewModel.toggleCalendarSection(eventsByCalendar.keys.first())
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (eventsByCalendar.size == 1) {
            val calName = eventsByCalendar.keys.first()
            val events = eventsByCalendar[calName] ?: emptyList()

            item {
                CalendarHeader(
                    calendarName = calName,
                    isExpanded = true,
                    onToggle = {},
                    showToggle = false
                )
            }

            items(events) { event ->
                val start = event.startDateTime?.let { SimpleDateFormat("MMM d, h:mm a").format(Date(it.value)) } ?: "N/A"
                val end = event.endDateTime?.let { SimpleDateFormat("MMM d, h:mm a").format(Date(it.value)) } ?: "N/A"
                EventItem(event.summary ?: "(No Title)", start, end)
            }

        } else {
            eventsByCalendar.forEach { (calendarName, events) ->
                item {
                    CalendarHeader(
                        calendarName = calendarName,
                        isExpanded = calendarName in expandedCalendarNames,
                        onToggle = { mainViewModel.toggleCalendarSection(calendarName) },
                        showToggle = true
                    )
                }
                if (calendarName in expandedCalendarNames) {
                    items(events) { event ->
                        val start = event.startDateTime?.let { SimpleDateFormat("MMM d, h:mm a").format(Date(it.value)) } ?: "N/A"
                        val end = event.endDateTime?.let { SimpleDateFormat("MMM d, h:mm a").format(Date(it.value)) } ?: "N/A"
                        EventItem(event.summary ?: "(No Title)", start, end)
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
    showToggle: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(calendarName, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (showToggle) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }
    }
}

@Composable
fun EventItem(summary: String, start: String, end: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(summary, style = MaterialTheme.typography.titleMedium)
        Text("Start: $start", style = MaterialTheme.typography.bodySmall)
        Text("End: $end", style = MaterialTheme.typography.bodySmall)
        Divider(modifier = Modifier.padding(top = 8.dp))
    }
}
