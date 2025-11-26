package com.example.a501_final_project.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AgendaView(
    events: List<CalendarEventInfo>,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Text(
            text = "Household Calendar",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(events) { event ->
                val start = event.startDateTime?.let {
                    SimpleDateFormat(
                        "MMM d, h:mm a",
                        Locale.getDefault()
                    ).format(Date(it.value))
                } ?: "N/A"
                val end = event.endDateTime?.let {
                    SimpleDateFormat(
                        "MMM d, h:mm a",
                        Locale.getDefault()
                    ).format(Date(it.value))
                } ?: "N/A"
                EventItem(event.summary ?: "(No Title)", start, end)
            }
        }
    }
}

@Composable
fun EventItem(summary: String, start: String, end: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(summary, style = MaterialTheme.typography.titleMedium)
        Text("Start: $start", style = MaterialTheme.typography.bodySmall)
        Text("End: $end", style = MaterialTheme.typography.bodySmall)
        Divider(modifier = Modifier.padding(top = 8.dp))
    }
}
