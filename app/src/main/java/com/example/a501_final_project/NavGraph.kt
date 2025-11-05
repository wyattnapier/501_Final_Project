package com.example.a501_final_project

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // Home
        composable(Screen.Home.route) {
            HomeScreen(modifier, navController)
        }
        // Chores
        composable(Screen.Chores.route) {
            GenericScreen(
                title = Screen.Chores.title,
                modifier = modifier
            )
            // TODO: add onclick?
        }
        // Pay
        composable(Screen.Pay.route) {
            GenericScreen(
                title = Screen.Pay.title,
                modifier = modifier
            )
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
            GenericScreen(
                title = Screen.Profile.title,
                modifier = modifier
            )
            // TODO: add onclick?
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