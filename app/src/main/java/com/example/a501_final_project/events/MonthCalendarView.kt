package com.example.a501_final_project.events

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MonthCalendarView(
    events: List<CalendarEventInfo>,
    calendarDataDateRangeStart: Calendar,
    calendarDataDateRangeEnd: Calendar,
    onDaySelected: (Calendar) -> Unit
) {
    var displayedMonth by remember { mutableStateOf(Calendar.getInstance()) }

    Column(Modifier.fillMaxSize()) {

        MonthHeader(
            monthCalendar = displayedMonth,
            onPrev = { displayedMonth = displayedMonth.cloneAs { add(Calendar.MONTH, -1) } },
            onNext = { displayedMonth = displayedMonth.cloneAs { add(Calendar.MONTH, 1) } }
        )

        WeekdayLabels()

        val monthDays = remember(displayedMonth) { generateMonthGrid(displayedMonth) }
        val eventsByDay = remember(events) {
            events.groupBy { it.startDateTime?.toCalendar()?.get(Calendar.DAY_OF_YEAR) ?: -1 }
        }

        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.weight(1f)) {
            items(monthDays) { day ->
                val hasEvents = eventsByDay[day.get(Calendar.DAY_OF_YEAR)]?.isNotEmpty() == true

                DayCell_Month(
                    day = day,
                    isInCurrentMonth = day.get(Calendar.MONTH) == displayedMonth.get(Calendar.MONTH),
                    isInDataDateRange = !day.before(calendarDataDateRangeStart) && !day.after(calendarDataDateRangeEnd),
                    hasEvents = hasEvents,
                    onClick = { onDaySelected(day) }
                )
            }
        }
    }
}

@Composable
fun MonthHeader(
    monthCalendar: Calendar,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) { Icon(Icons.Default.KeyboardArrowLeft, null) }
        Text(SimpleDateFormat("MMMM yyyy").format(monthCalendar.time), style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = onNext) { Icon(Icons.Default.KeyboardArrowRight, null) }
    }
}

@Composable
fun WeekdayLabels() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        listOf("S", "M", "T", "W", "T", "F", "S").forEach {
            Text(
                text = it,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun DayCell_Month(
    day: Calendar,
    isInCurrentMonth: Boolean,
    isInDataDateRange: Boolean,
    hasEvents: Boolean,
    onClick: () -> Unit
) {
    val today = Calendar.getInstance()
    val isToday = day.isSameDayAs(today)

    val clickableModifier = if (isInDataDateRange) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier   // no clickable modifier added → cannot click
    }

    Column(
        modifier = Modifier
            .height(50.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(6.dp))
            .then(clickableModifier)
            .background(
                when {
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    !isInDataDateRange -> Color.Transparent
                    else -> Color.Gray.copy(alpha = 0.2f)
                }
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = day.get(Calendar.DAY_OF_MONTH).toString(),
            color = if (isInCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
        if (hasEvents) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        } else {
            // This spacer takes up the same space as the dot, ensuring consistent height
            Spacer(modifier = Modifier.size(6.dp))
        }
    }
}