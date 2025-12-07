package com.example.a501_final_project.payment

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun UpcomingPaymentsWidget(
    paymentViewModel: PaymentViewModel,
    onCardClick: () -> Unit,
    currentUserId: String?,
    modifier: Modifier = Modifier
) {
    val isPaymentLoading = paymentViewModel.isLoading.collectAsState()
    val allPayments = paymentViewModel.paymentsList.collectAsState()
    val allPaymentsList = allPayments.value

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
                    style = MaterialTheme.typography.titleLarge,
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
    val oweOthers = allPaymentsList
        .filter { it.payFromId == currentUserId }
        .sortedBy{ it.dueDate }
    val othersOwe = allPaymentsList
        .filter { it.payToId == currentUserId }
        .sortedBy{ it.dueDate }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp) // min height
            .clickable(onClick = { onCardClick() }),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onTertiaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Payments",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (isPaymentLoading.value) {
                Box(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Card // skip rest of content in card
            }

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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}