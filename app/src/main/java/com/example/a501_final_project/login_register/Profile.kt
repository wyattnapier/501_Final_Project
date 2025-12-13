package com.example.a501_final_project.login_register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import coil.compose.AsyncImage
import com.example.a501_final_project.MainViewModel
import com.example.a501_final_project.R
import com.example.a501_final_project.Screen
import com.example.a501_final_project.chores.ChoresViewModel
import com.example.a501_final_project.events.EventsViewModel
import com.example.a501_final_project.navigateToScreen
import com.example.a501_final_project.payment.PaymentViewModel

@Composable
fun ProfileScreen(
    modifier: Modifier,
    mainViewModel: MainViewModel,
    eventsViewModel: EventsViewModel,
    choresViewModel: ChoresViewModel,
    paymentViewModel: PaymentViewModel,
    householdViewModel: HouseholdViewModel,
    loginViewModel: LoginViewModel,
    navController: NavController
) {
    val userStateVal by loginViewModel.userState.collectAsState()
    val uiState by loginViewModel.uiState.collectAsState()
    val householdID by mainViewModel.householdId.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (userStateVal == UserState.READY) {
            val verticalSpacingBetweenInformation = 8.dp // TODO: is there a better way to do this?
            // Profile picture
            AsyncImage(
                model = uiState.profilePictureUrl,
                contentDescription = "User Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                placeholder = painterResource(id = R.drawable.baseline_person_24),
                error = painterResource(id = R.drawable.baseline_person_24) // handles if profile picture url is null
            )
            // null safe displaying/handling of uiState (user) data
            uiState.userName?.let { Text(it, modifier = Modifier.padding(top = verticalSpacingBetweenInformation)) }
            // household id
            householdID?.let { id ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = verticalSpacingBetweenInformation)
                ) {
                    Text("Household id: $id")
                    // make it so you can copy the id
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(id))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.content_copy_24px),
                            contentDescription = "Copy Household ID"
                        )
                    }
                }
            }
            // settings
            Button(
                onClick = { navigateToScreen(navController, Screen.Settings) },
                modifier = Modifier.padding(top = verticalSpacingBetweenInformation)
            ) {
                Text("User Settings")
            }
            // TODO: add button to go to household settings here
            // sign out
            Button(
                onClick = {
                    loginViewModel.fullSignOut(
                        context,
                        mainViewModel,
                        eventsViewModel,
                        choresViewModel,
                        paymentViewModel,
                        householdViewModel
                    )
                }
            ) {
                Text("Sign Out")
            }
        } else {
            // doesn't navigate to login screen
            // stays in profile screen so after logging back in your see your details
            LoginScreen(modifier, loginViewModel, navController)
        }
    }
}