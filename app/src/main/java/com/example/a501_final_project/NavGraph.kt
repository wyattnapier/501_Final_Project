package com.example.a501_final_project

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
            GenericScreen(
                title = Screen.Home.title,
                modifier = modifier
                // TODO: add onclick?
            )
        }
        composable(Screen.Calendar.route) {
            GenericScreen(
                title = Screen.Calendar.title,
                modifier = modifier
            )
            // TODO: add onclick?
        }
    }
}