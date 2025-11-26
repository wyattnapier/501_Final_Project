package com.example.a501_final_project

import androidx.compose.runtime.Composable

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

// TODO: fetch actual value for these constants from database and use below
const val currentUser = "Wyatt"

@Composable
fun VenmoPaymentScreen(
    modifier: Modifier = Modifier,
    paymentViewModel: PaymentViewModel
) {
    // using stateflow
    val showPastPayments by paymentViewModel.showPastPayments.collectAsState()
    val pastPayments by paymentViewModel.pastPayments.collectAsState()

    // get other lists
    val currentPaymentsForUser = (paymentViewModel.getPaymentsFor(currentUser) + paymentViewModel.getPaymentsFrom(currentUser)).filter { !it.paid }
    val pastPaymentsForUser = pastPayments.filter { it.payFrom == currentUser || it.payTo == currentUser }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Pay with Venmo",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            FilledTonalButton(
                onClick = { paymentViewModel.toggleShowPastPayments() },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (showPastPayments)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = if (showPastPayments) "Showing Past" else "Show Past",
                    color = if (showPastPayments)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        LazyColumn {
            val listToShow = if (showPastPayments) {
                currentPaymentsForUser + pastPaymentsForUser
            } else {
                currentPaymentsForUser
            }
            items(listToShow.size) { index ->
                val payment = listToShow[index]
                PaymentListItem(payment, paymentViewModel, Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
fun PaymentListItem(
    payment: Payment,
    paymentViewModel: PaymentViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
    ) {
        if (payment.paid) {
            PaidPaymentListItem(payment)
        } else {
            UnpaidPaymentListItem(payment, paymentViewModel)
        }
    }
}

@Composable
fun UnpaidPaymentListItem(
    payment: Payment,
    paymentViewModel: PaymentViewModel
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(6f)
        ) {
            Text(
                text = "${payment.payFrom} must pay $${"%.2f".format(payment.amount)} to ${payment.payTo}",
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
        if (payment.payTo == "Wyatt") { // TODO: change to current user
            PaymentReminderButton(
                payment = payment,
                modifier = Modifier
                    .weight(5f)
                    .padding(start = 8.dp)
            )
        } else {
            PayButton(
                payTo = payment.payTo,
                amount = payment.amount,
                note = payment.memo,
                paymentViewModel = paymentViewModel,
                modifier = Modifier
                    .weight(5f)
                    .padding(start = 8.dp),
            )
        }
    }
}

@Composable
fun PaidPaymentListItem(
    payment: Payment,
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
                text = "${payment.payFrom} paid $${"%.2f".format(payment.amount)} to ${payment.payTo}",
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
    }
}

@Composable
fun PayButton(
    payTo: String,
    amount: Double,
    note: String,
    modifier: Modifier = Modifier,
    paymentViewModel: PaymentViewModel
) {
    val context = LocalContext.current // used to open venmo activity

    val payToVenmoUsername = paymentViewModel.getVenmoUsername(payTo)
    if (payToVenmoUsername == null) {
        Log.d("payment","Error: Venmo username not found for $payTo")
        // TODO: add more robust error handling
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

@Composable
fun PaymentReminderButton(
    payment: Payment,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = {Log.d("Payment", "Reminder button clicked") },
        modifier = modifier
    ) {
        Text(text = "Remind ${payment.payFrom}")
    }
}