package com.example.a501_final_project.login_register

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
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
    // hoisted state so it is still there on back
    var name by rememberSaveable { mutableStateOf("") }
    var venmoUsername by rememberSaveable { mutableStateOf("") }

    // scaffold so we can have snackbar message
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (currentStep) {
            // handle each different step[
            SignUpSteps.GOOGLE_LOGIN -> {
                val context = LocalContext.current
                val userState by loginViewModel.userState.collectAsState()
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

                        userState == UserState.NEEDS_SETUP -> {
                            LaunchedEffect(Unit) {
                                currentStep = SignUpSteps.USER_INFO
                            }
                        }

                        uiState.error != null -> Text(uiState.error!!, color = Color.Red)
                    }
                }
            }

            SignUpSteps.USER_INFO -> {
                GetUserInfo(
                    name = name,
                    onNameChange = { name = it },
                    venmoUsername = venmoUsername,
                    onVenmoUsernameChange = { venmoUsername = it },
                    onNext = {
                        // When "Next" is clicked, update the ViewModel before switching screens
                        loginViewModel.displayName = name
                        loginViewModel.venmoUsername = venmoUsername
                        currentStep = SignUpSteps.REVIEW
                    }
                )
            }

            SignUpSteps.REVIEW -> {
                ReviewInfo(
                    loginViewModel = loginViewModel,
                    navController = navController,
                    onBack = { currentStep = SignUpSteps.USER_INFO }
                )
            }
        }
    }

}

// composable for entering other user info
@Composable
fun GetUserInfo(
    name: String,
    onNameChange: (String) -> Unit,
    venmoUsername: String,
    onVenmoUsernameChange: (String) -> Unit,
    onNext : () -> Unit
) {
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    val isNameError = hasAttemptedSubmit && !isInputStringValidLength(name)
    val isVenmoError = hasAttemptedSubmit && !isInputStringValidLength(venmoUsername, 5, 30)

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
                    onValueChange = onNameChange,
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
                        text = "Name must be between 1 and 25 characters",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                } else {
                    // Add a spacer to maintain layout consistency
                    Spacer(modifier = Modifier.height(24.dp))
                }
                OutlinedTextField(
                    value = venmoUsername,
                    onValueChange = onVenmoUsernameChange,
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
                        text = "Venmo username must be between 5 and 30 characters", // aligns with real venmo criterion
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
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
                if (isInputStringValidLength(name) && isInputStringValidLength(venmoUsername)) {
                    Log.d("GetUserInfo", "no errors --> name: $name, venmo: $venmoUsername")
                    onNext() // Call the simplified onNext lambda
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Next", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ReviewInfo(
    loginViewModel: LoginViewModel,
    navController : NavController,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
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
                verticalArrangement = Arrangement.spacedBy(24.dp) // Increased spacing
            ) {
                // Display Name as read-only text
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Name Icon",
                        modifier = Modifier.padding(end = 16.dp), // Increased padding
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text("Name", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        Text(loginViewModel.displayName, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                // Display Venmo as read-only text
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Venmo Icon",
                        modifier = Modifier.padding(end = 16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text("Venmo Username", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        Text(loginViewModel.venmoUsername, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // Pushes the buttons to the bottom

        Text(
            text = "Ready to go?",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Primary Action: Create Household
        Button(
            onClick = {
                loginViewModel.saveUserToDb()
                navController.navigate("HouseholdSetup/create")
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
        Button(
            onClick = {
                loginViewModel.saveUserToDb()
                navController.navigate("HouseholdSetup/join")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Join Existing Household", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text("Go Back & Edit", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(20.dp)) // Some padding at the bottom
    }
}

fun isInputStringValidLength(
    input: String,
    minLength: Int = 1, // default to nonempty strings for names
    maxLength: Int = 25 // default to 25 for most names and longer for descriptions
): Boolean {
    return input.length in minLength .. maxLength
}