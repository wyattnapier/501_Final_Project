package com.example.a501_final_project

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// custom data object created here for now
data class User(
    val username: String,
    val email: String,
    val venmoUsername: String,
)


/**
 * chore should include an id for easy maintainence
 * title and description for what the chore is
 * dueDate to keep track of when they are due
 * assignee to indicate who it is assigned to
 * completed to indicate if chore is completed
 */
data class Chore(
    val choreID: Int,
    val userID: Number,
    val householdID: Number,
    val name: String,
    val description: String,
//    var dueDate: Date, // trying this, can change later
    var dueDate: String,
    var assignedTo: String,
    var completed: Boolean,
    var priority: Boolean,
)

/**
 * payment class should include id for maintenance
 * payTo tracks who the payment should go to
 * payFrom tracks who is sending the payment
 * amount indicates the amount the payment is for
 * memo stores the memo message for the payment
 * paid indicates if payment has been paid
 * date ?
 */
data class Payment(
    val id: Int,
    val payTo: String, // data types may change depending on how we represent this?
    val payFrom: String,
    val amount: Double,
    val memo: String,
    var paid: Boolean,
    val recurring: Boolean
)

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

/** our ViewModel to hold our business logic and data.
 * For now this will be one ViewModel that all of the screens can access.
 * One this grows, we may break it down into ViewModels per screen,
 * but since Home view needs all this data we are doing one for now.
 **/

class MainViewModel : ViewModel() {

    // dummy household members....
    val roommates = listOf("Alice", "Wyatt", "Tiffany")
    val users = listOf(
        User("alice_username", "william.henry.harrison@example-pet-store.com", "alice_venmo"),
        User("tiffany_username", "william.henry.harrison@example-pet-store.com", "tiffany_venmo"),
        User("john_username", "william.henry.moody@my-own-personal-domain.com", "john_venmo")
    )

    // chores viewmodel portion
    private val _choresList = MutableStateFlow<List<Chore>>(listOf(
        Chore(
            choreID = 1,
            name = "Wash Dishes",
            description = "Clean all dishes, utensils, and pots used during dinner.",
            assignedTo = "Alice",
            householdID = 1,
            userID = 1,
            dueDate = "November 15, 2025",
            completed = true,
            priority = true
        ),
        Chore(
            choreID = 2,
            name = "Vacuum Living Room",
            description = "Vacuum the carpet and under the furniture in the living room.",
            assignedTo = "Bob",
            householdID = 1,
            userID = 2,
            dueDate = "November 15, 2025",
            completed = true,
            priority = true
        ),
        Chore(
            choreID = 3,
            name = "Laundry",
            description = "Wash, dry, and fold all the clothes from the laundry basket.",
            assignedTo = "Charlie",
            householdID = 1,
            userID = 3,
            dueDate = "November 15, 2025",
            completed = true,
            priority = true,
        ),
        Chore(
            choreID = 4,
            name = "Take Out Trash",
            description = "Empty all trash bins and take the garbage out to the curb.",
            assignedTo = "Dana",
            householdID = 1,
            userID = 4,
            dueDate = "November 15, 2025",
            completed = false,
            priority = true,
        ),
        Chore(
            choreID = 5,
            name = "Clean Bathroom",
            description = "Scrub the sink, toilet, and shower, and mop the bathroom floor.",
            assignedTo = "Eve",
            householdID = 1,
            userID = 5,
            dueDate = "November 15, 2025",
            completed = false,
            priority = true,
        ))
    )

    var choresList: StateFlow<List<Chore>> = _choresList.asStateFlow()
    private val _showPrevChores = MutableStateFlow(false)
    val showPrevChores: StateFlow<Boolean> = _showPrevChores.asStateFlow()

    // temporary values to keep track of who is current user and current household
    val userID = 2
    val currentUser = "Wyatt"
    val householdID = 1

    // functions to help us maintain chores
    /**
     * function to be used when first initializing/creating the list of chores for the household
     */
    fun addChores(newChore : Chore) {
        _choresList.value = _choresList.value + newChore
    }

    /**
     * function to assign chores to members of the household
     * eventually we should do this based on time?
     */
    fun assignChores() {
        _choresList.value = _choresList.value.mapIndexed { index, chore ->
            chore.copy(assignedTo = roommates[index % roommates.size])
        }
    }

