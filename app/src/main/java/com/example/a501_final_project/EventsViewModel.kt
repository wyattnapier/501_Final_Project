package com.example.a501_final_project

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header

// -----------------------------
// DATA MODELS (Google Calendar)
// -----------------------------
data class CalendarEventsResponse(
    val items: List<CalendarEvent>?
)

data class CalendarEvent(
    val id: String,
    val summary: String?,
    val start: EventTime?,
    val end: EventTime?
)

data class EventTime(
    val dateTime: String?
)

// -----------------------------
// RETROFIT API INTERFACE
// -----------------------------
interface CalendarApi {
    @GET("calendar/v3/calendars/primary/events")
    suspend fun getEvents(
        @Header("Authorization") authHeader: String
    ): CalendarEventsResponse
}

// -----------------------------
// RETROFIT INSTANCE
// -----------------------------
private val retrofit = Retrofit.Builder()
    .baseUrl("https://www.googleapis.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

private val calendarApi = retrofit.create(CalendarApi::class.java)

// -----------------------------
// EVENTS VIEWMODEL
// -----------------------------
class EventsViewModel : ViewModel() {

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events = _events.asStateFlow()

    fun loadCalendarEvents(accessToken: String) {
        viewModelScope.launch {
            try {
                Log.d("EventsViewModel", "Fetching events with token: $accessToken")

                val response = calendarApi.getEvents("Bearer $accessToken")

                val items = response.items ?: emptyList()

                _events.value = items

                Log.d("EventsViewModel", "Loaded ${items.size} events")

            } catch (e: Exception) {
                Log.e("EventsViewModel", "Error loading events", e)
            }
        }
    }
}