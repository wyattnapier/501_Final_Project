package com.example.a501_final_project.events

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a501_final_project.FirestoreRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * data class for Calendar events
 */
data class CalendarEventInfo(
    val id: String,
    val summary: String?,
    val startDateTime: DateTime?,
    val endDateTime: DateTime?,
    val isAllDay: Boolean = false
)

/**
 * define different UI view types for Calendar
 */
enum class CalendarViewType {
    AGENDA,
    THREE_DAY,
    MONTH
}

class EventsViewModel(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
): ViewModel() {
    private val _events = MutableStateFlow<List<CalendarEventInfo>>(emptyList())
    val events: StateFlow<List<CalendarEventInfo>> = _events.asStateFlow()

    // State to manage the current calendar view
    private val _calendarViewType = MutableStateFlow(CalendarViewType.AGENDA)
    val calendarViewType = _calendarViewType.asStateFlow()

    private val _calendarError = MutableStateFlow<String?>(null)
    val calendarError = _calendarError.asStateFlow()

    private val _isLoadingCalendar = MutableStateFlow(false)
    val isLoadingCalendar = _isLoadingCalendar.asStateFlow()

    private val numCalendarDataDays = 28

    private val _calendarDataDateRangeStart = MutableStateFlow(Calendar.getInstance())
    val calendarDataDateRangeStart = _calendarDataDateRangeStart.asStateFlow()

    private val _calendarDataDateRangeEnd = MutableStateFlow(
        (Calendar.getInstance().clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, numCalendarDataDays)
        }
    )
    val calendarDataDateRangeEnd = _calendarDataDateRangeEnd.asStateFlow()

    // The leftmost date of 3-day view
    private val _leftDayForThreeDay = MutableStateFlow(Calendar.getInstance())
    val leftDayForThreeDay: StateFlow<Calendar> = _leftDayForThreeDay.asStateFlow()

    private val _householdCalendarName = MutableStateFlow<String?>(null)
    val householdCalendarName: StateFlow<String?> = _householdCalendarName.asStateFlow()

    init {
        loadHouseholdCalendarName()
    }

    /**
     * Load and cache the household calendar name
     */
    fun loadHouseholdCalendarName() {
        firestoreRepository.getHouseholdCalendarNameWithoutId(
            onSuccess = { calendarName ->
                _householdCalendarName.value = calendarName
                Log.d("EventsViewModel", "Loaded calendar name: $calendarName")
            },
            onFailure = { exception ->
                Log.e("EventsViewModel", "Failed to load calendar name", exception)
                _calendarError.value = "Could not load calendar name: ${exception.message}"
            }
        )
    }

    fun setLeftDayForThreeDay(day: Calendar) {
        _leftDayForThreeDay.value = day
    }

    // change the view type
    fun setCalendarView(viewType: CalendarViewType) {
        _calendarViewType.value = viewType
    }

    fun incrementThreeDayView() {
        val currentLeftDay = _leftDayForThreeDay.value.clone() as Calendar
        currentLeftDay.add(Calendar.DAY_OF_YEAR, 1)

        // The view shows 3 days, so the last visible day is leftDay + 2
        val lastVisibleDay = currentLeftDay.clone() as Calendar
        lastVisibleDay.add(Calendar.DAY_OF_YEAR, 2)

        // Allow incrementing if the last visible day is not after the 14-day end date
        if (!lastVisibleDay.after(_calendarDataDateRangeEnd.value)) {
            _leftDayForThreeDay.value = currentLeftDay
        }
    }

    fun decrementThreeDayView() {
        val currentLeftDay = _leftDayForThreeDay.value.clone() as Calendar
        currentLeftDay.add(Calendar.DAY_OF_YEAR, -1)

        // Allow decrementing if the new leftDay is not before today (the start of the 14-day range)
        if (!currentLeftDay.before(_calendarDataDateRangeStart.value)) {
            _leftDayForThreeDay.value = currentLeftDay
        }
    }

    /** Called when user clicks a day in the month calendar */
    fun onDaySelected(clickedDay: Calendar) {
        val startRange = calendarDataDateRangeStart.value
        val endRange = calendarDataDateRangeEnd.value
        var potentialLeftDay = clickedDay.clone() as Calendar

        // The potential last day of the 3-day view if we use the clicked day as the start.
        val potentialRightDay = (clickedDay.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 2)
        }

        // SCENARIO 1: The clicked day is before the valid range.
        // Adjust the view to start on the first valid day.
        if (potentialLeftDay.before(startRange)) {
            potentialLeftDay = startRange.clone() as Calendar
        }
        // SCENARIO 2: The resulting 3-day view would extend beyond the valid range.
        // Adjust the view to end on the last valid day.
        else if (potentialRightDay.after(endRange)) {
            // To make `endRange` the rightmost day, the `leftDay` must be `endRange` - 3 days.
            potentialLeftDay = (endRange.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -3)
            }
        }

        // Set the correctly adjusted left day and switch the view.
        Log.d("MainViewModel","Last day of range is $potentialRightDay and range end is $endRange")
        Log.d("MainViewModel", "Clicked day was $clickedDay but setting left day to $potentialLeftDay")
        setLeftDayForThreeDay(potentialLeftDay)
        setCalendarView(CalendarViewType.THREE_DAY)
    }

    /**
     * Finds the calendar ID for a calendar with a specific name.
     * @param calendarService The authenticated Calendar API service instance.
     * @param calendarName The name of the calendar to find (e.g., "Other Events").
     * @return The calendarId string, or null if not found.
     */
    private suspend fun getCalendarIdByName(
        calendarService: com.google.api.services.calendar.Calendar,
        calendarName: String?
    ): String? {
        return try {
            val calendarList = calendarService.calendarList().list().execute()
            calendarList.items.find { it.summary.equals(calendarName, ignoreCase = true) }?.id
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to get calendar list", e)
            null
        }
    }

    fun fetchCalendarEvents(
        context: Context,
        days: Int = numCalendarDataDays,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val googleAccount = GoogleSignIn.getLastSignedInAccount(context)

            if (googleAccount == null) {
                _calendarError.value = "Cannot refresh events: User is not signed in."
                Log.e("EventsViewModel", "fetchCalendarEvents failed: GoogleSignInAccount is null.")
                return@launch
            }

            // Get the cached calendar name
            val calendarName = _householdCalendarName.value // no fallback!

            _isLoadingCalendar.value = true
            _calendarError.value = null
            try {
                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf(CalendarScopes.CALENDAR_READONLY)
                ).apply {
                    selectedAccount = googleAccount.account
                }

                val calendarService = com.google.api.services.calendar.Calendar.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                )
                    .setApplicationName("501_Final_Project")
                    .build()

                Log.d("EventsViewModel", "Calendar name right before getting target calendar id is is $calendarName")
                val targetCalendarId = getCalendarIdByName(calendarService, calendarName)

                if (targetCalendarId == null) {
                    _calendarError.value = "Calendar '$calendarName' not found."
                    Log.e("EventsViewModel", "Could not find calendar '$calendarName'.")
                    _isLoadingCalendar.value = false
                    return@launch
                }


                // set time range for fetching events
                val now = DateTime(System.currentTimeMillis())
                val timeMax = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, days)
                }.timeInMillis
                val maxDateTime = DateTime(timeMax)

                // fetch events for the target calendar ID
                val eventsResult = calendarService.events().list(targetCalendarId)
                    .setTimeMin(now)
                    .setTimeMax(maxDateTime)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute()

                val items = eventsResult.items.mapNotNull { event ->
                    val isAllDay = event.start?.dateTime == null && event.start?.date != null

                    if (isAllDay) {
                        val startString = event.start.date.toString()
                        // The end date from the API is exclusive, so we need to get it too.
                        val endString = event.end?.date?.toString()

                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val startDate = dateFormat.parse(startString)

                        if (startDate != null) {
                            val localStartCal = Calendar.getInstance().apply {
                                time = startDate
                            }
                            val startDateTime = DateTime(localStartCal.time)

                            // Now, handle the end date.
                            val localEndCal: Calendar
                            if (endString != null) {
                                val endDate = dateFormat.parse(endString)
                                localEndCal = Calendar.getInstance().apply {
                                    time = endDate
                                    // The API end date is exclusive, so subtract 1 day to get the inclusive end day.
                                    add(Calendar.DAY_OF_YEAR, -1)
                                }
                            } else {
                                // Fallback for safety, though API should always provide an end date.
                                localEndCal = localStartCal.clone() as Calendar
                            }

                            // Set the time to the very end of the final day.
                            localEndCal.set(Calendar.HOUR_OF_DAY, 23)
                            localEndCal.set(Calendar.MINUTE, 59)
                            localEndCal.set(Calendar.SECOND, 59)

                            val endDateTime = DateTime(localEndCal.time)

                            Log.d("MainViewModel", "Summary: ${event.summary}, Start date time: $startDateTime, End date time: $endDateTime")
                            CalendarEventInfo(
                                id = event.id,
                                summary = event.summary,
                                startDateTime = startDateTime,
                                endDateTime = endDateTime,
                                isAllDay = true
                            )
                        } else {
                            null // Skip if date is invalid
                        }
                    } else {
                        // Timed events logic remains the same
                        val startDateTime = event.start?.dateTime
                        val endDateTime = event.end?.dateTime
                        if (startDateTime != null && endDateTime != null) {
                            CalendarEventInfo(
                                id = event.id,
                                summary = event.summary,
                                startDateTime = startDateTime,
                                endDateTime = endDateTime,
                                isAllDay = false
                            )
                        } else { null } // Skip timed events with invalid dates
                    }
                }
                _events.value = items
            } catch (e: Exception) {
                Log.e("EventsViewModel", "Calendar API error", e)
                _calendarError.value = "Failed to fetch events: ${e.message}"
            } finally {
                _isLoadingCalendar.value = false
            }
        }
    }
    fun addCalendarEvent(
        context: Context,
        summary: String,
        description: String?,
        startTime: Calendar,
        endTime: Calendar,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null) {
                _calendarError.value = "Cannot add event: User not signed in."
                return@launch
            }

            // Get the cached calendar name
            val calendarName = _householdCalendarName.value
            if (calendarName == null) {
                _calendarError.value = "Calendar name not loaded"
                return@launch
            }

            try {
                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    setOf(CalendarScopes.CALENDAR)
                ).setSelectedAccount(account.account)

                val calendarService = com.google.api.services.calendar.Calendar.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                ).setApplicationName("501-Final-Project").build()

                val calendarIdForEvent = getCalendarIdByName(calendarService, calendarName)

                if (calendarIdForEvent == null) {
                    _calendarError.value = "Calendar '$calendarName' not found"
                    return@launch
                }
                Log.d("EventsViewModel", "Adding event to calendar ID: $calendarIdForEvent")

                val event = Event().apply {
                    this.summary = summary
                    this.description = description
                    start = EventDateTime().setDateTime(DateTime(startTime.time))
                    end = EventDateTime().setDateTime(DateTime(endTime.time))
                }

                calendarService.events().insert(calendarIdForEvent, event).execute()

                // Refresh the events list to show the new event
                fetchCalendarEvents(context)

            } catch (e: Exception) {
                Log.e("EventsViewModel", "Failed to add event", e)
                _calendarError.value = "Error adding event: ${e.message}"
            }
        }
    }
}