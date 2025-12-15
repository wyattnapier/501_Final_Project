package com.example.a501_final_project.events

import android.content.Context
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.a501_final_project.FirestoreRepository
import com.example.a501_final_project.IRepository
import com.example.a501_final_project.login_register.LoginViewModel
import com.example.a501_final_project.login_register.UserState
import com.google.api.client.util.DateTime
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import java.util.*

class EventsScreenTest {
    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockRepository: IRepository

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var eventsViewModel: EventsViewModel
    private lateinit var context: Context

    //helper to check if user is ready
    fun loginViewModel_stateIsReadyInSetup() {
        assertEquals(UserState.READY, loginViewModel.userState.value)
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        loginViewModel = LoginViewModel(mockRepository)

        // Set user state to READY so UI elements are enabled
        val userState = loginViewModel::class.java.getDeclaredField("_userState").apply {
            isAccessible = true
        }.get(loginViewModel) as MutableStateFlow<UserState>
        userState.value = UserState.READY

        eventsViewModel = EventsViewModel(mockRepository)

        // Set a household calendar ID so buttons are enabled
        val calendarId = eventsViewModel::class.java.getDeclaredField("_householdCalendarId").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<String?>
        calendarId.value = "test-calendar-id"

        val eventsList = listOf(
            CalendarEventInfo(
                id = "event_123",
                summary = "Team Standup",
                startDateTime = DateTime("2025-01-15T10:00:00-05:00"),
                endDateTime = DateTime("2025-01-15T10:30:00-05:00"),
                isAllDay = false
            )
        )
        val events = eventsViewModel::class.java.getDeclaredField("_events").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<List<CalendarEventInfo>>
        events.value = eventsList

    }

    @Test
    fun eventsScreen_showsCalendarViewSwitcher() {
        composeTestRule.setContent {
            EventsScreen(
                loginViewModel = loginViewModel,
                eventsViewModel = eventsViewModel
            )
        }

        composeTestRule.onNodeWithText("Agenda").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 Day").assertIsDisplayed()
        composeTestRule.onNodeWithText("Month").assertIsDisplayed()
    }

