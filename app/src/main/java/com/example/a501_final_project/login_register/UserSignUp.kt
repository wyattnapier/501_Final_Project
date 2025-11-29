package com.example.a501_final_project.login_register

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController


// temp state variables to mark out which step of log in process we are in
// TODO: this should go in ViewModel? either loginviewmodel or ignup viewmodel?
enum class SignUpSteps {
    GOOGLE_LOGIN,
    USER_INFO,
    REVIEW
}
@Composable
fun SignUpScreen(
    loginViewModel: LoginViewModel,
    navController: NavController,
    onNavigateToLogin : () -> Unit
) {
    var currentStep by rememberSaveable { mutableStateOf(SignUpSteps.GOOGLE_LOGIN) }
    val snackbarHostState = remember { SnackbarHostState() }
    // scaffold so we can have snackbar message
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (currentStep) {
            // handle each different step[
            SignUpSteps.GOOGLE_LOGIN -> {
                val context = LocalContext.current
                val uiState by loginViewModel.uiState.collectAsState()

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    loginViewModel.handleSignInResult(result)
                }

                // launch Google Sign-In automatically
                // essentially the logic of prev screen just here so its direct
                LaunchedEffect(Unit) {
                    val client = loginViewModel.getGoogleSignInClient(context)
                    launcher.launch(client.signInIntent)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when {
                        uiState.isLoginInProgress -> CircularProgressIndicator()

                        uiState.isLoggedIn && uiState.userAlreadyExists == true -> {
                            LaunchedEffect(Unit) {
                                snackbarHostState.showSnackbar(
                                    message = "Account already exists. Please login",
                                    withDismissAction = true
                                )
                                loginViewModel.signOut(context)
                                onNavigateToLogin()
                            }
                        }

                        uiState.isLoggedIn && uiState.userAlreadyExists == false -> {
                            LaunchedEffect(Unit) {
                                currentStep = SignUpSteps.USER_INFO
                            }
                        }

                        uiState.error != null -> Text(uiState.error!!, color = Color.Red)
                    }
                }
            }

            SignUpSteps.USER_INFO -> {
                GetUserInfo(onNext = { username, name, venmoUsername ->
                    loginViewModel.username = username
                    loginViewModel.displayName = name
                    loginViewModel.venmoUsername = venmoUsername
                    currentStep = SignUpSteps.REVIEW
                })
            }

            SignUpSteps.REVIEW -> {
                ReviewInfo(loginViewModel, navController)
            }

        }
    }

}

// composable for entering other user info

//@Preview(
//    showBackground=true
//)
@Composable
fun GetUserInfo(onNext : (username: String, name: String, venmoUsername: String) -> Unit) {

    // temp values, will eventually do with view model
    var name by rememberSaveable { mutableStateOf("")}
    var venmoUsername by rememberSaveable { mutableStateOf("")}
    var username by rememberSaveable { mutableStateOf("")}

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(text = "Tell us more about yourself!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom=16.dp)
        )

        Text(text="Choose a username",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(8.dp))
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.padding(bottom=32.dp),
            colors = TextFieldDefaults.colors( unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
        )

        Text(text="What should we call you?",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(8.dp))
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.padding(bottom=32.dp),
            colors = TextFieldDefaults.colors( unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
        )

        Text(text="What's is your venmo username?",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(8.dp))
        TextField(
            value = venmoUsername,
            onValueChange = { venmoUsername = it },
            label = { Text("Venmo") },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.padding(bottom=32.dp),
            colors = TextFieldDefaults.colors( unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
        )
        // TODO: this button should route to household set up (or review info)?
        Button(onClick = {
            onNext(username, name, venmoUsername)
        }) {
            Text(text = "Next")
        }
    }
}

@Composable
fun ReviewInfo(loginViewModel: LoginViewModel, navController : NavController) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(text = "Review your information",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom=16.dp)
        )

        // Editable name
        OutlinedTextField(
            value = loginViewModel.username,
            onValueChange = { loginViewModel.username = it },
            label = { Text("Username") },
            modifier = Modifier.padding(bottom=12.dp)
        )
        OutlinedTextField(
            value = loginViewModel.displayName,
            onValueChange = { loginViewModel.displayName = it },
            label = { Text("Name") },
            modifier = Modifier.padding(bottom=12.dp)
        )
        OutlinedTextField(
            value = loginViewModel.venmoUsername,
            onValueChange = { loginViewModel.venmoUsername = it },
            label = { Text("Venmo username") },
            modifier = Modifier.padding(bottom=24.dp)
        )

        Text(text = "Looks good?",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(8.dp))
        Button(onClick = {
            loginViewModel.saveUserToDb()
            navController.navigate("HouseholdSetup")
        }) {
            Text("Join a household!")
        }
    }
}