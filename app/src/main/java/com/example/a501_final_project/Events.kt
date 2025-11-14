package com.example.a501_final_project

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    loginViewModel: LoginViewModel = viewModel(),
    eventsViewModel: EventsViewModel = viewModel(),
) {
    val accessToken = loginViewModel.uiState.collectAsState().value.googleAccessToken

    LaunchedEffect(accessToken) {
        if (accessToken != null) {
            eventsViewModel.loadCalendarEvents(accessToken)
        }
    }

    val events by eventsViewModel.events.collectAsState()
    Log.d("EventsScreen", "Events: $events")
    Log.d("EventsScreen", "Access Token: $accessToken")
    Log.d("EventsScreen", "EventsViewModel: $eventsViewModel")
    Log.d("EventsScreen", "LoginViewModel: $loginViewModel")
    Log.d("EventsScreen", "Modifier: $modifier")
    Log.d("EventsScreen", "LoginViewModel UI State: ${loginViewModel.uiState.collectAsState().value}")
    Log.d("EventsScreen", "EventsViewModel Events: ${eventsViewModel.events.collectAsState().value}")
    Log.d("EventsScreen", "events.isEmpty(): ${events.isEmpty()}")

    if (events.isEmpty()) {
        Text("No events found")
    } else {
        LazyColumn {
            items(events.size) { index ->
                val event = events[index]
                Column(modifier.padding(16.dp)) {
                    Text(
                        text = event.summary ?: "(No Title)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Start: ${event.start?.dateTime}")
                    Text("End: ${event.end?.dateTime}")
                }
            }
        }
    }
}
