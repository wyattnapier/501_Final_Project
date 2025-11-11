package com.example.a501_final_project

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


val Context.dataStore by preferencesDataStore(name = "settings")

object WidgetPrefs {
    val SHOW_PAYMENTS = booleanPreferencesKey("show_payments")
    val SHOW_CHORES = booleanPreferencesKey("show_chores")
    val SHOW_EVENTS = booleanPreferencesKey("show_calendar")
}

class UserPreferences(private val context: Context) {

    val showPayments: Flow<Boolean> = context.dataStore.data.map {
        it[WidgetPrefs.SHOW_PAYMENTS] ?: true
    }

    val showChores: Flow<Boolean> = context.dataStore.data.map {
        it[WidgetPrefs.SHOW_CHORES] ?: true
    }

    val showEvents: Flow<Boolean> = context.dataStore.data.map {
        it[WidgetPrefs.SHOW_EVENTS] ?: true
    }

    suspend fun setShowPayments(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[WidgetPrefs.SHOW_PAYMENTS] = enabled
        }
    }

    suspend fun setShowChores(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[WidgetPrefs.SHOW_CHORES] = enabled
        }
    }

    suspend fun setShowEvents(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[WidgetPrefs.SHOW_EVENTS] = enabled
        }
    }
}

@Composable
fun UserPrefScreen(modifier: Modifier = Modifier){
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }

    val showPayments by prefs.showPayments.collectAsState(initial = true)
    val showChores by prefs.showChores.collectAsState(initial = true)
    val showEvents by prefs.showEvents.collectAsState(initial = true)

    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = {
            scope.launch { prefs.setShowPayments(!showPayments) }
        }) {
            Text(if (showPayments) "Hide Payments" else "Show Payments")
        }

        Button(onClick = {
            scope.launch { prefs.setShowChores(!showChores) }
        }) {
            Text(if (showChores) "Hide Chores" else "Show Chores")
        }

        Button(onClick = {
            scope.launch { prefs.setShowEvents(!showEvents) }
        }) {
            Text(if (showEvents) "Hide Events" else "Show Events")
        }
    }
}

