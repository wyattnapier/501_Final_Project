package com.example.a501_final_project.events

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.a501_final_project.CalendarEventInfo
import com.google.api.client.util.DateTime
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private val hourHeight = 60.dp
private val sidebarWidth = 60.dp
private val am_pm_time_formatter = SimpleDateFormat("h a", Locale.getDefault()) // TODO: make dynamic/stateful

@Composable
fun ThreeDayView(
    leftDay: Calendar,
    events: List<CalendarEventInfo>,
    modifier: Modifier = Modifier,
    onEventClick: (CalendarEventInfo) -> Unit = {},
    onIncrementDay: () -> Unit,
    onDecrementDay: () -> Unit,
    canIncrement: Boolean,
    canDecrement: Boolean,
) {
    val days = remember(leftDay) {
        listOf(
            leftDay.clone() as Calendar,
            (leftDay.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) },
            (leftDay.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 2) }
        )
    }

    val allDayEventsByDay = remember(events, days) {
        // First, identify all events that should be treated as all-day.
        // This includes true all-day events and timed events that span across midnight.
        val spanningOrAllDayEvents = events.filter { event ->
            event.isAllDay || (event.startDateTime != null && !event.startDateTime.toCalendar().isSameDayAs(
                event.endDateTime?.toCalendar()
            ))
        }

        // Then, for each day in the view, find which of these events fall on that day.
        days.associateWith { day ->
            val dayStart = day.cloneAs { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }
            val dayEnd = day.cloneAs { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }

            spanningOrAllDayEvents.filter { event ->
                val eventStart = event.startDateTime?.toCalendar()
                val eventEnd = event.endDateTime?.toCalendar()

                // An event is on this day if the day is not completely after the event ends,
                // and the day is not completely before the event starts.
                eventStart != null && eventEnd != null &&
                        !dayStart.after(eventEnd) && !dayEnd.before(eventStart)
            }
        }
    }

    val timedEventsByDay = remember(events, days) {
        days.associateWith { day ->
            events.filter { event ->
                !event.isAllDay &&
                        event.startDateTime != null &&
                        event.startDateTime.toCalendar().isSameDayAs(event.endDateTime?.toCalendar()) &&
                        event.startDateTime.toCalendar().isSameDayAs(day)
            }
        }
    }

    val scrollState = rememberScrollState(1090)

    Column(modifier.fillMaxSize()) {
        DayHeaders(
            days = days,
            onIncrementDay = onIncrementDay,
            onDecrementDay = onDecrementDay,
            canIncrement = canIncrement,
            canDecrement = canDecrement
        )
        AllDayEventsHeader(days, allDayEventsByDay, onEventClick)
        Divider()
        Row(
            Modifier.fillMaxSize().verticalScroll(scrollState)
        ) {
            HourSidebar(Modifier.width(sidebarWidth).height(hourHeight * 24))
            BasicCalendar(
                days = days,
                eventsByDay = timedEventsByDay,
                onEventClick = onEventClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DayHeaders(
    days: List<Calendar>,
    onIncrementDay: () -> Unit,
    onDecrementDay: () -> Unit,
    canIncrement: Boolean,
    canDecrement: Boolean
) {
    Row(modifier = Modifier
            .fillMaxWidth()
            .padding(start = sidebarWidth), // Pad for the hour sidebar
        verticalAlignment = Alignment.CenterVertically
    ) {
        Log.d("DayHeaders", "canIncrement: $canIncrement, canDecrement: $canDecrement")
        // Decrement (Left Arrow) Button
        IconButton(
            onClick = onDecrementDay,
            enabled = canDecrement,
            modifier = Modifier.alpha(if (canDecrement) 1f else 0f)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous Day",
            )
        }
        days.forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = SimpleDateFormat("EEE", Locale.getDefault()).format(day.time), style = MaterialTheme.typography.labelSmall)
                Text(text = SimpleDateFormat("d", Locale.getDefault()).format(day.time), style = MaterialTheme.typography.titleMedium)
            }
        }
        // Increment (Right Arrow) Button
        IconButton(
            onClick = onIncrementDay,
            enabled = canIncrement,
            modifier = Modifier.alpha(if (canIncrement) 1f else 0f)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next Day",
            )
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
            .heightIn(min = 8.dp, max = 50.dp) // max - cut off end of second event so it is clear that its scrollable
            .padding(start = sidebarWidth) // Align with the calendar grid
    ) {
        days.forEach { day ->
            val eventsForDay = allDayEventsByDay[day] ?: emptyList()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                eventsForDay.forEach { event ->
                    AllDayEventItem(
                        event = event,
                        onClick = { onEventClick(event) }
                    )
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
    modifier: Modifier = Modifier,
    onEventClick: (CalendarEventInfo) -> Unit = {},
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

@Composable
fun Event(event: CalendarEventInfo, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
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

// ----------- helper functions

private fun getEventY(start: DateTime, density: Density): Int {
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