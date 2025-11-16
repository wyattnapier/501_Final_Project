package com.example.a501_final_project

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.api.client.util.DateTime
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Data class to hold layout information for an event
private class EventData(
    val position: Int,
    val total: Int
) : ParentDataModifier {
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
    val context = LocalContext.current
    val viewType by mainViewModel.calendarViewType.collectAsState()
    var selectedEvent by remember { mutableStateOf<CalendarEventInfo?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        CalendarViewSwitcher(
            selectedView = viewType,
            onViewSelected = { mainViewModel.setCalendarView(it) }
        )

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
                            events = allEvents,
                            onEventClick = { selectedEvent = it }
                        )
                        CalendarViewType.FOURTEEN_DAY -> FourteenDayAgendaView(events = allEvents)
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

    LaunchedEffect(eventsByCalendar) {
        // If there's exactly one calendar, expand it by default.
        if (eventsByCalendar.size == 1) {
            val singleCalendarName = eventsByCalendar.keys.first()
            mainViewModel.toggleCalendarSection(singleCalendarName)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (eventsByCalendar.size == 1) {
            // always render list open
            val singleCalendarName = eventsByCalendar.keys.first()
            item {
                CalendarHeader(
                    calendarName = singleCalendarName,
                    isExpanded = true,
                    onToggle = {},
                    showToggle = false
                )
            }
            val events = eventsByCalendar[singleCalendarName] ?: emptyList()
            items(events) { event ->
                val startStr = event.startDateTime?.let {
                    SimpleDateFormat(
                        "MMM d, h:mm a",
                        Locale.getDefault()
                    ).format(Date(it.value))
                } ?: "N/A"
                val endStr = event.endDateTime?.let {
                    SimpleDateFormat(
                        "MMM d, h:mm a",
                        Locale.getDefault()
                    ).format(Date(it.value))
                } ?: "N/A"
                EventItem(event.summary ?: "(No Title)", startStr, endStr)
            }
        } else {
            eventsByCalendar.forEach { (calendarName, events) ->
                item {
                    CalendarHeader(
                        calendarName = calendarName,
                        isExpanded = calendarName in expandedCalendarNames,
                        onToggle = { mainViewModel.toggleCalendarSection(calendarName) },
                        showToggle = true,
                    )
                }
                if (calendarName in expandedCalendarNames) {
                    items(events) { event ->
                        val startStr = event.startDateTime?.let {
                            SimpleDateFormat(
                                "MMM d, h:mm a",
                                Locale.getDefault()
                            ).format(Date(it.value))
                        } ?: "N/A"
                        val endStr = event.endDateTime?.let {
                            SimpleDateFormat(
                                "MMM d, h:mm a",
                                Locale.getDefault()
                            ).format(Date(it.value))
                        } ?: "N/A"
                        EventItem(event.summary ?: "(No Title)", startStr, endStr)
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
    modifier: Modifier = Modifier,
    showToggle: Boolean = true,
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
        if (showToggle) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }
    }
}

// Simplified EventItem for the Agenda view
@Composable
fun EventItem(summary: String, start: String, end: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(text = summary, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Start: $start",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "End: $end",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

// NEW: View for 14-Day grouped list
@Composable
fun FourteenDayAgendaView(events: List<CalendarEventInfo>) {
    val groupedEvents = events.groupBy {
        val cal = Calendar.getInstance()
        cal.timeInMillis = it.startDateTime?.value ?: 0
        cal[Calendar.DAY_OF_YEAR]
    }

    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 8.dp)) {
        groupedEvents.forEach { (_, dayEvents) ->
            val day = dayEvents.first().startDateTime?.value
            if (day != null) {
                item {
                    Text(
                        text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(day)),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)
                    )
                }
                items(dayEvents) { event ->
                    val startStr = event.startDateTime?.let {
                        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it.value))
                    } ?: ""
                    val endStr = event.endDateTime?.let {
                        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it.value))
                    } ?: ""
                    Text(
                        text = "${event.summary ?: "(No title)"} ($startStr - $endStr)",
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                }
            }
        }
    }
}


private val hourHeight = 60.dp
private val sidebarWidth = 60.dp
private val am_pm_time_formatter = SimpleDateFormat("h a", Locale.getDefault()) // TODO: make this dynamic

