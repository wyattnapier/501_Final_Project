package com.example.a501_final_project

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel() // Get the ViewModel instance
) {
    // Observe the UI state from the ViewModel.
    // The UI will automatically recompose whenever the state changes.
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Pass the result directly to the ViewModel to handle the logic.
        viewModel.handleSignInResult(result)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize()
    ) {
        if (uiState.isLoginInProgress) {
            CircularProgressIndicator()
        } else if (uiState.userEmail != null) {
            // --- SIGNED-IN VIEW ---
            Text(
                text = "Logged in as ${uiState.userEmail}",
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Button(onClick = { viewModel.signOut(context) }) {
                Text(text = "Logout")
            }
        } else {
            // --- SIGNED-OUT VIEW ---
            Button(
                onClick = {
                    val client = viewModel.getGoogleSignInClient(context)
                    signInLauncher.launch(client.signInIntent)
                }
            ) {
                Text(text = "Login")
            }
        }

        // Optionally display errors
        uiState.error?.let {
            Text(
                text = it,
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
