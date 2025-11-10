package com.example.a501_final_project

import androidx.compose.runtime.Composable

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
//        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Pay with Venmo",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 20.dp)
        )
        LazyColumn {
            for (payment in payments) {
                item {
                    PaymentListItem(payment, modifier.fillMaxWidth()
                        .padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun PaymentListItem(payment: Payment, modifier: Modifier) {
    val payTo = payment.payTo
    val amount = payment.amount
    val note = payment.memo

    Row(
        modifier = Modifier.padding(vertical=5.dp, horizontal = 10.dp).fillMaxWidth().clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer).padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Pay $amount to $payTo for $note")
            Text(text = "Paid: ${payment.paid}") // TODO: add checkbox icon here
            PayButton(payTo, amount, note)
        }
    }
}

@Composable
fun PayButton(
    payTo: String,
    amount: Double,
    note: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current // used to open venmo activity

//    val payToVenmoUsername = viewModel.getVenmoUsername(payTo) // TODO: convert username on our app to venmo username
    val payToVenmoUsername = payTo // temp placeholder

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
    VenmoPaymentScreen()
}
