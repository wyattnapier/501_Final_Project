package com.example.a501_final_project

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.a501_final_project.chores.ChoresViewModel
import com.example.a501_final_project.events.EventsViewModel
import com.example.a501_final_project.login_register.HouseholdViewModel
import com.example.a501_final_project.login_register.LoginViewModel
import com.example.a501_final_project.payment.PaymentViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.example.a501_final_project.ui.theme._501_Final_ProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _501_Final_ProjectTheme {
                MainScreen()
            }
        }
    }
}

// definitions of all screens used in the app
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Chores : Screen("chores", "Chores", Icons.Default.List)
    object Pay : Screen("pay", "Pay", Icons.Default.Send) // TODO: replace with better money icon
    object Calendar : Screen("calendar", "Calendar", Icons.Default.DateRange)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Login : Screen("login", "Login", Icons.Default.Person) // TODO: replace with unique icon
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Error : Screen("Error", "Error", Icons.Default.Settings)
    object UserSignUp : Screen("UserSignUp", "UserSignUp", Icons.Default.AccountBox)
    // screen for household setup is custom route
}

// list of all screens used in the bottom bar
val bottomBarScreens = listOf(
    Screen.Home,
    Screen.Chores,
    Screen.Pay,
    Screen.Calendar,
)

/**
 * helper function to act as onclick for profile button
 * and the widgets on the home screen to help with navigation
 * @param navController: NavHostController, the navigation controller
 * @param screen: Screen, the screen to navigate to
 */
fun navigateToScreen(navController: NavController, screen: Screen) {
    navController.navigate(screen.route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Main screen that contains the scaffold and navigation
 */
@Composable
fun MainScreen() {

    val navController = rememberNavController()

    // for conditional rendering o top and bottom bars
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Screens where the bars should NOT appear
    val noBars = setOf(
        Screen.Login.route,
        Screen.UserSignUp.route,
    )
    val shouldShowBars = currentRoute !in noBars && !(currentRoute?.startsWith("HouseholdSetup") ?: false)
    // view models
    val mainViewModel: MainViewModel = viewModel()
    val loginViewModel: LoginViewModel = viewModel()
    val paymentViewModel: PaymentViewModel = viewModel()
    val choresViewModel: ChoresViewModel = viewModel()
    val eventsViewModel: EventsViewModel = viewModel()
    val householdViewModel : HouseholdViewModel = viewModel()

    val loginState by loginViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // fetch user google calendar data on app start or login change
    LaunchedEffect(Unit, loginState) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (loginState.isLoggedIn && account != null) {
            Log.d("MainScreen", "Loading user data")
            mainViewModel.loadUserData()
            Log.d("MainScreen", "Loading household data for user")
            mainViewModel.loadHouseholdData()
            Log.d("MainScreen", "Loading current user id for signup-household data")
            householdViewModel.loadCurrentUserId()
        }
    }
    // Separate effect that watches for household data to be loaded to handle race conditions
    LaunchedEffect(loginState.isLoggedIn, mainViewModel.isHouseholdDataLoaded.collectAsState().value) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val isHouseholdLoaded = mainViewModel.isHouseholdDataLoaded.value
        val isChoresLoaded = choresViewModel.isChoresDataLoaded.value
        val isCalendarNameLoaded = eventsViewModel.isCalendarNameLoaded.value
        val isPaymentsLoaded = paymentViewModel.isPaymentsDataLoaded.value

        if (loginState.isLoggedIn && account != null && isHouseholdLoaded) {
            Log.d("MainScreen", "Household loaded, now fetching data for widgets")
            if (!isCalendarNameLoaded) {
                eventsViewModel.loadHouseholdCalendarName(context) // chains call to fetch calendar events too
            }
            if (!isChoresLoaded) {
                choresViewModel.loadHouseholdData()
            }
            if (!isPaymentsLoaded) {
                paymentViewModel.loadPaymentsData()
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (shouldShowBars) {
                BottomBar(navController)
            }
        },
        topBar = {
            if (shouldShowBars) {
                TopBar(navController)
            }
         },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        AppNavGraph(
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            mainViewModel = mainViewModel,
            loginViewModel = loginViewModel,
            paymentViewModel = paymentViewModel,
            choresViewModel = choresViewModel,
            eventsViewModel = eventsViewModel,
            householdViewModel = householdViewModel
        )
    }
}

/**
 * Top bar with navigation to profile page
 * @param navController: NavHostController, the navigation controller
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(navController: NavController){
    val actionIconWidth = 64.dp
    TopAppBar(
        title = {
            Text("apt.",
                modifier=Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge)
                },
        navigationIcon = {
            // Add a Spacer with the same width as the actions icon to balance the layout.
            Spacer(modifier = Modifier.width(actionIconWidth))
        },
        actions = {
            Box(modifier = Modifier.width(actionIconWidth)) {
                IconButton(onClick = { navigateToScreen(navController, Screen.Profile) }) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Profile"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

/**
 * Bottom bar with navigation to bottomBarScreens (home, chores, pay, calendar)
 * @param navController: NavHostController, the navigation controller
 */
@Composable
fun BottomBar(navController: NavHostController) {
    NavigationBar (
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        bottomBarScreens.forEach { screen ->
            NavigationBarItem(
                label = { Text(screen.title) },
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                // We check if the current route is part of the destination's hierarchy.
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                // Define the click action for the item.
                onClick = {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items.
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true // Save the state of the screen you're leaving.
                        }
                        launchSingleTop = true // Avoid multiple copies of the same destination when re-selecting the same item.
                        restoreState = true // Restore state when re-selecting a previously selected item.
                    }
                }
            )
        }
    }
}

/**
 * Generic screen that acts as a placeholder
 * for screens that we haven't developed yet
 * @param title: String, the title of the screen
 */
@Composable
fun GenericScreen(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Welcome to the $title page!",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}