package com.example.a501_final_project

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color


// temp state variables to mark out which step of log in process we are in
// TODO: this should go in ViewModel? either loginviewmodel or ignup viewmodel?
enum class SignUpSteps {
    GOOGLE_LOGIN,
    USER_INFO,
    REVIEW
}
@Composable
fun SignUpScreen(
    loginViewModel: LoginViewModel
) {
    var currentStep by rememberSaveable { mutableStateOf(SignUpSteps.GOOGLE_LOGIN) }
    when (currentStep) {
        // handle each different step[
        SignUpSteps.GOOGLE_LOGIN -> {
            SignUpGoogle(loginViewModel, onSuccess = {
                currentStep = SignUpSteps.USER_INFO // update to go to next step
            })
        }
        SignUpSteps.USER_INFO -> {
            GetUserInfo()
        }

        SignUpSteps.REVIEW -> {

        }

    }

}

// composable for google login page
@Composable
fun SignUpGoogle(loginViewModel: LoginViewModel, onSuccess: ()-> Unit) {
    val uiState by loginViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Pass the result directly to the ViewModel to handle the logic.
        loginViewModel.handleSignInResult(result)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        if (uiState.isLoggedIn) {
            // if signed in to google successfully, move to next step of the sign up process
            LaunchedEffect(Unit) { onSuccess() } // on success does what was defined for it in the parent composable
        } else if (uiState.isLoginInProgress) {
            CircularProgressIndicator()
        } else {
            // --- SIGNED-OUT VIEW ---
            Button(
                onClick = {
                    val client = loginViewModel.getGoogleSignInClient(context)
                    signInLauncher.launch(client.signInIntent)
                }
            ) {
                Text(text = "Sign up with Google")
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

// composable for entering other user info

@Preview(
    showBackground=true
)
@Composable
fun GetUserInfo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
            .padding(top=32.dp, start=16.dp, end=16.dp)
    ) {
        Text(text = "Tell us more about yourself!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom=16.dp)
        )

        Text(text="What should we call you?",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(8.dp))
        TextField(
            value = "",
            onValueChange = {},
            label = { Text("Name") },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.padding(bottom=32.dp),
            colors = TextFieldDefaults.colors( unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
        )

        Text(text="What's is your venmo username?",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(8.dp))
        TextField(
            value = "",
            onValueChange = {},
            label = { Text("Venmo") },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.padding(bottom=32.dp),
            colors = TextFieldDefaults.colors( unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
        )
        Button(onClick = {}) {
            Text(text = "Next")
        }
    }
}