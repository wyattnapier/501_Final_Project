package com.example.a501_final_project.login_register

import android.accounts.Account
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a501_final_project.FirestoreRepository
import com.example.a501_final_project.MainViewModel
import com.example.a501_final_project.R
import com.example.a501_final_project.chores.ChoresViewModel
import com.example.a501_final_project.events.EventsViewModel
import com.example.a501_final_project.payment.PaymentViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// maps out which step of sign up we are in
enum class SignUpSteps {
    GOOGLE_LOGIN,
    USER_INFO,
    REVIEW
}

// maps out overall login state
// TODO: replace the SignUpSteps enum with this one
enum class UserState {
    CHECKING,           // Initial state, checking auth
    NOT_LOGGED_IN,      // No Google account
    NEEDS_SETUP,        // needs user setup in db and/or household setup in db
    READY              // Fully authenticated and in household
}

class LoginViewModel(
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {
    var displayName by mutableStateOf("")
    var venmoUsername by mutableStateOf("")

    private val auth: FirebaseAuth = Firebase.auth

    private val _userState = MutableStateFlow(UserState.NOT_LOGGED_IN) // TODO: shoudl be set to checking
    val userState = _userState.asStateFlow()

    // Keep existing _uiState but simplify it
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _signOutComplete = MutableSharedFlow<Unit>()
    val signOutComplete = _signOutComplete.asSharedFlow()

    // check if user is logged in or not on init
    init {
        auth.addAuthStateListener { firebaseAuth ->
            Log.d("LoginViewModel", "AuthStateListener triggered")
            viewModelScope.launch {
                handleAuthStateChange(firebaseAuth.currentUser != null)
            }
        }
    }

    /**
     * Centralized function to handle auth state changes
     */
    private suspend fun handleAuthStateChange(isSignedIn: Boolean) {
        if (!isSignedIn) {
            Log.d("LoginViewModel", "No Firebase user, setting to NOT_LOGGED_IN")
            _userState.value = UserState.NOT_LOGGED_IN
            _uiState.value = LoginUiState(
                isChecking = false,
                isLoginInProgress = false
            )
            return
        }

        val firebaseUser = auth.currentUser ?: return
        Log.d("LoginViewModel", "Processing Firebase user: ${firebaseUser.email}")

        try {
            // Refresh token to ensure we're in sync with server
            firebaseUser.getIdToken(true).await()
            Log.d("LoginViewModel", "✓ Token refreshed")

            // Check user and household status
            val userExists = repository.checkUserExists(firebaseUser.uid)
            Log.d("LoginViewModel", "✓ User exists: $userExists")

            val hasHousehold = if (userExists) {
                repository.isUserInHousehold(firebaseUser.uid)
            } else {
                false
            }
            Log.d("LoginViewModel", "✓ Has household: $hasHousehold")

            // Determine user state
            _userState.value = when {
                !userExists -> {
                    Log.d("LoginViewModel", "→ State: NEEDS_SETUP (no user)")
                    UserState.NEEDS_SETUP
                }
                !hasHousehold -> {
                    Log.d("LoginViewModel", "→ State: NEEDS_SETUP (no household)")
                    UserState.NEEDS_SETUP
                }
                else -> {
                    Log.d("LoginViewModel", "→ State: READY")
                    UserState.READY
                }
            }

            // Update UI state
            _uiState.value = _uiState.value.copy(
                userEmail = firebaseUser.email,
                userName = firebaseUser.displayName,
                profilePictureUrl = firebaseUser.photoUrl?.toString(),
                isLoginInProgress = false, // ✓ Stop loading
                isChecking = false,
                isLoggedIn = true,
                userAccount = firebaseUser.email?.let { Account(it, "com.google") },
                error = null
            )

            Log.d("LoginViewModel", "✓ Auth processing complete")

        } catch (e: Exception) {
            Log.e("LoginViewModel", "✗ Error processing auth state", e)
            auth.signOut()
            _userState.value = UserState.NOT_LOGGED_IN
            _uiState.value = LoginUiState(
                error = "Authentication error: ${e.message}",
                isChecking = false,
                isLoginInProgress = false
            )
        }
    }

    // function to figure out which screen to display based on enum in MainActivity.kt
    fun refreshUserState() {
        Log.d("LoginViewModel", "refreshUserState called")
        viewModelScope.launch {
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                val userExists = repository.checkUserExists(firebaseUser.uid)
                val hasHousehold = if (userExists) repository.isUserInHousehold(firebaseUser.uid) else false

                _userState.value = when {
                    !userExists || !hasHousehold -> UserState.NEEDS_SETUP
                    else -> UserState.READY
                }
            }
        }
        Log.d("LoginViewModel", "refreshUserState completed: ${_userState.value}")
    }

    // get token that firebase can use to sign in while also getting gcal permissions and auth code
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .requestServerAuthCode(context.getString(R.string.default_web_client_id))
            .requestScopes(Scope(CalendarScopes.CALENDAR))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun handleSignInResult(result: ActivityResult) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoginInProgress = true)
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val googleAccount = task.getResult(ApiException::class.java)

                if (googleAccount?.idToken != null) {
                    firebaseAuthWithGoogle(googleAccount.idToken!!)
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Google Sign-In failed: No ID token.",
                        isLoginInProgress = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Google Sign-In error: ${e.message}",
                    isLoginInProgress = false
                )
                Log.e("LoginViewModel", "Google Sign-In error", e)
            }
        }
    }

    private suspend fun firebaseAuthWithGoogle(idToken: String) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            handleAuthStateChange(true)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Firebase authentication failed: ${e.message}",
                isLoginInProgress = false
            )
            Log.e("LoginViewModel", "Firebase auth failed", e)
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            try {
                // Sign out from Firebase
                auth.signOut()

                // reset state variables
                _uiState.value = _uiState.value.copy(
                    userEmail = null,
                    userName = null,
                    profilePictureUrl = null,
                    isLoginInProgress = false,
                    error = null,
                    isLoggedIn = false,
                    userAlreadyExists = null,
                    isChecking = true
                )

                // Sign out from Google to allow account switching
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                GoogleSignIn.getClient(context, gso).signOut().await()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Sign out failed: ${e.message}")
            }
        }
    }

    fun fullSignOut(
        context: Context,
        mainViewModel: MainViewModel,
        eventsViewModel: EventsViewModel,
        choresViewModel: ChoresViewModel,
        paymentViewModel: PaymentViewModel,
        householdViewModel: HouseholdViewModel
    ) {
        viewModelScope.launch {
            try {
                // user profile sign out
                signOut(context)
                // Now, reset all other ViewModels
                mainViewModel.reset()
                eventsViewModel.reset()
                choresViewModel.reset()
                paymentViewModel.reset()
                householdViewModel.reset()
                // triggers navigation and rerender
                _userState.value = UserState.NOT_LOGGED_IN // slower UI but safer if signout fails
                // Emit an event to tell the UI to navigate
                _signOutComplete.emit(Unit)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Sign out failed: ${e.message}")
            }
        }
    }

    fun saveUserToDb() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w("LoginViewModel", "No authenticated user found, can't save to Firestore")
            return
        }

        viewModelScope.launch {
            try {
                repository.saveNewUser(currentUser.uid, displayName, venmoUsername)
                Log.d("LoginViewModel", "User save process initiated for ID: ${currentUser.uid}")
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error saving user to Firestore", e)
                _uiState.value = _uiState.value.copy(error = "Failed to save user profile.")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

// A data class to hold all UI state in one object.
// todo: remove isLoggedIn and isChecking
data class LoginUiState(
    val userEmail: String? = null,
    val userAccount: Account? = null,
    val userName: String? = null,
    val profilePictureUrl: String? = null,
    val isLoginInProgress: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false, // flag for firebase state
    val userAlreadyExists: Boolean? = null,
    val isChecking : Boolean = true // flag for if login status is actively being checked, since we dont want login or signup to appear right off the bat, this has to be true
)

// data class for a user
data class Member(
    val name : String,
    val venmoUsername : String,
)
