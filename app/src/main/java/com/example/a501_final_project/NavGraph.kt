package com.example.a501_final_project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.a501_final_project.events.EventsScreen

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    mainViewModel: MainViewModel,
    loginViewModel: LoginViewModel
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
            HomeScreen(navController, mainViewModel, modifier = modifier)
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
            EventsScreen(
                modifier = modifier,
                loginViewModel = loginViewModel,
                mainViewModel = mainViewModel
            )
        }
        // Profile
        composable(Screen.Profile.route) {
            ProfileScreen(modifier = modifier, navController = navController, loginViewModel = loginViewModel)
        }
        // Login
        composable(Screen.Login.route) {
            LoginScreen(modifier = modifier, viewModel = loginViewModel, navController = navController)
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

        // User sign up page
        // TODO: adjust this navigation later on, currently jsut to be able to navigate to sign up page
        composable(Screen.UserSignUp.route) {
            SignUpScreen(loginViewModel = loginViewModel, navController = navController, onNavigateToLogin = {
                navController.navigate("login") {
                    popUpTo("signup") { inclusive = true }
                }
            })
        }
    }
}