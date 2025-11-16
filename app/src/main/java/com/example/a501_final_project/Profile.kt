package com.example.a501_final_project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage

@Composable
fun ProfileScreen(modifier: Modifier, loginViewModel: LoginViewModel = viewModel(), navController: NavController) {
    val uiState by loginViewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (uiState.isLoggedIn) {
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
            uiState.userEmail?.let { Text(it, modifier = Modifier.padding(top = verticalSpacingBetweenInformation)) }
            // settings
            Button(
                onClick = { navigateToScreen(navController, Screen.Settings)},
                modifier = Modifier.padding(top = verticalSpacingBetweenInformation)
            ) {
                Text("User Settings")
            }
            // TODO: add button to go to household settings here
            // sign out
            Button(
                onClick = { loginViewModel.signOut(context) },
                modifier = Modifier.padding(top = verticalSpacingBetweenInformation)
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