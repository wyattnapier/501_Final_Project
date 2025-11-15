package com.example.a501_final_project

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    val startStr = event.startDateTime?.let {
                        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(it.value))
                    } ?: "N/A"
                    val endStr = event.endDateTime?.let {
                        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(it.value))
                    } ?: "N/A"
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

    val eventsByDay = days.associateWith { day ->
        events.filter { event ->
            if (event.startDateTime == null) return@filter false
            val eventCal = Calendar.getInstance().apply { timeInMillis = event.startDateTime.value }
            eventCal.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                    eventCal.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        DayHeaders(days)
        Row(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            HourSidebar(modifier = Modifier.width(sidebarWidth).height(hourHeight * 24))
            BasicCalendar(
                modifier = Modifier.weight(1f),
                days = days,
                eventsByDay = eventsByDay,
                onEventClick = onEventClick
            )
        }
    }
}

@Composable
fun DayHeaders(days: List<Calendar>) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = sidebarWidth)) { // Pad for the hour sidebar
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
                    strokeWidth = 1f
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
    // 1. Define eventGroups *before* the Layout so it's accessible in both lambdas.
    val eventGroups = remember(events) {
        mutableListOf<MutableList<CalendarEventInfo>>().also { groups ->
            events.sortedBy { it.startDateTime?.value }.forEach { event ->
                val group = groups.find { g -> g.none { it.overlaps(event) } }
                if (group != null) {
                    group.add(event)
                } else {
                    groups.add(mutableListOf(event))
                }
            }
        }
    }

    val density = LocalDensity.current

    // Calculate optimal width for each event based on group size
    // Events get wider when there are fewer overlaps
    val eventWidthFractions = remember(eventGroups) {
        eventGroups.flatMap { group ->
            group.mapIndexed { index, event ->
                event to when (group.size) {
                    1 -> 1.0f // Full width if no overlaps
                    2 -> 0.6f // 60% width if 2 overlaps
                    else -> 1f / group.size // Equal split for 3+
                }
            }
        }.toMap()
    }

    Layout(
        modifier = modifier,
        content = {
            // 2. The content block now just creates the UI elements.
            eventGroups.forEach { group ->
                group.forEach { event ->
                    Box(modifier = EventData(position = group.indexOf(event), total = group.size)) {
                        Event(event = event, onClick = { onEventClick(event) })
                    }
                }
            }
        }
    ) { measurables, constraints ->
        val totalHeight = with(density) { (hourHeight * 24).toPx() }.roundToInt()

        val placeables = measurables.map {
            val eventData = it.parentData as EventData
            val event = eventGroups.flatMap { g -> g }[measurables.indexOf(it)]
            val eventHeightPx = with(density) { (event.durationInMinutes() / 60f * hourHeight.toPx()).roundToInt() }
            val widthFraction = eventWidthFractions[event] ?: (1f / eventData.total)
            val eventWidth = (constraints.maxWidth * widthFraction).toInt()

            it.measure(constraints.copy(
                minWidth = eventWidth,
                maxWidth = eventWidth,
                minHeight = eventHeightPx,
                maxHeight = eventHeightPx
            ))
        }

        layout(constraints.maxWidth, totalHeight) {
            placeables.forEachIndexed { index, placeable ->
                val eventData = placeable.parentData as EventData
                // 3. Find the event using the index, which is much simpler and more reliable.
                val event = eventGroups.flatMap { it }[index]

                val yPosition = event.startDateTime?.let { getEventY(it, density) } ?: 0
                val xPosition = eventData.position * (constraints.maxWidth / eventData.total)

                placeable.placeRelative(x = xPosition, y = yPosition)
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

@Composable
fun Event(event: CalendarEventInfo, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    val eventHeight = with(LocalDensity.current) { (event.durationInMinutes() / 60f * hourHeight.toPx()).toDp() }

    Column(
        modifier = modifier
            .fillMaxWidth()
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
                        text = "Summary: $it",
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