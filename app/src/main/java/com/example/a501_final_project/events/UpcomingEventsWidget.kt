package com.example.a501_final_project.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun UpcomingEventsWidget(
    events: List<CalendarEventInfo>,
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = {},
    onEventClick: (CalendarEventInfo) -> Unit = {},
    eventsViewModel: EventsViewModel
) {
    val isCalendarIdLoaded = eventsViewModel.isCalendarIdLoaded.collectAsState()
    val isLoadingCalendar = eventsViewModel.isLoadingCalendar.collectAsState()

    val now = remember { Calendar.getInstance().timeInMillis }

    // Filter events after "now" and sort ascending
    val nextThreeEvents = events
        .filter { (it.startDateTime?.value ?: 0) > now }
        .sortedBy { it.startDateTime?.value }
        .take(3)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp) // minimum height
            .clickable(onClick = { onCardClick() }),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Upcoming Events",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (isLoadingCalendar.value || !isCalendarIdLoaded.value) {
                Box(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Spacer(Modifier.height(4.dp))
                if (nextThreeEvents.isEmpty()) {
                    Text(
                        "No upcoming events",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    nextThreeEvents.forEach { event ->
                        UpcomingEventItem(event, onClick = { onEventClick(event) })
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingEventItem(
    event: CalendarEventInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    val startDate = event.startDateTime?.let { Date(it.value) }
    val dateStr = startDate?.let { dateFormat.format(it) } ?: "No date"
    val timeStr = startDate?.let { timeFormat.format(it) } ?: ""

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.summary ?: "(No title)",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$dateStr • $timeStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
