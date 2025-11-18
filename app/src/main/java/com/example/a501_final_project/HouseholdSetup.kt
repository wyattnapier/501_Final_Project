package com.example.a501_final_project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.a501_final_project.ui.theme._501_Final_ProjectTheme

@Composable
fun NewHousehold(viewModel: HouseholdViewModel){
    var name by rememberSaveable { mutableStateOf("") }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxHeight().padding(10.dp)
    ){
        Text(
            "New Household",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Create a Household Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    viewModel.updateName(name)
                }
            },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Household")
        }
    }
}

@Composable
fun NewHouseholdChore(viewModel: HouseholdViewModel){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxHeight().padding(10.dp)
    ){
        Text(
            "Create Chores",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Define the recurring chores that you will have for your household. These can be changed at any point through the household settings under profile.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ){
            itemsIndexed(viewModel.choreInputs) { index, chore ->
                Text(
                    "Chore ${index + 1}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                ChoreSection(
                    chore = chore,
                    onChoreChanged = { updated ->
                        viewModel.updateChore(index, updated)
                    }
                )
            }
        }

        Button(
            onClick = { viewModel.addChore() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add another chore")
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { /* Save chores to Firestore */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Chores")
        }


    }
}

@Composable
fun ChoreSection(
    chore: ChoreInput,
    onChoreChanged: (ChoreInput) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        OutlinedTextField(
            value = chore.name,
            onValueChange = { onChoreChanged(chore.copy(name = it)) },
            label = { Text("Chore Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = chore.description,
            onValueChange = { onChoreChanged(chore.copy(description = it)) },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = "${chore.cycle}",
            onValueChange = { newValue ->
                // Allow empty input OR digits only
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    onChoreChanged(chore.copy(cycle = newValue.toInt()))
                }
            },
            label = { Text("Cycle (in days)") },
            modifier = Modifier.fillMaxWidth()
        )

    }
}

@Composable
fun NewHouseholdPayment(viewModel: HouseholdViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxHeight().padding(10.dp)
    ) {
        Text(
            "Create Recurring Payments",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center

        )
        Text(
            "Define the recurring payments that you will have for your household. These can be changed at any point through the household settings under profile.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(viewModel.paymentInputs) { index, payment ->
                Text(
                    "Payment ${index + 1}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                PaymentSection(
                    payment = payment,
                    onPaymentChanged = { updated ->
                        viewModel.updatePayment(index, updated)
                    }
                )
            }
        }

        Button(
            onClick = { viewModel.addPayment() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add another payment")
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { /* Save payments to Firestore */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Payments")
        }
    }
}

@Composable
fun PaymentSection(
    payment: PaymentInput,
    onPaymentChanged: (PaymentInput) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        OutlinedTextField(
            value = payment.name,
            onValueChange = { onPaymentChanged(payment.copy(name = it)) },
            label = { Text("Payment Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = payment.amount.toString(),
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    onPaymentChanged(payment.copy(amount = newValue.toInt()))
                }
            },
            label = { Text("Amount ($)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = payment.split.toString(),
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    onPaymentChanged(payment.copy(split = newValue.toInt()))
                }
            },
            label = { Text("Your split (%)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = "payment.cycle.toString()",
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    onPaymentChanged(payment.copy(cycle = newValue.toInt()))
                }
            },
            label = { Text("Cycle (in days)") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Do you pay this bill?")
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = payment.youPay,
                onCheckedChange = { checked ->
                    onPaymentChanged(payment.copy(youPay = checked))
                }
            )
        }

    }
}

@Composable
fun NewHouseholdCalendar(viewModel: HouseholdViewModel){
    var calendarName by rememberSaveable { mutableStateOf("") }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxHeight().padding(10.dp)
    ){
        Text(
            "New Household",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Enter the name of the shared Google calendar that you use for apartment activities",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        OutlinedTextField(
            value = calendarName,
            onValueChange = { calendarName = it },
            label = { Text("Shared Calendar Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (calendarName.isNotBlank()) {
                    viewModel.updateName(calendarName)
                }
            },
            enabled = calendarName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Household")
        }
    }
}




@Preview(showBackground = true)
@Composable
fun HouseholdPreview() {
    _501_Final_ProjectTheme {
        val viewModel = remember { HouseholdViewModel() }
        NewHouseholdCalendar(viewModel)
    }
}
