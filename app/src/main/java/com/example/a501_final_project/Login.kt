package com.example.a501_final_project

import android.accounts.Account
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    var userEmail by remember { mutableStateOf<String?>(null) }
    var userAccount by remember { mutableStateOf<Account?>(null)}

    LoginSubScreen(
        userEmail = userEmail,
        onLogin = { email, account ->
            userEmail = email
            userAccount = account
        },
        onLogout = {
            userEmail = null
            userAccount = null
        },
        modifier = modifier
    )
}

@Composable
fun LoginSubScreen(
    userEmail: String?,
    onLogin: (String, Account) -> Unit,     // used to hoist state
    onLogout: () -> Unit,                   // used to hoist state
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var loginInProgress by remember { mutableStateOf(false) }

    // create the signin launcher which is the little google window
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val accountObject: GoogleSignInAccount? = task.getResult(ApiException::class.java)
                val email: String? = accountObject?.email // todo: may need to trim
                Log.d("LoginScreen", "Account: $accountObject")
                Log.d("LoginScreen", "Email: $email")
                if (!email.isNullOrBlank()) {
                    val account = accountObject.account ?: Account(email, "com.google")
                    onLogin(email, account)
                    Log.d("LoginScreen", "Logged in as $email")
                } else {
                    Log.d("LoginScreen", "No account found")
                }
            } catch (e: Exception) {
                Log.d("LoginScreen", "Error logging in", e)
            } finally {
                loginInProgress = false // login in popup done!
            }
        }
    )

    // actual login/logout UI goes here
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // improve the logic here
        if (userEmail != null) {
            Text(text = "Logged in as $userEmail")
            Button(onClick = {
                loginInProgress = false
                val client = GoogleSignIn.getClient(
                    context,
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build()
                )
                client.signOut()    // actually logout
                onLogout()          // update state
            }) {
                Text(text = "Logout")
            }
        } else {
            Button(
                onClick = { loginInProgress = true // login process beginning
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(Scope("https://www.googleapis.com/auth/calendar.events.readonly")) // TODO: update scope if adding events too
                        .build()
                    val client = GoogleSignIn.getClient(context, gso)
                    signInLauncher.launch(client.signInIntent)
                },
                enabled = !loginInProgress
            ) {
                Text(text = "Login")
            }
        }
    }
}