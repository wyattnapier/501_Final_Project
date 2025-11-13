package com.example.a501_final_project

import android.accounts.Account
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.compose.animation.core.copy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    // check if user is logged in or not on init
    init {
        // Observe Firebase auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                // User is signed in to Firebase
                _uiState.value = _uiState.value.copy(
                    userEmail = firebaseUser.email,
                    userName = firebaseUser.displayName,
                    profilePictureUrl = firebaseUser.photoUrl?.toString(),
                    isLoginInProgress = false,
                    isLoggedIn = true,
                    userAccount = firebaseUser.email?.let { Account(it, "com.google") }
                )
                Log.d("LoginViewModel", "Firebase user signed in: ${firebaseUser.email}")
            } else {
                // User is signed out
                _uiState.value = _uiState.value.copy( // use a copy for better error handling
                    isLoggedIn = false,
                    isLoginInProgress = false,
                    userEmail = null,
                    userName = null,
                    profilePictureUrl = null,
                    userAccount = null
                )
                Log.d("LoginViewModel", "Firebase user signed out.")
            }
        }
    }

    // get token that firebase can use to sign in while also getting gcal permissions
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/calendar.events.readonly"))
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
            // AuthStateListener will handle updating the UI state.
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

                // Sign out from Google to allow account switching
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                GoogleSignIn.getClient(context, gso).signOut().await()

                // AuthStateListener will handle resetting the UI state.
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Sign out failed: ${e.message}")
            }
        }
    }
}

// A data class to hold all UI state in one object.
data class LoginUiState(
    val userEmail: String? = null,
    val userAccount: Account? = null,
    val userName: String? = null,
    val profilePictureUrl: String? = null,
    val isLoginInProgress: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false // flag for firebase state
)
