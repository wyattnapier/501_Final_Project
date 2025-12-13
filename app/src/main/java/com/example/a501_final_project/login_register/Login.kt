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
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("LoginScreen", "Sign-in result received")
        viewModel.handleSignInResult(result)
    }

    // Log when recomposing
    Log.d("LoginScreen", "Recomposing - isLoginInProgress: ${uiState.isLoginInProgress}, isChecking: ${uiState.isChecking}")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize()
    ) {
        when {
            // Show loading indicator when login is in progress OR checking auth status
            uiState.isLoginInProgress || uiState.isChecking -> {
                Log.d("LoginScreen", "Showing loading indicator")
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (uiState.isLoginInProgress) "Signing in..." else "Checking authentication...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Show login buttons when not loading
            else -> {
                Log.d("LoginScreen", "Showing login buttons")
                Button(
                    onClick = {
                        Log.d("LoginScreen", "Login button clicked")
                        val client = viewModel.getGoogleSignInClient(context)
                        signInLauncher.launch(client.signInIntent)
                    }
                ) {
                    Text(text = "Login")
                }

                Text(
                    text = "OR",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )

                Button(
                    onClick = {
                        Log.d("LoginScreen", "Sign up button clicked")
                        navController.navigate("UserSignUp")
                    }
                ) {
                    Text(text = "Sign up")
                }
            }
        }

        // Show error toast if there's an error
        LaunchedEffect(uiState.error) {
            uiState.error?.let { errorMessage ->
                Log.e("LoginScreen", "Showing error: $errorMessage")
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                viewModel.clearError()
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