package com.example.a501_final_project

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavHostController
import com.example.a501_final_project.chores.ChoresScreen
import com.example.a501_final_project.chores.ChoresViewModel
import com.example.a501_final_project.events.EventsScreen
import com.example.a501_final_project.events.EventsViewModel
import com.example.a501_final_project.login_register.HouseholdLanding
import com.example.a501_final_project.login_register.HouseholdViewModel
import com.example.a501_final_project.login_register.LoginScreen
import com.example.a501_final_project.login_register.LoginViewModel
import com.example.a501_final_project.login_register.ProfileScreen
import com.example.a501_final_project.login_register.SignUpScreen
import com.example.a501_final_project.login_register.UserPrefScreen
import com.example.a501_final_project.payment.PaymentViewModel
import com.example.a501_final_project.payment.VenmoPaymentScreen

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    mainViewModel: MainViewModel,
    paymentViewModel: PaymentViewModel,
    loginViewModel: LoginViewModel,
    choresViewModel: ChoresViewModel,
    eventsViewModel: EventsViewModel,
    householdViewModel : HouseholdViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // Home
        composable(Screen.Home.route) {
            HomeScreen(navController, mainViewModel, eventsViewModel, paymentViewModel, choresViewModel, modifier = modifier)
        }
        // Chores
        composable(Screen.Chores.route) {
            ChoresScreen(
                mainViewModel = mainViewModel,
                choresViewModel = choresViewModel,
                modifier = modifier
            )
        }
        // Pay
        composable(Screen.Pay.route) {
            VenmoPaymentScreen(
                modifier = modifier,
                paymentViewModel = paymentViewModel,
                mainViewModel = mainViewModel
            )
        }
        // Calendar
        composable(Screen.Calendar.route) {
            EventsScreen(
                modifier = modifier,
                loginViewModel = loginViewModel,
                eventsViewModel = eventsViewModel
            )
        }
        // Profile
        composable(Screen.Profile.route) {
            ProfileScreen(
                modifier = modifier,
                navController = navController,
                loginViewModel = loginViewModel
            )
        }
        // Login
        composable(Screen.Login.route) {
            LoginScreen(
                modifier = modifier,
                viewModel = loginViewModel,
                navController = navController
            )
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
        }

        // User sign up page
        // TODO: adjust this navigation later on, currently just to be able to navigate to sign up page
        composable(Screen.UserSignUp.route) {
            SignUpScreen(
                loginViewModel = loginViewModel,
                navController = navController,
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                    }
                })
        }

        // to make householdsetup page navigable
        composable(Screen.HouseholdSetup.route) {
            HouseholdLanding(
                viewModel = householdViewModel, navController = navController
            )
        }
    }
}