    /**
     * function to mark a chore as completed
     * ideally when user completes their chore,
     * this function is called and gets passed the chore
     * this function might not be needed, but it could help with safe access/storing logic in one place
     */
    fun completeChore(completedChore: Chore) {
        _choresList.value = _choresList.value.map { chore ->
            if (chore.choreID == completedChore.choreID) chore.copy(completed = true) else chore
        }
    }
    /**
     * function to get the chore(s) assigned to the specified person
     */
    fun getChoresFor(person: String): List<Chore> {
        return _choresList.value.filter { it.assignedTo == person }
    }

    /**
     * function to change priority fo a chore.
     * currently making chores that are due sooner higher priority
     * Should call this function when we load in chores/onResume()?
     */
    fun changePriority(chore : Chore) {
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH) // e.g. "November 15, 2024"
        val today = Calendar.getInstance() // Use fully qualified name
        val due = Calendar.getInstance() // Use fully qualified name
        val dueDate: Date = dateFormat.parse(chore.dueDate) ?: return
        due.time = dueDate
        val diffMillis = due.timeInMillis - today.timeInMillis
        val daysUntilDue = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        chore.priority = daysUntilDue in 0..5
    }

    /**
     * function to toggle if we are showing previous chores or not
     */
    fun toggleShowPrevChores() {
        _showPrevChores.value = !_showPrevChores.value
    }


    /***********************************************************************************************************************************************************************/

    // pay viewmodel portion
    private val _paymentsList = MutableStateFlow<List<Payment>>(listOf(
        Payment(
            0,
            "tiffany_username",
            "alice_username",
            85.50,
            "Dinner",
            paid = false,
            recurring = false
        ),
        Payment(1, "alice_username", "Wyatt", 15.50, "Dinner", paid = false, recurring = false),
        Payment(2, "tiffany_username", "Wyatt", 25.00, "Utilities", paid = false, recurring = true),
        Payment(3, "john_username", "Wyatt", 100.25, "Rent", paid = false, recurring = false),
        Payment(4, "Wyatt", "john_username", 100.25, "Rent", paid = true, recurring = false),
        Payment(5, "john_username", "Wyatt", 100.25, "Rent", paid = true, recurring = false),
    )
    )

    // TODO: get this from the viewmodel instead of dummy data
    var paymentsList: StateFlow<List<Payment>> = _paymentsList.asStateFlow()

    // forcing a past payment, otherwise empty
    private val _pastPayments = MutableStateFlow<List<Payment>>(listOf(
        Payment(0, "Wyatt", "alice_username", 85.50, "Dinner", paid = true, recurring = false),
    ))
    val pastPayments: StateFlow<List<Payment>> = _pastPayments
    private val _showPastPayments = MutableStateFlow(false)
    val showPastPayments: StateFlow<Boolean> = _showPastPayments.asStateFlow()


    // functions to help us maintain payments

    /**
     * function to add payments to active payments
     * this function should be used more frequently than add chore since
     * chores are set at creating while payment is constantly changing (either recurring or one time)?
     *
     * hypothetically when someone inputs payment information, they can select who has to pay?
     * so the payFrom may be a list and we add some number of payments based on the calculation
     *
     * eventually we can determine weighting of payments for things like room size?
     */
