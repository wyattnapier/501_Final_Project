package com.example.a501_final_project.payment

import androidx.compose.runtime.Composable
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.a501_final_project.MainViewModel
import com.example.a501_final_project.models.LocalResident
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

enum class PaymentDirection {
    PAYING, REQUESTING
}

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
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    val paymentsList by paymentViewModel.paymentsList.collectAsState()

    val currentPaymentsForUser = (
            paymentViewModel.getPaymentsFor(currentUserId ?: "", paymentsList) + // Pass the observed list
                    paymentViewModel.getPaymentsFrom(currentUserId ?: "", paymentsList) // Pass the observed list
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
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                IconButton(onClick = { showAddPaymentDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Payment")
                }
            }
            Text(
                text = "Pay with Venmo",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(2f) // Give it more weight to occupy the center
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
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
                        text = if (showPastPayments) "Past" else "Now",
                        color = if (showPastPayments)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
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
                PaymentListItem(
                    paymentViewModel = paymentViewModel,
                    payment = payment,
                    currentUserId = currentUserId!!,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
    if (showAddPaymentDialog) {
        AddPaymentDialog(
            mainViewModel = mainViewModel,
            onDismiss = { showAddPaymentDialog = false },
            onConfirm = { payFromId, payToId, amount, memo, dueDate ->
                paymentViewModel.createNewPayment(
                    payFromId = payFromId,
                    payToId = payToId,
                    amount = amount,
                    memo = memo,
                    dueDate = dueDate
                )
                showAddPaymentDialog = false
            }
        )
    }
}

@Composable
fun PaymentListItem(
    paymentViewModel: PaymentViewModel,
    payment: Payment,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
    ) {
        if (payment.paid) {
            PaidPaymentListItem(payment)
        } else {
            UnpaidPaymentListItem(
                paymentViewModel =paymentViewModel,
                payment = payment,
                currentUserId = currentUserId
            )
        }
    }
}

@Composable
fun UnpaidPaymentListItem(
    paymentViewModel: PaymentViewModel,
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
                paymentViewModel = paymentViewModel,
                payment = payment,
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
    paymentViewModel: PaymentViewModel,
    payment: Payment,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val payToVenmoUsername: String? = payment.payToVenmoUsername
    val amount: Double = payment.amount
    val note: String = payment.memo

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
                paymentViewModel.completePayment(payment)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentDialog(
    mainViewModel: MainViewModel,
    onDismiss: () -> Unit,
    onConfirm: (payFromId: String, payToId: String, amount: Double, memo: String, dueDate: Date?) -> Unit
) {
    val residents by mainViewModel.residents.collectAsState()
    val currentUserId by mainViewModel.userId.collectAsState()
    val otherResidents = residents.filter { it.id != currentUserId } // everyone except current user

    var amount by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedResident by remember { mutableStateOf<LocalResident?>(null) }
    var paymentDirection by remember { mutableStateOf(PaymentDirection.PAYING) }
    val options = listOf("Pay To", "Request From")
    var dueDate by remember { mutableStateOf<Date?>(null) }
    val showDatePicker = remember { mutableStateOf(false) }

    if (showDatePicker.value) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker.value = false
                        // Convert the selected Long to a Date object
                        datePickerState.selectedDateMillis?.let {
                            dueDate = Date(it)
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Payment Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // select if requesting or paying
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = RoundedCornerShape(50),
                            onClick = {
                                paymentDirection = if (index == 0) PaymentDirection.PAYING else PaymentDirection.REQUESTING
                            },
                            selected = (paymentDirection == PaymentDirection.PAYING && index == 0) ||
                                    (paymentDirection == PaymentDirection.REQUESTING && index == 1)
                        ) {
                            Text(label)
                        }
                    }
                }
                // Dropdown to select who to pay
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(),
                        readOnly = true,
                        value = selectedResident?.name ?: "Select a person",
                        label = { Text(if (paymentDirection == PaymentDirection.PAYING) "Pay To" else "Request From") },
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        otherResidents.forEach { resident ->
                            DropdownMenuItem(
                                text = { Text(resident.name) },
                                onClick = {
                                    selectedResident = resident // Store the whole object
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                    label = { Text("Amount") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Memo Field
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("Memo (e.g., Groceries)") }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Display the selected date or a placeholder
                    Text(
                        text = dueDate?.let {
                            "Due: ${
                                SimpleDateFormat(
                                    "MMM d, yyyy",
                                    Locale.getDefault()
                                ).format(it)}"
                        } ?: "No due date set",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Button to open the date picker
                    TextButton(onClick = { showDatePicker.value = true }) {
                        Text(if (dueDate == null) "Set Date" else "Change")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalAmount = amount.toDoubleOrNull()
                    val otherUserId = selectedResident?.id
                    if (selectedResident != null && finalAmount != null && finalAmount > 0) {
                        val payFromId = if (paymentDirection == PaymentDirection.PAYING) currentUserId else otherUserId
                        val payToId = if (paymentDirection == PaymentDirection.PAYING) otherUserId else currentUserId
                        if (payFromId == null || payToId == null) {
                            Log.e("AddPaymentDialog", "payFromId or payToId is null")
                            return@Button
                        }
                        onConfirm(payFromId, payToId, finalAmount, memo, dueDate)
                    }
                },
                // Disable button until all fields are valid
                enabled = selectedResident != null && (amount.toDoubleOrNull() ?: 0.0) > 0.0 && memo.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