@Composable
fun ThreeDayView(
    events: List<CalendarEventInfo>,
    onEventClick: (CalendarEventInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val numDays = 3
    val today = Calendar.getInstance()

    val days = (0 until numDays).map { dayIndex ->
        (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, dayIndex) }
    }

    val allDayEventsByDay = remember(events, days) {
        days.associateWith { day ->
            events.filter { event ->
                // Use the new, robust isSameDayAs check
                event.isAllDay && event.startDateTime != null &&
                        event.startDateTime.toCalendar().isSameDayAs(day)
            }
        }
    }

    val timedEventsByDay = remember(events, days) {
        days.associateWith { day ->
            events.filter { event ->
                !event.isAllDay && event.startDateTime != null &&
                        event.startDateTime.toCalendar().isSameDayAs(day)
            }
        }
    }

    val scrollState = rememberScrollState(1090)

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        DayHeaders(days)
        AllDayEventsHeader( // not included in scrollable section
            days = days,
            allDayEventsByDay = allDayEventsByDay,
            onEventClick = onEventClick
        )
        HorizontalDivider()
        Row(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)) {
            HourSidebar(modifier = Modifier
                .width(sidebarWidth)
                .height(hourHeight * 24))
            BasicCalendar(
                modifier = Modifier.weight(1f),
                days = days,
                // Pass the timed events to the grid
                eventsByDay = timedEventsByDay,
                onEventClick = onEventClick
            )
        }
    }
}

@Composable
fun DayHeaders(days: List<Calendar>) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(start = sidebarWidth)) { // Pad for the hour sidebar
        days.forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = SimpleDateFormat("EEE", Locale.getDefault()).format(day.time), style = MaterialTheme.typography.labelSmall)
                Text(text = SimpleDateFormat("d", Locale.getDefault()).format(day.time), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun HourSidebar(modifier: Modifier = Modifier) {
    val hourColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    Column(modifier) {
        (0..23).forEach { hour ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(hourHeight)
            ) {
                Text(
                    text = am_pm_time_formatter.format(Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour) }.time),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(color = hourColor)
                )
            }
        }
    }
}

@Composable
fun AllDayEventsHeader(
    days: List<Calendar>,
    allDayEventsByDay: Map<Calendar, List<CalendarEventInfo>>,
    onEventClick: (CalendarEventInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = sidebarWidth) // Align with the calendar grid
    ) {
        days.forEach { day ->
            val eventsForDay = allDayEventsByDay[day] ?: emptyList()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp, vertical = 4.dp)
            ) {
                // Limit to showing 2-3 all-day events to prevent the header from getting too tall
                eventsForDay.take(2).forEach { event ->
                    AllDayEventItem(event = event, onClick = { onEventClick(event) })
                }
            }
        }
    }
}

@Composable
fun AllDayEventItem(
    event: CalendarEventInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = event.summary ?: "(No Title)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BasicCalendar(
    modifier: Modifier = Modifier,
    days: List<Calendar>,
    eventsByDay: Map<Calendar, List<CalendarEventInfo>>,
    onEventClick: (CalendarEventInfo) -> Unit = {}
) {
    val hourColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val density = LocalDensity.current

    Layout(
        modifier = modifier.drawBehind {
            // Draw horizontal hour lines
            for (i in 0..23) {
                val y = with(density) { (hourHeight * i).toPx() }
                drawLine(
                    color = hourColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2f
                )
            }
            // draw vertical lines
            val dayWidthPx = size.width / days.size
            for (i in 1 until days.size) {
                val x = dayWidthPx * i
                drawLine(
                    color = hourColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 2f
                )
            }
        },
        content = {
            days.forEach { day ->
                val events = eventsByDay[day] ?: emptyList()
                Day(events = events, onEventClick = onEventClick)
            }
        }
    ) { measurables, constraints ->
        val dayWidthPx = constraints.maxWidth / days.size
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = dayWidthPx, maxWidth = dayWidthPx)) }
        val totalWidth = placeables.sumOf { it.width }
        val totalHeight = with(density) { (hourHeight * 24).toPx() }.roundToInt()

        layout(totalWidth, totalHeight) {
            var xPosition = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(x = xPosition, y = 0)
                xPosition += placeable.width
            }
        }
    }
}

