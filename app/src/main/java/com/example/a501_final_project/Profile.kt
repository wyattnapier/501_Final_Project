package com.example.a501_final_project

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun ProfileScreen(modifier: Modifier, loginViewModel: LoginViewModel = viewModel(), navController: NavController) {
    val uiState by loginViewModel.uiState.collectAsState()
    val context = LocalContext.current
    Log.d("ProfileScreen", "ProfileScreen recomposing with uiState: $uiState")
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (uiState.isLoggedIn) {
            val verticalSpacingBetweenInformation = 8.dp
            Icon(Icons.Default.Person, "User Profile", Modifier.size(50.dp)) // TODO: add actual profile picture
            uiState.userName?.let { Text(it, modifier = Modifier.padding(top = verticalSpacingBetweenInformation)) }
            uiState.userEmail?.let { Text(it, modifier = Modifier.padding(top = verticalSpacingBetweenInformation)) }
            Button(onClick = { navigateToScreen(navController, Screen.Settings)}, modifier = Modifier.padding(top = verticalSpacingBetweenInformation)) {
                Text("User Settings")
            }
            Button(onClick = { loginViewModel.signOut(context) }, modifier = Modifier.padding(top = verticalSpacingBetweenInformation)) {
                Text("Sign Out")
            }
        } else {
            LoginScreen(modifier, loginViewModel)
        }
    }
}