package com.example.a501_final_project.login_register

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(),
    navController: NavController,
) {
    // Observe the UI state from the ViewModel.
    // The UI will automatically recompose whenever the state changes.
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // at this point firebase is done checking and the user is not logged in, so continue with the regular sign in process
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
        // todo: check if isLoggedIn will always be false when this is called
        if (uiState.isLoginInProgress) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    val client = viewModel.getGoogleSignInClient(context)
                    signInLauncher.launch(client.signInIntent)
                }
            ) {
                Text(text = "Login")
            }
            Text(text="OR",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
            Button(onClick={
                navController.navigate("UserSignUp")
            }) {
                Text(text="Sign up")
            }
        }
        LaunchedEffect(uiState.error) {
            uiState.error?.let { errorMessage ->
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                viewModel.clearError() // Optionally, clear the error after showing it
            }
        }
    }
}

// composable for on app opening (while firebase does all the status checks)
@Composable
fun AptLoading() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "apt.",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(64.dp))
        CircularProgressIndicator()
    }
}