@Composable
fun Day(
    events: List<CalendarEventInfo>,
    onEventClick: (CalendarEventInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    data class EventLayout(
        val event: CalendarEventInfo,
        val maxConcurrentEvents: Int, // The TRUE max overlaps for this event's collision group
        val position: Int             // The horizontal "lane" for this event
    )

    val eventLayouts = remember(events) {
        val sortedEvents = events.sortedBy { it.startDateTime?.value }
        val layouts = mutableListOf<EventLayout>()

        for (event in sortedEvents) {
            val collidingEvents = sortedEvents.filter { other ->
                event.overlaps(other)
            }

            // --- COLLISION SWEEP-LINE ALGORITHM ---
            // 1. Create a list of start (+1) and end (-1) points for the collision group.
            val points = collidingEvents.flatMap {
                listOf(
                    (it.startDateTime?.value ?: 0) to 1,
                    (it.endDateTime?.value ?: 0) to -1
                )
            }.sortedBy { it.first }

            // 2. Sweep through the points to find the peak number of overlaps.
            var maxConcurrentEvents = 0
            var currentOverlaps = 0
            for (point in points) {
                currentOverlaps += point.second
                if (currentOverlaps > maxConcurrentEvents) {
                    maxConcurrentEvents = currentOverlaps
                }
            }

            // Find a horizontal position ("lane") for the current event.
            // This re-uses the stable lane-finding logic.
            val positionsTaken = layouts
                .filter { layout -> layout.event.overlaps(event) }
                .map { it.position }
                .toSet()

            var position = 0
            while (positionsTaken.contains(position)) {
                position++
            }

            layouts.add(EventLayout(event, maxConcurrentEvents.coerceAtLeast(1), position))
        }
        layouts
    }

    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            eventLayouts.forEach { layout ->
                Box(modifier = Modifier.padding(1.dp)) {
                    Event(event = layout.event, onClick = { onEventClick(layout.event) })
                }
            }
        }
    ) { measurables, constraints ->
        val totalHeight = with(density) { (hourHeight * 24).toPx() }.roundToInt()

        val placeables = measurables.mapIndexed { index, measurable ->
            val layout = eventLayouts[index]
            // The denominator is now the correct peak overlap count.
            val colWidth = constraints.maxWidth / layout.maxConcurrentEvents

            val eventHeightPx = with(density) {
                (layout.event.durationInMinutes() / 60f * hourHeight.toPx()).roundToInt()
            }

            val eventWidthPx = colWidth - 2.dp.roundToPx() // Account for padding

            val placeable = measurable.measure(
                constraints.copy(
                    minWidth = eventWidthPx.coerceAtLeast(0),
                    maxWidth = eventWidthPx.coerceAtLeast(0),
                    minHeight = eventHeightPx,
                    maxHeight = eventHeightPx
                )
            )

            val xPos = layout.position * colWidth
            val yPos = layout.event.startDateTime?.let { getEventY(it, density) } ?: 0

            Triple(placeable, xPos, yPos)
        }

        layout(constraints.maxWidth, totalHeight) {
            placeables.forEach { (placeable, x, y) ->
                placeable.placeRelative(x = x, y = y)
            }
        }
    }
}


private fun getEventY(start: com.google.api.client.util.DateTime, density: Density): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = start.value }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    return with(density) { (hour * hourHeight.toPx() + minute / 60f * hourHeight.toPx()).roundToInt() }
}

private fun CalendarEventInfo.durationInMinutes(): Long {
    val start = this.startDateTime?.value ?: 0
    val end = this.endDateTime?.value ?: 0
    return ((end - start) / (1000 * 60)).coerceAtLeast(15) // Show at least a 15-min block
}

private fun CalendarEventInfo.overlaps(other: CalendarEventInfo): Boolean {
    return (this.startDateTime?.value ?: 0) < (other.endDateTime?.value ?: 0) &&
            (this.endDateTime?.value ?: 0) > (other.startDateTime?.value ?: 0)
}

/**
 * Converts a Google DateTime to a Java Calendar, correcting for time zone issues
 * with all-day events.
 * @param isAllDay True if the event is an all-day event.
 */
fun DateTime.valueAsCalendar(isAllDay: Boolean): Calendar {
    return Calendar.getInstance().apply {
        timeInMillis = value
        if (isAllDay) {
            // For all-day events, Google API returns a date at UTC midnight.
            // We need to adjust it to the local time zone to prevent it from
            // appearing on the previous day in some time zones.
            val zoneOffset = timeZone.getOffset(timeInMillis)
            add(Calendar.MILLISECOND, zoneOffset)
        }
    }
}

fun DateTime.toCalendar(): Calendar {
    return Calendar.getInstance().apply { timeInMillis = value }
}

/**
 * Checks if two Calendar objects represent the same year and day-of-year.
 */
fun Calendar.isSameDayAs(other: Calendar): Boolean {
    return this.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            this.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun Event(event: CalendarEventInfo, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    val eventHeight = with(LocalDensity.current) { (event.durationInMinutes() / 60f * hourHeight.toPx()).toDp() }

    Column(
        modifier = modifier
            .height(eventHeight)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Text(
            text = event.summary ?: "(No title)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val startStr = event.startDateTime?.let { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it.value)) } ?: ""
        Text(
            text = startStr,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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