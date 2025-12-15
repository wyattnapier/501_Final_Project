package com.example.a501_final_project.events

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a501_final_project.FirestoreRepository
import com.example.a501_final_project.IRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CalendarEventInfo(
    val id: String,
    val summary: String?,
    val startDateTime: DateTime?,
    val endDateTime: DateTime?,
    val isAllDay: Boolean = false
)

enum class CalendarViewType {
    AGENDA,
    THREE_DAY,
    MONTH
}

class EventsViewModel(
    private val firestoreRepository: IRepository = FirestoreRepository()
): ViewModel() {
    private val _events = MutableStateFlow<List<CalendarEventInfo>>(emptyList())
    val events: StateFlow<List<CalendarEventInfo>> = _events.asStateFlow()

    private val _calendarViewType = MutableStateFlow(CalendarViewType.AGENDA)
    val calendarViewType = _calendarViewType.asStateFlow()

    private val _calendarError = MutableStateFlow<String?>(null)
    val calendarError = _calendarError.asStateFlow()

    private val _isLoadingCalendar = MutableStateFlow(false)
    val isLoadingCalendar = _isLoadingCalendar.asStateFlow()

    // New state for calendar sharing progress
    private val _isSharingCalendar = MutableStateFlow(false)
    val isSharingCalendar = _isSharingCalendar.asStateFlow()

    private val _sharingProgress = MutableStateFlow<String?>(null)
    val sharingProgress = _sharingProgress.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val numCalendarDataDays = 28

    private val _calendarDataDateRangeStart = MutableStateFlow(Calendar.getInstance())
    val calendarDataDateRangeStart = _calendarDataDateRangeStart.asStateFlow()

    private val _calendarDataDateRangeEnd = MutableStateFlow(
        (Calendar.getInstance().clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, numCalendarDataDays)
        }
    )
    val calendarDataDateRangeEnd = _calendarDataDateRangeEnd.asStateFlow()

    private val _leftDayForThreeDay = MutableStateFlow(Calendar.getInstance())
    val leftDayForThreeDay: StateFlow<Calendar> = _leftDayForThreeDay.asStateFlow()

    private val _householdCalendarId = MutableStateFlow<String?>(null)
    val householdCalendarId: StateFlow<String?> = _householdCalendarId.asStateFlow()

    private val _isCalendarIdLoaded = MutableStateFlow(false)
    val isCalendarIdLoaded: StateFlow<Boolean> = _isCalendarIdLoaded.asStateFlow()

    /**
     * Load and cache the household calendar name
     */
    fun loadCalendarData(context: Context, forceReload: Boolean = false) {
        Log.d("EventsViewModel", ">>> loadCalendarData() CALLED <<<")
        Log.d("EventsViewModel", "forceReload: $forceReload, _isCalendarIdLoaded: ${_isCalendarIdLoaded.value}")

        // Allow reload if forced, or if not already loaded
        if (_isCalendarIdLoaded.value && !forceReload) {
            Log.d("EventsViewModel", "Calendar data already loaded, skipping")
            return
        }

        viewModelScope.launch {
            try {
                _isLoadingCalendar.value = true
                _calendarError.value = null

                // 1. Fetch calendar data from repository
                Log.d("EventsViewModel", "Fetching household calendar data from Firestore")
                val calendarDataMap = firestoreRepository.getHouseholdCalendarIdAndPendingMembersSuspend()
                val householdId = calendarDataMap["household_id"] as? String
                val calendarId = calendarDataMap["calendar_id"] as? String
                val pendingMembers = calendarDataMap["pending_members"] as? List<String>

                if (householdId.isNullOrBlank() || calendarId.isNullOrBlank()) {
                    Log.e("EventsViewModel", "Household ID [$householdId] or calendar ID [$calendarId] is null or blank")
                    throw Exception("Household calendar information is missing")
                }

                _householdCalendarId.value = calendarId
                Log.d("EventsViewModel", "Loaded calendar ID: $calendarId")

                // 2. Process pending invites if any exist (non-blocking)
                if (!pendingMembers.isNullOrEmpty()) {
                    Log.d("EventsViewModel", "Found ${pendingMembers.size} pending members to invite")
                    _isSharingCalendar.value = true
                    _sharingProgress.value = "Setting up calendar access for new members..."

                    // Process invites without blocking calendar fetch
                    launch {
                        try {
                            processPendingInvites(context, householdId, calendarId, pendingMembers)
                            _sharingProgress.value = "Calendar access granted successfully"
                            delay(2000) // Show success message briefly
                            _sharingProgress.value = null
                        } catch (e: Exception) {
                            Log.e("EventsViewModel", "Error processing pending invites", e)
                            _sharingProgress.value = "Some calendar shares may have failed"
                            delay(3000)
                            _sharingProgress.value = null
                        } finally {
                            _isSharingCalendar.value = false
                        }
                    }

                    // Small delay to let initial shares start
                    delay(1000)
                }

                // 3. Fetch events (don't wait for shares to complete)
                Log.d("EventsViewModel", "Fetching calendar events")
                fetchCalendarEvents(context, calendarId)

            } catch (e: Exception) {
                Log.e("EventsViewModel", "Failed to load calendar data", e)
                _calendarError.value = "Could not load household calendar: ${e.message}"
            } finally {
                _isCalendarIdLoaded.value = true
                _isLoadingCalendar.value = false
            }
        }
    }

    /**
     * Process pending calendar invites in parallel for better performance
     */
    private suspend fun processPendingInvites(
        context: Context,
        householdId: String,
        calendarId: String,
        pendingEmails: List<String>
    ) = withContext(Dispatchers.IO) {
        Log.d("EventsViewModel", "Processing ${pendingEmails.size} pending invites")

        // Process all invites in parallel
        val results = pendingEmails.mapIndexed { index, email ->
            async {
                try {
                    withContext(Dispatchers.Main) {
                        _sharingProgress.value = "Sharing with ${index + 1}/${pendingEmails.size} members..."
                    }

                    shareGoogleCalendar(context, calendarId, email)
                    firestoreRepository.removePendingMember(householdId, email)
                    Log.d("EventsViewModel", "Successfully shared with $email")
                    true
                } catch (e: Exception) {
                    Log.e("EventsViewModel", "Failed to share calendar with $email: ${e.message}", e)
                    false
                }
            }
        }.awaitAll()

        val successCount = results.count { it }
        Log.d("EventsViewModel", "Successfully processed $successCount/${pendingEmails.size} invites")
        val message = if (successCount > 0) {
            "Successfully invited $successCount/${pendingEmails.size} new member(s) to the calendar."
        } else {
            null // Don't show a toast if nothing happened
        }
        if (successCount < pendingEmails.size) {
            withContext(Dispatchers.Main) {
                Log.d("EventsViewModel", "Some calendar shares failed. ${pendingEmails.size - successCount} member(s) may not have access.")
            }
        }
        withContext(Dispatchers.Main) {
            _toastMessage.value = message
        }
    }

    /**
     * Share an existing Google Calendar with a new user
     */
    private suspend fun shareGoogleCalendar(
        context: Context,
        calendarId: String,
        newUserEmail: String
    ) = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            Log.e("EventsViewModel", "Cannot share: No signed-in account")
            throw Exception("No signed-in account")
        }

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(CalendarScopes.CALENDAR)
        ).apply { selectedAccount = account.account }

        val calendarService = com.google.api.services.calendar.Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("501_Final_Project").build()

        try {
            // Create ACL rule for the new user
            val scope = com.google.api.services.calendar.model.AclRule.Scope().apply {
                type = "user"
                value = newUserEmail
            }

            val rule = com.google.api.services.calendar.model.AclRule().apply {
                this.scope = scope
                role = "owner"
            }

            calendarService.acl().insert(calendarId, rule).execute()
            Log.d("EventsViewModel", "Successfully shared calendar with $newUserEmail")

        } catch (e: UserRecoverableAuthIOException) {
            Log.e("EventsViewModel", "Need additional permissions to share calendar", e)
            throw Exception("Additional permissions needed: ${e.message}")
        } catch (e: Exception) {
            Log.e("EventsViewModel", "Failed to share Google Calendar with $newUserEmail", e)
            throw e
        }
    }

    fun setLeftDayForThreeDay(day: Calendar) {
        _leftDayForThreeDay.value = day
    }

    fun setCalendarView(viewType: CalendarViewType) {
        _calendarViewType.value = viewType
    }

    fun incrementThreeDayView() {
        val currentLeftDay = _leftDayForThreeDay.value.clone() as Calendar
        currentLeftDay.add(Calendar.DAY_OF_YEAR, 1)

        val lastVisibleDay = currentLeftDay.clone() as Calendar
        lastVisibleDay.add(Calendar.DAY_OF_YEAR, 2)

        if (!lastVisibleDay.after(_calendarDataDateRangeEnd.value)) {
            _leftDayForThreeDay.value = currentLeftDay
        }
    }

    fun decrementThreeDayView() {
        val currentLeftDay = _leftDayForThreeDay.value.clone() as Calendar
        currentLeftDay.add(Calendar.DAY_OF_YEAR, -1)

        if (!currentLeftDay.before(_calendarDataDateRangeStart.value)) {
            _leftDayForThreeDay.value = currentLeftDay
        }
    }

    fun onDaySelected(clickedDay: Calendar) {
        val startRange = calendarDataDateRangeStart.value
        val endRange = calendarDataDateRangeEnd.value
        var potentialLeftDay = clickedDay.clone() as Calendar

        val potentialRightDay = (clickedDay.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 2)
        }

        if (potentialLeftDay.before(startRange)) {
            potentialLeftDay = startRange.clone() as Calendar
        } else if (potentialRightDay.after(endRange)) {
            potentialLeftDay = (endRange.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -3)
            }
        }

        Log.d("EventsViewModel", "Setting left day to $potentialLeftDay")
        setLeftDayForThreeDay(potentialLeftDay)
        setCalendarView(CalendarViewType.THREE_DAY)
    }

    fun fetchCalendarEvents(
        context: Context,
        targetCalendarId: String,
        days: Int = numCalendarDataDays,
    ) {
        Log.d("EventsViewModel", "fetchCalendarEvents called with targetCalendarId: $targetCalendarId")

        viewModelScope.launch(Dispatchers.IO) {
            val googleAccount = GoogleSignIn.getLastSignedInAccount(context)

            if (googleAccount == null) {
                withContext(Dispatchers.Main) {
                    _calendarError.value = "Cannot refresh events: User is not signed in."
                }
                Log.e("EventsViewModel", "fetchCalendarEvents failed: GoogleSignInAccount is null")
                return@launch
            }

            if (targetCalendarId.isBlank()) {
                withContext(Dispatchers.Main) {
                    _calendarError.value = "Household calendar ID is missing."
                }
                Log.e("EventsViewModel", "fetchCalendarEvents failed: targetCalendarId is blank")
                return@launch
            }

            withContext(Dispatchers.Main) {
                _isLoadingCalendar.value = true
                _calendarError.value = null
            }

            try {
                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf(CalendarScopes.CALENDAR)
                ).apply {
                    selectedAccount = googleAccount.account
                }

                val calendarService = com.google.api.services.calendar.Calendar.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                ).setApplicationName("501_Final_Project").build()

                Log.d("EventsViewModel", "Fetching events for calendar ID: $targetCalendarId")

                val now = DateTime(System.currentTimeMillis())
                val timeMax = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, days)
                }.timeInMillis
                val maxDateTime = DateTime(timeMax)

                val eventsResult = calendarService.events().list(targetCalendarId)
                    .setTimeMin(now)
                    .setTimeMax(maxDateTime)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute()

                Log.d("EventsViewModel", "Fetched ${eventsResult.items.size} events")

                val items = eventsResult.items.mapNotNull { event ->
                    val isAllDay = event.start?.dateTime == null && event.start?.date != null

                    if (isAllDay) {
                        val startString = event.start.date.toString()
                        val endString = event.end?.date?.toString()

                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val startDate = dateFormat.parse(startString)

                        if (startDate != null) {
                            val localStartCal = Calendar.getInstance().apply {
                                time = startDate
                            }
                            val startDateTime = DateTime(localStartCal.time)

                            val localEndCal: Calendar
                            if (endString != null) {
                                val endDate = dateFormat.parse(endString)
                                localEndCal = Calendar.getInstance().apply {
                                    time = endDate
                                    add(Calendar.DAY_OF_YEAR, -1)
                                }
                            } else {
                                localEndCal = localStartCal.clone() as Calendar
                            }

                            localEndCal.set(Calendar.HOUR_OF_DAY, 23)
                            localEndCal.set(Calendar.MINUTE, 59)
                            localEndCal.set(Calendar.SECOND, 59)

                            val endDateTime = DateTime(localEndCal.time)

                            CalendarEventInfo(
                                id = event.id,
                                summary = event.summary,
                                startDateTime = startDateTime,
                                endDateTime = endDateTime,
                                isAllDay = true
                            )
                        } else {
                            null
                        }
                    } else {
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
                        } else {
                            null
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    _events.value = items
                    Log.d("EventsViewModel", "Updated events state with ${items.size} events")
                }

            } catch (e: UserRecoverableAuthIOException) {
                Log.e("EventsViewModel", "Need additional permissions", e)
                withContext(Dispatchers.Main) {
                    _calendarError.value = "Additional calendar permissions needed. Please sign in again."
                }
            } catch (e: GoogleJsonResponseException) {
                // This block specifically catches errors from the Google API
                if (e.statusCode == 404) {
                    Log.w("EventsViewModel", "Calendar not found (404). Access is likely pending.", e)
                    withContext(Dispatchers.Main) {
                        _calendarError.value = "Access to the shared calendar is pending. Please try again later."
                    }
                } else {
                    Log.e("EventsViewModel", "Google API error with code: ${e.statusCode}", e)
                    withContext(Dispatchers.Main) {
                        _calendarError.value = "Calendar API error: ${e.message}"
                    }
                }
            } catch (e: Exception) {
                Log.e("EventsViewModel", "Calendar API error", e)
                withContext(Dispatchers.Main) {
                    _calendarError.value = "Failed to fetch events: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoadingCalendar.value = false
                }
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
                withContext(Dispatchers.Main) {
                    _calendarError.value = "Cannot add event: User not signed in."
                }
                return@launch
            }

            val calendarIdForEvent = _householdCalendarId.value
            if (calendarIdForEvent.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    _calendarError.value = "Household calendar ID not loaded."
                }
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

                Log.d("EventsViewModel", "Adding event to calendar ID: $calendarIdForEvent")

                val event = Event().apply {
                    this.summary = summary
                    this.description = description
                    start = EventDateTime().setDateTime(DateTime(startTime.time))
                    end = EventDateTime().setDateTime(DateTime(endTime.time))
                }

                calendarService.events().insert(calendarIdForEvent, event).execute()

                Log.d("EventsViewModel", "Event added successfully, refreshing events")
                fetchCalendarEvents(context, calendarIdForEvent)

            } catch (e: Exception) {
                Log.e("EventsViewModel", "Failed to add event", e)
                withContext(Dispatchers.Main) {
                    _calendarError.value = "Error adding event: ${e.message}"
                }
            }
        }
    }
    /**
     * Resets the toast message state to null after it has been shown.
     */
    fun clearToastMessage() {
        _toastMessage.value = null
    }

    // clear all state on sign out
    fun reset() {
        _events.value = emptyList()
        _householdCalendarId.value = null
        _isCalendarIdLoaded.value = false
        _calendarError.value = null
        Log.d("EventsViewModel", "State has been reset.")
    }
}