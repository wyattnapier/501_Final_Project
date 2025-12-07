package com.example.a501_final_project.login_register

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.example.a501_final_project.ui.theme._501_Final_ProjectTheme

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
                    modifier = Modifier
                        .fillMaxSize()
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
                GetUserInfo(onNext = {name, venmoUsername ->
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
@Composable
fun GetUserInfo(onNext : (name: String, venmoUsername: String) -> Unit) {

    var name by rememberSaveable { mutableStateOf("")}
    var venmoUsername by rememberSaveable { mutableStateOf("")}
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    val isNameError = hasAttemptedSubmit && name.isBlank()
    val isVenmoError = hasAttemptedSubmit && venmoUsername.isBlank()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 32.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(text = "Tell Us About Yourself",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isNameError,
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = "Name")
                    }
                )
                if (isNameError) {
                    Text(
                        text = "Name cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                } else {
                    // Add a spacer to maintain layout consistency
                    Spacer(modifier = Modifier.height(24.dp))
                }
                OutlinedTextField(
                    value = venmoUsername,
                    onValueChange = { venmoUsername = it },
                    label = { Text("Venmo Username") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isVenmoError,
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Venmo")
                    }
                )
                if (isVenmoError) {
                    Text(
                        text = "Venmo username cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                } else {
                    // Add a spacer to maintain layout consistency
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // Pushes the button to the bottom

        Button(
            onClick = {
                hasAttemptedSubmit = true
                if (name.isNotBlank() && venmoUsername.isNotBlank()) {
                    Log.d("GetUserInfo", "no errors --> name: $name, venmo: $venmoUsername")
                    onNext(name, venmoUsername)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Next", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(20.dp)) // Some padding at the bottom
    }
}

@Composable
fun ReviewInfo(loginViewModel: LoginViewModel, navController : NavController) {
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    val isNameError = hasAttemptedSubmit && loginViewModel.displayName.isBlank()
    val isVenmoError = hasAttemptedSubmit && loginViewModel.venmoUsername.isBlank()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 32.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = "Review Your Info",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = loginViewModel.displayName,
                    onValueChange = { loginViewModel.displayName = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isNameError,
                    singleLine = true,
                    leadingIcon = { // Add an icon for Name
                        Icon(Icons.Default.Person, contentDescription = "Name")
                    }
                )
                if (isNameError) {
                    Text(
                        text = "Name cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }
                OutlinedTextField(
                    value = loginViewModel.venmoUsername,
                    onValueChange = { loginViewModel.venmoUsername = it },
                    label = { Text("Venmo username") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isVenmoError,
                    singleLine = true,
                    leadingIcon = { // Add an icon for Venmo
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Venmo"
                        ) // TODO: make credit card
                    }
                )
                if (isVenmoError) {
                    Text(
                        text = "Venmo username cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // Pushes the buttons to the bottom

        Text(
            text = "Ready to go?",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Primary Action: Create Household
        Button(
            onClick = {
                hasAttemptedSubmit = true
                if (loginViewModel.displayName.isNotBlank() && loginViewModel.venmoUsername.isNotBlank()) {
                    loginViewModel.saveUserToDb()
                    navController.navigate("HouseholdSetup/create")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp), // A slightly larger button
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Create New Household", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Secondary Action: Join Household
        OutlinedButton( // Use an OutlinedButton for secondary actions
            onClick = {
                hasAttemptedSubmit = true
                if (loginViewModel.displayName.isNotBlank() && loginViewModel.venmoUsername.isNotBlank()) {
                    loginViewModel.saveUserToDb()
                    navController.navigate("HouseholdSetup/join")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Join Existing Household", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(20.dp)) // Some padding at the bottom
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GetUserInfoPreview() {
    _501_Final_ProjectTheme {
        GetUserInfo(onNext = { _, _ -> })
    }
}