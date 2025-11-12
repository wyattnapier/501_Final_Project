package com.example.a501_final_project

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {

    // creating a shred viewModel for all screens
    val mainViewModel: MainViewModel = viewModel() // lifecycle-aware
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // Home
        composable(Screen.Home.route) {
            HomeScreen(modifier, navController)
        }
        // Chores
        composable(Screen.Chores.route) {
            // TODO: add onclick?
            ChoresScreen(mainViewModel, modifier = modifier)
        }
        // Pay
        composable(Screen.Pay.route) {
            VenmoPaymentScreen(modifier = modifier, mainViewModel) // do I need to pass the viewmodel into it?
            // TODO: add onclick?
        }
        // Calendar
        composable(Screen.Calendar.route) {
            GenericScreen(
                title = Screen.Calendar.title,
                modifier = modifier
            )
            // TODO: add onclick?
        }
        // Profile
        composable(Screen.Profile.route) {
            UserPrefScreen(modifier = modifier)
        }
        // Login
        composable(Screen.Login.route) {
            LoginScreen(modifier = modifier)
        }
        // Settings
        composable(Screen.Settings.route) {
            GenericScreen(
                title = Screen.Settings.title,
                modifier = modifier
            )
            // TODO: add onclick?
        }
        // Error Handler
        composable(Screen.Error.route) {
            GenericScreen(
                title = Screen.Error.title,
                modifier = modifier
            )
            // TODO: add onclick?
        }
    }
}