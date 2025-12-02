package com.example.a501_final_project.payment

import android.R
import androidx.compose.runtime.Composable

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.a501_final_project.MainViewModel

@Composable
fun VenmoPaymentScreen(
    modifier: Modifier = Modifier,
    paymentViewModel: PaymentViewModel,
    mainViewModel: MainViewModel
) {
    // using stateflow
    val showPastPayments by paymentViewModel.showPastPayments.collectAsState()
    val pastPayments by paymentViewModel.pastPayments.collectAsState()
    val currentUserId by mainViewModel.userId.collectAsState()

    // Get payments by ID
    val currentPaymentsForUser = (
            paymentViewModel.getPaymentsFor(currentUserId ?: "") +
                    paymentViewModel.getPaymentsFrom(currentUserId ?: "")
            ).filter { !it.paid }

    val pastPaymentsForUser = pastPayments.filter {
        it.payFromId == currentUserId || it.payToId == currentUserId
    }

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
                if (currentUserId == null) {
                    Log.d("VenmoPaymentScreen", "Skipping payment with null currentUserId")
                    return@items
                }
                PaymentListItem(payment, currentUserId!!, Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
fun PaymentListItem(
    payment: Payment,
    currentUser: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
    ) {
        if (payment.paid) {
            PaidPaymentListItem(payment)
        } else {
            UnpaidPaymentListItem(payment, currentUser)
        }
    }
}

@Composable
fun UnpaidPaymentListItem(
    payment: Payment,
    currentUserId: String
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(6f)) {
            Text(
                text = "${payment.payFromName} must pay $${"%.2f".format(payment.amount)} to ${payment.payToName}",
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

        if (payment.payToId == currentUserId) {
            PaymentReminderButton(
                payment = payment,
                modifier = Modifier
                    .weight(5f)
                    .padding(start = 8.dp)
            )
        } else {
            PayButton(
                payToVenmoUsername = payment.payToVenmoUsername,
                amount = payment.amount,
                note = payment.memo,
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
                text = "${payment.payFromName} paid $${"%.2f".format(payment.amount)} to ${payment.payToName}",
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
    payToVenmoUsername: String?,
    amount: Double,
    note: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Button(
        onClick = {
            if (payToVenmoUsername == null) {
                Toast.makeText(
                    context,
                    "Venmo username not available",
                    Toast.LENGTH_SHORT
                ).show()
                return@Button
            }

            val uri = "venmo://paycharge?txn=pay&recipients=$payToVenmoUsername&amount=$amount&note=$note".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)

            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // Venmo not installed → open Play Store
                val playStoreUri = "https://play.google.com/store/apps/details?id=com.venmo".toUri()
                context.startActivity(Intent(Intent.ACTION_VIEW, playStoreUri))
            }
        },
        modifier = modifier,
        enabled = payToVenmoUsername != null  // Disable if no venmo username
    ) {
        Text(text = "Pay via Venmo")
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
        Text(text = "Remind ${payment.payFromName}")
    }
}

@Composable
fun UpcomingPaymentsWidget(
    onCardClick: () -> Unit,
    currentPaymentsForUser: List<Payment>,
    currentUserId: String?,
    modifier: Modifier = Modifier
) {
    if (currentUserId == null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = { onCardClick() }),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(
                    "Upcoming Payments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Login to see payment information!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Log.d("UpcomingPaymentsWidget", "Skipping payment with null currentUserId")
        return
    }
    val oweOthers = currentPaymentsForUser
        .filter { it.payFromId == currentUserId }
        .sortedBy{ it.dueDate }
    val othersOwe = currentPaymentsForUser
        .filter { it.payToId == currentUserId }
        .sortedBy{ it.dueDate }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = { onCardClick() }),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                "Upcoming Payments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "You owe others",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (oweOthers.isEmpty()) {
                        Text(
                            text = "You're all caught up!",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        oweOthers.forEach { payment ->
                            UpcomingPaymentItem(false, payment)
                        }
                    }
                }

                VerticalDivider(
                    thickness = 2.dp,
                    color = androidx.compose.ui.graphics.Color.Black,
                    modifier = Modifier.alpha(0.5f)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Others owe you",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (othersOwe.isEmpty()) {
                        Text(
                            text = "Everybody has paid you!",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        othersOwe.forEach { payment ->
                            UpcomingPaymentItem(true, payment)
                        }
                    }
                }

            }
        }
    }
}

@Composable
fun UpcomingPaymentItem(
    payToCurrentUser: Boolean,
    payment: Payment
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Column {
            Text (
                text =
                    if (payToCurrentUser) {
                        "${payment.payFromName} owes you for ${payment.memo}"
                    } else {
                        "You owe ${payment.payToName} for ${payment.memo}"
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}