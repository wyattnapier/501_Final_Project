package com.example.a501_final_project

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    // --- STATE ---
    // Expose a read-only StateFlow for the UI to observe.
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    // --- LOGIC ---

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
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
                val accountObject: GoogleSignInAccount? = task.getResult(ApiException::class.java)

                if (accountObject != null && !accountObject.email.isNullOrBlank()) {
                    val email = accountObject.email!!
                    val account = accountObject.account ?: Account(email, "com.google")

                    _uiState.value = LoginUiState(
                        userEmail = email,
                        userAccount = account,
                        isLoginInProgress = false
                    )
                    Log.d("LoginViewModel", "Logged in as $email")
                } else {
                    _uiState.value = LoginUiState(error = "No account found.")
                    Log.d("LoginViewModel", "No account found")
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState(error = "Error logging in: ${e.message}")
                Log.e("LoginViewModel", "Error logging in", e)
            }
        }
    }

    fun signOut(context: Context) {
        val client = GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
        )
        client.signOut().addOnCompleteListener {
            // Reset the state after sign-out is complete.
            _uiState.value = LoginUiState()
            Log.d("LoginViewModel", "Logged out successfully.")
        }
    }

    // helper function to reset error state for toast
    fun errorShown() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

// A data class to hold all UI state in one object.
data class LoginUiState(
    val userEmail: String? = null,
    val userAccount: Account? = null,
    val isLoginInProgress: Boolean = false,
    val error: String? = null
)
