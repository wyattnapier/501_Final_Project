package com.example.a501_final_project

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
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

    // creating shared viewModels for all screens
    val mainViewModel: MainViewModel = viewModel() // lifecycle-aware
    val loginViewModel: LoginViewModel = viewModel()

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
            EventsScreen(modifier = modifier, mainViewModel)
        }
        // Profile
        composable(Screen.Profile.route) {
            ProfileScreen(modifier = modifier, navController = navController, loginViewModel = loginViewModel)
        }
        // Login
        composable(Screen.Login.route) {
            LoginScreen(modifier = modifier, viewModel = loginViewModel)
        }
        // Settings
        composable(Screen.Settings.route) {
            UserPrefScreen(modifier)
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