//    fun addPayment(newPayment: Payment) {
//        paymentList.add(newPayment)
//    }
    fun addPayment(payTo: String, payFrom: List<String>, amount: Double, memo: String, recurring: Boolean) {
        val amountPerPerson = amount / payFrom.size
        for (name in payFrom) {
            val newPayment = Payment(
                id = paymentsList.value.size + 1,
                payTo = payTo,
                payFrom = name,
                amount = amountPerPerson,
                memo = memo,
                paid = false,
                recurring = recurring
            )
            _paymentsList.value = _paymentsList.value + newPayment
        }
    }

    /**
     * function to remove payment, either to get rid of it or upon completion
     */
    fun removePayment(payment: Payment) {
        _paymentsList.value = _paymentsList.value.filter { it != payment }
    }


    /**
     * function to mark a payment as paid
     */
    fun completePayment(payment: Payment) {
        // payment should already be in list so we can access
        val updatedPayment = payment.copy(paid = true)

        // Add to pastPayments
        _pastPayments.value = _pastPayments.value + updatedPayment

        // Remove from active paymentList
        _paymentsList.value = _paymentsList.value.filter { it != payment }
    }

    /**
     * function to get payments assigned to specified person
     */
    fun getPaymentsFor(person: String): List<Payment> {
        return paymentsList.value.filter { it.payTo == person }
    }

    /**
     * function to get payments from a specific person
     * this will be used for payments they need to make
     */
    fun getPaymentsFrom(person: String): List<Payment> {
        return paymentsList.value.filter { it.payFrom == person }
    }

    /**
     * function to get venmo user name of a user
     */
    fun getVenmoUsername(person: String): String? {
        val user: User? = users.find { it.username == person }
        return user?.venmoUsername // TODO: should return an error message if null
    }

    /**
     * function to toggle if past payments are shown
     */
    fun toggleShowPastPayments() {
        _showPastPayments.value = !_showPastPayments.value
    }

    /***********************************************************************************************************************************************************************/
    // --- CALENDAR SECTION ---
    // Store events as a map from Calendar Name -> List of Events
    private val _eventsByCalendar = MutableStateFlow<Map<String, List<CalendarEventInfo>>>(emptyMap())
    val eventsByCalendar = _eventsByCalendar.asStateFlow()

    // Store the set of expanded calendar names
    private val _expandedCalendarNames = MutableStateFlow<Set<String>>(emptySet())
    val expandedCalendarNames = _expandedCalendarNames.asStateFlow()

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

    fun setLeftDayForThreeDay(day: Calendar) {
        _leftDayForThreeDay.value = day
    }

    // change the view type
    fun setCalendarView(viewType: CalendarViewType) {
        _calendarViewType.value = viewType
        // TODO: re-fetch events if the date range changes significantly
    }

    fun toggleCalendarSection(calendarName: String) {
        val current = _expandedCalendarNames.value
        _expandedCalendarNames.value = if (calendarName in current) {
            current - calendarName
        } else {
            current + calendarName
        }
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


    // TODO: add a date picker to input start date and end date of date range
    fun fetchCalendarEvents(
        context: Context,
        days: Int = numCalendarDataDays,
        calendarFilterName: String? = "Other Events" // filter to only get one calendar if not null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // Get the last signed-in Google account using the provided context.
            val googleAccount = GoogleSignIn.getLastSignedInAccount(context)

            // If account is null, the user is not signed in. Post an error and stop.
            if (googleAccount == null) {
                _calendarError.value = "Cannot refresh events: User is not signed in."
                Log.e("MainViewModel", "fetchCalendarEvents failed: GoogleSignInAccount is null.")
                return@launch
            }

            _isLoadingCalendar.value = true
            _calendarError.value = null
            try {
                // 1. Create a credential using the signed-in account
                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf(
                        CalendarScopes.CALENDAR_READONLY,
                    )
                ).apply {
                    selectedAccount = googleAccount.account
                }

                // 2. Build the Calendar service
                val calendarService = com.google.api.services.calendar.Calendar.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                )
                    .setApplicationName("501_Final_Project")
                    .build()

                // 3. Get the list of all calendars
                val calendarList = calendarService.calendarList().list().execute()
                val eventsMap = mutableMapOf<String, List<CalendarEventInfo>>()

                // set time range for fetching events
                val now = DateTime(System.currentTimeMillis())
                val timeMax = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, days)
                }.timeInMillis
                val maxDateTime = DateTime(timeMax)


                // 4. Iterate over each calendar to fetch its events
                for (calendarListEntry in calendarList.items) {
                    val calendarName = calendarListEntry.summary ?: "Unknown Calendar"
                    Log.d("MainViewModel", "Fetching events for calendar: $calendarName")

                    // if we're filtering by name then only fetch that calendar
                    // otherwise fetch all calendars
                    if (calendarFilterName != null && calendarName != calendarFilterName) {
                        continue
                    }

                    val eventsResult = calendarService.events().list(calendarListEntry.id)
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
                                } else {
                                    null // Skip timed events with invalid dates
                                }
                            }
                        }


                    if (items.isNotEmpty()) {
                        eventsMap[calendarName] = items
                    }
                }

                // 5. Update the UI state with the map of events
                _eventsByCalendar.value = eventsMap

            } catch (e: Exception) {
                Log.e("MainViewModel", "Calendar API error", e)
                _calendarError.value = "Failed to fetch events: ${e.message}"
            } finally {
                _isLoadingCalendar.value = false
            }
        }
    }
}