    @Test
    fun eventsScreen_showsAddEventButton() {
        val userState = loginViewModel::class.java.getDeclaredField("_userState").apply {
            isAccessible = true
        }.get(loginViewModel) as MutableStateFlow<UserState>
        userState.value = UserState.READY

        loginViewModel_stateIsReadyInSetup()

        eventsViewModel = EventsViewModel(mockRepository)

        // Set a household calendar ID so buttons are enabled
        val calendarId = eventsViewModel::class.java.getDeclaredField("_householdCalendarId").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<String?>
        calendarId.value = "test-calendar-id"

        val eventsList = listOf(
            CalendarEventInfo(
                id = "event_123",
                summary = "Team Standup",
                startDateTime = DateTime("2025-01-15T10:00:00-05:00"),
                endDateTime = DateTime("2025-01-15T10:30:00-05:00"),
                isAllDay = false
            )
        )
        val events = eventsViewModel::class.java.getDeclaredField("_events").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<List<CalendarEventInfo>>
        events.value = eventsList

        composeTestRule.setContent {
            EventsScreen(
                loginViewModel = loginViewModel,
                eventsViewModel = eventsViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("Add event")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun eventsScreen_showsRefreshButton() {
        val userState = loginViewModel::class.java.getDeclaredField("_userState").apply {
            isAccessible = true
        }.get(loginViewModel) as MutableStateFlow<UserState>
        userState.value = UserState.READY

        loginViewModel_stateIsReadyInSetup()

        eventsViewModel = EventsViewModel(mockRepository)

        // Set a household calendar ID so buttons are enabled
        val calendarId = eventsViewModel::class.java.getDeclaredField("_householdCalendarId").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<String?>
        calendarId.value = "test-calendar-id"

        val eventsList = listOf(
            CalendarEventInfo(
                id = "event_123",
                summary = "Team Standup",
                startDateTime = DateTime("2025-01-15T10:00:00-05:00"),
                endDateTime = DateTime("2025-01-15T10:30:00-05:00"),
                isAllDay = false
            )
        )
        val events = eventsViewModel::class.java.getDeclaredField("_events").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<List<CalendarEventInfo>>
        events.value = eventsList

        composeTestRule.setContent {
            EventsScreen(
                loginViewModel = loginViewModel,
                eventsViewModel = eventsViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("Refresh")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun eventsScreen_addEventButtonDisabledWhenNoCalendarId() {
        // Set calendar ID to null
        val calendarId = eventsViewModel::class.java.getDeclaredField("_householdCalendarId").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<String?>
        calendarId.value = null

        composeTestRule.setContent {
            EventsScreen(
                loginViewModel = loginViewModel,
                eventsViewModel = eventsViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("Add event")
            .assertIsNotEnabled()
    }

    @Test
    fun eventsScreen_clickAddEventButton_opensDialog() {
        val userState = loginViewModel::class.java.getDeclaredField("_userState").apply {
            isAccessible = true
        }.get(loginViewModel) as MutableStateFlow<UserState>
        userState.value = UserState.READY

        loginViewModel_stateIsReadyInSetup()

        eventsViewModel = EventsViewModel(mockRepository)

        // Set a household calendar ID so buttons are enabled
        val calendarId = eventsViewModel::class.java.getDeclaredField("_householdCalendarId").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<String?>
        calendarId.value = "test-calendar-id"

        val eventsList = listOf(
            CalendarEventInfo(
                id = "event_123",
                summary = "Team Standup",
                startDateTime = DateTime("2025-01-15T10:00:00-05:00"),
                endDateTime = DateTime("2025-01-15T10:30:00-05:00"),
                isAllDay = false
            )
        )
        val events = eventsViewModel::class.java.getDeclaredField("_events").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<List<CalendarEventInfo>>
        events.value = eventsList

        composeTestRule.setContent {
            EventsScreen(
                loginViewModel = loginViewModel,
                eventsViewModel = eventsViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("Add event")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Add New Event")
            .assertIsDisplayed()
    }

    @Test
    fun eventsScreen_switchToAgendaView() {
        val userState = loginViewModel::class.java.getDeclaredField("_userState").apply {
            isAccessible = true
        }.get(loginViewModel) as MutableStateFlow<UserState>
        userState.value = UserState.READY

        loginViewModel_stateIsReadyInSetup()

        eventsViewModel = EventsViewModel(mockRepository)

        // Set a household calendar ID so buttons are enabled
        val calendarId = eventsViewModel::class.java.getDeclaredField("_householdCalendarId").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<String?>
        calendarId.value = "test-calendar-id"

        val eventsList = listOf(
            CalendarEventInfo(
                id = "event_123",
                summary = "Team Standup",
                startDateTime = DateTime("2025-01-15T10:00:00-05:00"),
                endDateTime = DateTime("2025-01-15T10:30:00-05:00"),
                isAllDay = false
            )
        )
        val events = eventsViewModel::class.java.getDeclaredField("_events").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<List<CalendarEventInfo>>
        events.value = eventsList

        composeTestRule.setContent {
            EventsScreen(
                loginViewModel = loginViewModel,
                eventsViewModel = eventsViewModel
            )
        }

        composeTestRule.onNodeWithText("Agenda")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify agenda view is shown
        composeTestRule.onNodeWithText("Household Calendar")
            .assertIsDisplayed()
    }

    @Test
    fun eventsScreen_switchToThreeDayView() {
        val userState = loginViewModel::class.java.getDeclaredField("_userState").apply {
            isAccessible = true
        }.get(loginViewModel) as MutableStateFlow<UserState>
        userState.value = UserState.READY
        loginViewModel_stateIsReadyInSetup()

        eventsViewModel = EventsViewModel(mockRepository)

        // Set a household calendar ID so buttons are enabled
        val calendarId = eventsViewModel::class.java.getDeclaredField("_householdCalendarId").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<String?>
        calendarId.value = "test-calendar-id"

        val eventsList = listOf(
            CalendarEventInfo(
                id = "event_123",
                summary = "Team Standup",
                startDateTime = DateTime("2025-01-15T10:00:00-05:00"),
                endDateTime = DateTime("2025-01-15T10:30:00-05:00"),
                isAllDay = false
            )
        )
        val events = eventsViewModel::class.java.getDeclaredField("_events").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<List<CalendarEventInfo>>
        events.value = eventsList

        composeTestRule.setContent {
            EventsScreen(
                loginViewModel = loginViewModel,
                eventsViewModel = eventsViewModel
            )
        }

        composeTestRule.onNodeWithText("3 Day")
            .performClick()

        composeTestRule.waitForIdle()

        // Should show navigation arrows
        composeTestRule.onNodeWithContentDescription("Previous Day")
            .assertExists()
        composeTestRule.onNodeWithContentDescription("Next Day")
            .assertExists()
    }

    @Test
    fun eventsScreen_switchToMonthView() {
        val userState = loginViewModel::class.java.getDeclaredField("_userState").apply {
            isAccessible = true
        }.get(loginViewModel) as MutableStateFlow<UserState>
        userState.value = UserState.READY
        loginViewModel_stateIsReadyInSetup()

        eventsViewModel = EventsViewModel(mockRepository)

        // Set a household calendar ID so buttons are enabled
        val calendarId = eventsViewModel::class.java.getDeclaredField("_householdCalendarId").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<String?>
        calendarId.value = "test-calendar-id"

        val eventsList = listOf(
            CalendarEventInfo(
                id = "event_123",
                summary = "Team Standup",
                startDateTime = DateTime("2025-01-15T10:00:00-05:00"),
                endDateTime = DateTime("2025-01-15T10:30:00-05:00"),
                isAllDay = false
            )
        )
        val events = eventsViewModel::class.java.getDeclaredField("_events").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<List<CalendarEventInfo>>
        events.value = eventsList

        composeTestRule.setContent {
            EventsScreen(
                loginViewModel = loginViewModel,
                eventsViewModel = eventsViewModel
            )
        }

        composeTestRule.onNodeWithText("Month")
            .performClick()

        composeTestRule.waitForIdle()

        // Month view should show weekday labels
        composeTestRule.onNodeWithText("S").assertExists()
        composeTestRule.onNodeWithText("M").assertExists()
        composeTestRule.onNodeWithText("T").assertExists()
    }

    @Test
    fun eventsScreen_showsNoEventsMessage_whenEmpty() {
        val userState = loginViewModel::class.java.getDeclaredField("_userState").apply {
            isAccessible = true
        }.get(loginViewModel) as MutableStateFlow<UserState>
        userState.value = UserState.READY
        loginViewModel_stateIsReadyInSetup()


        // Ensure events list is empty
        val events = eventsViewModel::class.java.getDeclaredField("_events").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<List<CalendarEventInfo>>
        events.value = emptyList()

        composeTestRule.setContent {
            EventsScreen(
                loginViewModel = loginViewModel,
                eventsViewModel = eventsViewModel
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("No upcoming events found.")
            .assertIsDisplayed()
    }

    @Test
    fun eventsScreen_displaysEvents_inAgendaView() {
        val userState = loginViewModel::class.java.getDeclaredField("_userState").apply {
            isAccessible = true
        }.get(loginViewModel) as MutableStateFlow<UserState>
        userState.value = UserState.READY
        loginViewModel_stateIsReadyInSetup()

        eventsViewModel = EventsViewModel(mockRepository)

        val testEvents = listOf(
            CalendarEventInfo(
                id = "1",
                summary = "Team Meeting",
                startDateTime = DateTime(System.currentTimeMillis() + 3600000), // 1 hour from now
                endDateTime = DateTime(System.currentTimeMillis() + 7200000), // 2 hours from now
                isAllDay = false
            ),
            CalendarEventInfo(
                id = "2",
                summary = "Lunch with Friends",
                startDateTime = DateTime(System.currentTimeMillis() + 14400000), // 4 hours from now
                endDateTime = DateTime(System.currentTimeMillis() + 18000000), // 5 hours from now
                isAllDay = false
            )
        )

        val events = eventsViewModel::class.java.getDeclaredField("_events").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<List<CalendarEventInfo>>
        events.value = testEvents

        composeTestRule.setContent {
            EventsScreen(
                loginViewModel = loginViewModel,
                eventsViewModel = eventsViewModel
            )
        }

        // Switch to Agenda view
        composeTestRule.onNodeWithText("Agenda")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Team Meeting")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Lunch with Friends")
            .assertIsDisplayed()
    }

    @Test
    fun addEventDialog_showsAllFields() {
        val userState = loginViewModel::class.java.getDeclaredField("_userState").apply {
            isAccessible = true
        }.get(loginViewModel) as MutableStateFlow<UserState>
        userState.value = UserState.READY
        loginViewModel_stateIsReadyInSetup()

        eventsViewModel = EventsViewModel(mockRepository)

        // Set a household calendar ID so buttons are enabled
        val calendarId = eventsViewModel::class.java.getDeclaredField("_householdCalendarId").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<String?>
        calendarId.value = "test-calendar-id"

        val eventsList = listOf(
            CalendarEventInfo(
                id = "event_123",
                summary = "Team Standup",
                startDateTime = DateTime("2025-01-15T10:00:00-05:00"),
                endDateTime = DateTime("2025-01-15T10:30:00-05:00"),
                isAllDay = false
            )
        )
        val events = eventsViewModel::class.java.getDeclaredField("_events").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<List<CalendarEventInfo>>
        events.value = eventsList


        composeTestRule.setContent {
            EventsScreen(
                loginViewModel = loginViewModel,
                eventsViewModel = eventsViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("Add event")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Description (Optional)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun addEventDialog_saveButtonDisabledInitially() {
        composeTestRule.setContent {
            EventsScreen(
                loginViewModel = loginViewModel,
                eventsViewModel = eventsViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("Add event")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Save")
            .assertIsNotEnabled()
    }

    @Test
    fun addEventDialog_cancelButtonDismissesDialog() {
        val userState = loginViewModel::class.java.getDeclaredField("_userState").apply {
            isAccessible = true
        }.get(loginViewModel) as MutableStateFlow<UserState>
        userState.value = UserState.READY
        loginViewModel_stateIsReadyInSetup()

        eventsViewModel = EventsViewModel(mockRepository)

        // Set a household calendar ID so buttons are enabled
        val calendarId = eventsViewModel::class.java.getDeclaredField("_householdCalendarId").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<String?>
        calendarId.value = "test-calendar-id"

        val eventsList = listOf(
            CalendarEventInfo(
                id = "event_123",
                summary = "Team Standup",
                startDateTime = DateTime("2025-01-15T10:00:00-05:00"),
                endDateTime = DateTime("2025-01-15T10:30:00-05:00"),
                isAllDay = false
            )
        )
        val events = eventsViewModel::class.java.getDeclaredField("_events").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<List<CalendarEventInfo>>
        events.value = eventsList

        composeTestRule.setContent {
            EventsScreen(
                loginViewModel = loginViewModel,
                eventsViewModel = eventsViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("Add event")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Cancel")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Add New Event")
            .assertDoesNotExist()
    }

    @Test
    fun addEventDialog_fillFormAndSubmit() {
        val userState = loginViewModel::class.java.getDeclaredField("_userState").apply {
            isAccessible = true
        }.get(loginViewModel) as MutableStateFlow<UserState>
        userState.value = UserState.READY
        loginViewModel_stateIsReadyInSetup()

        eventsViewModel = EventsViewModel(mockRepository)

        // Set a household calendar ID so buttons are enabled
        val calendarId = eventsViewModel::class.java.getDeclaredField("_householdCalendarId").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<String?>
        calendarId.value = "test-calendar-id"

        val eventsList = listOf(
            CalendarEventInfo(
                id = "event_123",
                summary = "Team Standup",
                startDateTime = DateTime("2025-01-15T10:00:00-05:00"),
                endDateTime = DateTime("2025-01-15T10:30:00-05:00"),
                isAllDay = false
            )
        )
        val events = eventsViewModel::class.java.getDeclaredField("_events").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<List<CalendarEventInfo>>
        events.value = eventsList

        composeTestRule.setContent {
            EventsScreen(
                loginViewModel = loginViewModel,
                eventsViewModel = eventsViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("Add event")
            .performClick()

        composeTestRule.waitForIdle()

        // Fill in title
        composeTestRule.onNodeWithText("Title")
            .performTextInput("New Event")

        composeTestRule.waitForIdle()

        // Fill in description
        composeTestRule.onNodeWithText("Description (Optional)")
            .performTextInput("This is a test event")

        composeTestRule.waitForIdle()

        // Save button should now be enabled
        composeTestRule.onNodeWithText("Save")
            .assertIsEnabled()

        composeTestRule.onNodeWithText("Save")
            .performClick()

        composeTestRule.waitForIdle()

        // Dialog should be dismissed
        composeTestRule.onNodeWithText("Add New Event")
            .assertDoesNotExist()
    }

    @Test
    fun threeDayView_navigationArrowsWork() {
        val userState = loginViewModel::class.java.getDeclaredField("_userState").apply {
            isAccessible = true
        }.get(loginViewModel) as MutableStateFlow<UserState>
        userState.value = UserState.READY
        loginViewModel_stateIsReadyInSetup()

        eventsViewModel = EventsViewModel(mockRepository)

        // Set a household calendar ID so buttons are enabled
        val calendarId = eventsViewModel::class.java.getDeclaredField("_householdCalendarId").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<String?>
        calendarId.value = "test-calendar-id"

        val eventsList = listOf(
            CalendarEventInfo(
                id = "event_123",
                summary = "Team Standup",
                startDateTime = DateTime("2025-01-15T10:00:00-05:00"),
                endDateTime = DateTime("2025-01-15T10:30:00-05:00"),
                isAllDay = false
            )
        )
        val events = eventsViewModel::class.java.getDeclaredField("_events").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<List<CalendarEventInfo>>
        events.value = eventsList

        composeTestRule.setContent {
            EventsScreen(
                loginViewModel = loginViewModel,
                eventsViewModel = eventsViewModel
            )
        }

        // Switch to 3 Day view
        composeTestRule.onNodeWithText("3 Day")
            .performClick()

        composeTestRule.waitForIdle()

        // Click next day
        composeTestRule.onNodeWithContentDescription("Next Day")
            .performClick()

        composeTestRule.waitForIdle()

        // Click previous day
        composeTestRule.onNodeWithContentDescription("Previous Day")
            .performClick()

        composeTestRule.waitForIdle()

        // Should still be in 3 day view
        composeTestRule.onNodeWithContentDescription("Previous Day")
            .assertExists()
    }

    @Test
    fun eventsScreen_showsErrorMessage_whenErrorOccurs() {
        val userState = loginViewModel::class.java.getDeclaredField("_userState").apply {
            isAccessible = true
        }.get(loginViewModel) as MutableStateFlow<UserState>
        userState.value = UserState.READY
        loginViewModel_stateIsReadyInSetup()

        eventsViewModel = EventsViewModel(mockRepository)

        // Set a household calendar ID so buttons are enabled
        val calendarId = eventsViewModel::class.java.getDeclaredField("_householdCalendarId").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<String?>
        calendarId.value = "test-calendar-id"

        val eventsList = listOf(
            CalendarEventInfo(
                id = "event_123",
                summary = "Team Standup",
                startDateTime = DateTime("2025-01-15T10:00:00-05:00"),
                endDateTime = DateTime("2025-01-15T10:30:00-05:00"),
                isAllDay = false
            )
        )
        val events = eventsViewModel::class.java.getDeclaredField("_events").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<List<CalendarEventInfo>>
        events.value = eventsList

        val error = eventsViewModel::class.java.getDeclaredField("_calendarError").apply {
            isAccessible = true
        }.get(eventsViewModel) as MutableStateFlow<String?>
        error.value = "Failed to load calendar"

        composeTestRule.setContent {
            EventsScreen(
                loginViewModel = loginViewModel,
                eventsViewModel = eventsViewModel
            )
        }

        composeTestRule.onNodeWithText("Error: Failed to load calendar")
            .assertIsDisplayed()
    }

//    @Test
//    fun eventsScreen_showsLoadingIndicator_whenLoading() {
//        val userState = loginViewModel::class.java.getDeclaredField("_userState").apply {
//            isAccessible = true
//        }.get(loginViewModel) as MutableStateFlow<UserState>
//        userState.value = UserState.READY
//
//        val isLoading = eventsViewModel::class.java.getDeclaredField("_isLoadingCalendar").apply {
//            isAccessible = true
//        }.get(eventsViewModel) as MutableStateFlow<Boolean>
//        isLoading.value = true
//
//        // Clear events to ensure loading indicator shows
//        val events = eventsViewModel::class.java.getDeclaredField("_events").apply {
//            isAccessible = true
//        }.get(eventsViewModel) as MutableStateFlow<List<CalendarEventInfo>>
//        events.value = emptyList()
//
//        composeTestRule.setContent {
//            EventsScreen(
//                loginViewModel = loginViewModel,
//                eventsViewModel = eventsViewModel
//            )
//        }
//
//        composeTestRule.waitForIdle()
//
//        // Check for CircularProgressIndicator (it doesn't have text, but we can verify it exists)
//        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
//            .assertExists()
//    }
}