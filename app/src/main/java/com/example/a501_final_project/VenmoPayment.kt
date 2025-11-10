package com.example.a501_final_project

import androidx.compose.runtime.Composable

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun VenmoPaymentScreen(
    username: String = "exampleuser",
    amount: String = "10.00",
    note: String = "Thanks for lunch!"
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Pay with Venmo",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        Button(onClick = {
            val uri = Uri.parse(
                "venmo://paycharge?txn=pay&recipients=$username&amount=$amount&note=${Uri.encode(note)}"
            )
            val intent = Intent(Intent.ACTION_VIEW, uri)

            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // Venmo not installed → open Play Store
                val playStoreUri = Uri.parse("https://play.google.com/store/apps/details?id=com.venmo")
                context.startActivity(Intent(Intent.ACTION_VIEW, playStoreUri))
            }
        }) {
            Text(text = "Launch Venmo")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VenmoPaymentScreenPreview() {
    VenmoPaymentScreen()
}
