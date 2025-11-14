package com.example.a501_final_project

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.a501_final_project.LoginViewModel
import com.example.a501_final_project.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn

@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    loginViewModel: LoginViewModel,
    mainViewModel: MainViewModel
) {
    val loginState by loginViewModel.uiState.collectAsState()
    val events by mainViewModel.events.collectAsState()
    val isLoading by mainViewModel.isLoadingCalendar.collectAsState()
    val error by mainViewModel.calendarError.collectAsState()
    val context = LocalContext.current

    // Fetch events when the user is logged in
    LaunchedEffect(loginState.isLoggedIn) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (loginState.isLoggedIn && account != null) {
            mainViewModel.fetchCalendarEvents(account, context)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            !loginState.isLoggedIn -> Text("Please sign in to see events.")
            isLoading -> CircularProgressIndicator()
            error != null -> Log.d("EventsScreen", "Error: $error")
            events.isEmpty() -> Text("No upcoming events found.")
            else -> {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(events.size) { index ->
                        val event = events[index]
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = event.summary ?: "(No Title)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("Start: ${event.start}")
                            Text("End: ${event.end}")
                        }
                    }
                }
            }
        }
    }
}
