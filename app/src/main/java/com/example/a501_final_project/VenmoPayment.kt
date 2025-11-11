package com.example.a501_final_project

import androidx.compose.runtime.Composable

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.net.toUri

@Composable
fun VenmoPaymentScreen(
    viewModel: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    // TODO: get this from the viewmodel instead of dummy data
    val payments = listOf(
        Payment(1, "alice_username", "Wyatt", 15.50, "Dinner", paid = false, recurring = false),
        Payment(2, "tiffany_username", "Wyatt", 25.00, "Utilities", paid = false, recurring = true),
        Payment(3, "john_username", "Wyatt", 100.25, "Rent", paid = false, recurring = false),
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Pay with Venmo",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 20.dp)
        )
        LazyColumn {
            items(payments.size) { index ->
                val payment = payments[index]
                PaymentListItem(payment, Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
fun PaymentListItem(
    payment: Payment,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Pay $${"%.2f".format(payment.amount)} to ${payment.payTo}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "For: ${payment.memo}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Paid: ${if (payment.paid) "Yes" else "No"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (payment.paid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            PayButton(
                payTo = payment.payTo,
                amount = payment.amount,
                note = payment.memo,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun PayButton(
    payTo: String,
    amount: Double,
    note: String,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current // used to open venmo activity

    val payToVenmoUsername = viewModel.getVenmoUsername(payTo) // TODO: convert username on our app to venmo username
    if (payToVenmoUsername == null) {
        Log.d("payment","Error: Venmo username not found for $payTo")
        val payToVenmoUsername = "Wyatt" // temp placeholder rather than throwing real error
    }

    Button(onClick = {
        val uri =
            "venmo://paycharge?txn=pay&recipients=$payToVenmoUsername&amount=$amount&note=$note".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Venmo not installed → open Play Store
            val playStoreUri = "https://play.google.com/store/apps/details?id=com.venmo".toUri()
            context.startActivity(Intent(Intent.ACTION_VIEW, playStoreUri))
        }
    },
        modifier = modifier
    ) {
        Text(text = "Pay $payTo")
    }
}

@Preview(showBackground = true)
@Composable
fun VenmoPaymentScreenPreview() {
    VenmoPaymentScreen(modifier = Modifier.fillMaxSize())
}
