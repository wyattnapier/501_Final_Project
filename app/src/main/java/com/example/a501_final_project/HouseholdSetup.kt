package com.example.a501_final_project

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHousehold(viewModel: HouseholdViewModel){
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = { TopAppBar(
            title = { Text("Household Set Up", modifier=Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth()
        ) },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.decrementStep() },
                        enabled = viewModel.setupStep > 0
                    ) {
                        Text("Previous")
                    }
                    if (viewModel.setupStep < 4) {
                        Button(
                            onClick = { viewModel.incrementStep() },
                            enabled = viewModel.setupStep < 4
                        ) {
                            Text("Next")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.createHousehold() },
                            enabled = !viewModel.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Create Household")
                        }
                    }
                }
            }
        },
        content = { innerPadding ->
            if (viewModel.householdCreated) {
                HouseholdCreated(viewModel, Modifier.padding(innerPadding))
            }
            else {
                AnimatedContent(targetState = viewModel.setupStep) { step ->
                    when (step) {
                        0 -> NewHouseholdName(viewModel, Modifier.padding(innerPadding))
                        1 -> NewHouseholdChore(viewModel, Modifier.padding(innerPadding))
                        2 -> NewHouseholdPayment(viewModel, Modifier.padding(innerPadding))
                        3 -> NewHouseholdCalendar(viewModel, Modifier.padding(innerPadding))
                        4 -> ReviewHouseholdDetails(viewModel, Modifier.padding(innerPadding))
                    }
                }
            }
        }
    )
}


@Composable
fun NewHouseholdName(viewModel: HouseholdViewModel, modifier: Modifier){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxHeight()
            .padding(10.dp)
    ){
        Text(
            "Name Your Household",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        OutlinedTextField(
            value = viewModel.householdName,
            onValueChange = { viewModel.updateName(it) },
            label = { Text("Create a Household Name") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun NewHouseholdChore(viewModel: HouseholdViewModel, modifier: Modifier){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxHeight()
            .padding(10.dp)
    ){
        Text(
            "Create Chores",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            "Define the recurring chores that you will have for your household. These can be changed at any point through the household settings under profile.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
fun NewHouseholdPayment(viewModel: HouseholdViewModel, modifier: Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxHeight()
            .padding(10.dp)
    ) {
        Text(
            "Create Recurring Payments",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center

        )
        Text(
            "Define the recurring payments that you will have for your household. These can be changed at any point through the household settings under profile.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
            value = payment.cycle.toString(),
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
            Text("I pay this bill:")
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
fun NewHouseholdCalendar(viewModel: HouseholdViewModel, modifier: Modifier){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxHeight()
            .padding(10.dp)
    ){
        Text(
            "Set Up Household Calendar",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Enter the name of the shared Google calendar that you use for apartment activities",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        OutlinedTextField(
            value = viewModel.calendarName,
            onValueChange = { viewModel.updateCalendar(it) },
            label = { Text("Shared Calendar Name") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ReviewHouseholdDetails(viewModel: HouseholdViewModel, modifier: Modifier){
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        item {
            Text(
                "Household Name",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                viewModel.householdName.ifBlank { "Not set" },
                style = MaterialTheme.typography.bodyLarge
            )
        }

        item {
            Text(
                "Chores",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (viewModel.choreInputs.isEmpty()) {
            item { Text("No chores added.") }
        } else {
            itemsIndexed(viewModel.choreInputs) { index, chore ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Chore ${index + 1}", style = MaterialTheme.typography.titleMedium)
                        Text("Name: ${chore.name}")
                        Text("Description: ${chore.description}")
                        Text("Cycle: ${chore.cycle} days")
                    }
                }
            }
        }

        item {
            Text(
                "Payments",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (viewModel.paymentInputs.isEmpty()) {
            item { Text("No payments added.") }
        } else {
            itemsIndexed(viewModel.paymentInputs) { index, payment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Payment ${index + 1}", style = MaterialTheme.typography.titleMedium)
                        Text("Name: ${payment.name}")
                        Text("Amount: $${payment.amount}")
                        Text("Split: ${payment.split}%")
                        Text("Cycle: ${payment.cycle} days")
                        Text("Pays: ${if (payment.youPay) "You" else "Someone else"}")
                    }
                }
            }
        }

        item {
            Text(
                "Calendar Name",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                viewModel.calendarName.ifBlank { "Not set" },
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun HouseholdCreated(viewModel: HouseholdViewModel, modifier: Modifier){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxHeight()
            .padding(10.dp)
    ){
        Text(
            "Household Created",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Household ID: ${viewModel.householdID}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            "Shared Household ID with your roommates so they can join the household",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Button(
            onClick = { /*TODO: navigate back to home screen*/ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Proceed to App")
        }
    }
}



@Preview(showBackground = true)
@Composable
fun HouseholdPreview() {
    _501_Final_ProjectTheme {
        val viewModel = remember { HouseholdViewModel() }
        NewHousehold(viewModel)
    }
}
