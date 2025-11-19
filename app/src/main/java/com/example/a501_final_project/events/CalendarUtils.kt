package com.example.a501_final_project.events

import com.google.api.client.util.DateTime
import java.util.*

fun DateTime.toCalendar(): Calendar =
    Calendar.getInstance().apply { timeInMillis = value }

fun DateTime.toCalendarAtMidnight(): Calendar =
    this.toCalendar().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

fun Calendar.isSameDayAs(other: Calendar?): Boolean {
    if (other == null) return false
    return this.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            this.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}

fun Calendar.cloneAs(block: Calendar.() -> Unit): Calendar =
    (this.clone() as Calendar).apply(block)

fun generateMonthGrid(monthCalendar: Calendar): List<Calendar> {
    val cal = monthCalendar.cloneAs { set(Calendar.DAY_OF_MONTH, 1) }
    val offset = cal.get(Calendar.DAY_OF_WEEK) - 1
    cal.add(Calendar.DAY_OF_YEAR, -offset)
    return List(42) { i -> cal.cloneAs { add(Calendar.DAY_OF_YEAR, i) } }
}