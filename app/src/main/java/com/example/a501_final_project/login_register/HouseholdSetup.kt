package com.example.a501_final_project.login_register

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.a501_final_project.R
import com.example.a501_final_project.ui.theme._501_Final_ProjectTheme

@Composable
fun HouseholdLanding(viewModel: HouseholdViewModel, navController : NavController){
    if (viewModel.existingHousehold == true){
        if(!viewModel.gotHousehold && !viewModel.householdCreated) {
            FindHousehold(viewModel, Modifier, onBack = { navController.popBackStack() })
        } else {
            JoinHousehold(viewModel, Modifier, onBack = {
                viewModel.gotHousehold = false
                navController.popBackStack()
            })
        }
        if(viewModel.householdCreated){
            navController.navigate("Home")
        }

    }
    else if (viewModel.existingHousehold == false){
        NewHousehold(viewModel, navController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHousehold(viewModel: HouseholdViewModel, navController : NavController){
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (!viewModel.householdCreated) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (viewModel.setupStep == 0) {
                                    navController.popBackStack()
                                } else {
                                    viewModel.decrementStep()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Previous")
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        if (viewModel.setupStep < 4) {
                            Button(
                                onClick = { viewModel.incrementStep() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Next")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.createHousehold() },
                                enabled = !viewModel.isLoading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Create Household")
                            }
                        }
                    }
                }
            }
        },
        content = { innerPadding ->
            if (viewModel.householdCreated) {
                HouseholdCreated(viewModel, Modifier.padding(innerPadding), navController)
            } else {
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
    val isNameError = viewModel.hasAttemptedSubmit && viewModel.householdName.isBlank()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ){
        Text(
            "Name Your Household",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = viewModel.householdName,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("Household Name") },
                    placeholder = { Text("e.g., Downtown Apartment") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isNameError,
                    singleLine = true
                )
                if (isNameError) {
                    Text(
                        text = "Household name cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NewHouseholdChore(viewModel: HouseholdViewModel, modifier: Modifier){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ){
        Text(
            "Create Chores",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            "Define recurring chores for your household",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ){
            itemsIndexed(viewModel.choreInputs) { index, chore ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                "Chore ${index + 1}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (index > 0) {
                                OutlinedButton(onClick = {viewModel.removeChore(index)}) {
                                    Text("Remove")
                                }
                            }
                        }
                        ChoreSection(
                            chore = chore,
                            onChoreChanged = { updated ->
                                viewModel.updateChore(index, updated)
                            },
                            hasAttemptedSubmit = viewModel.hasAttemptedSubmit,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.addChore() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Add Another Chore", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun ChoreSection(
    chore: ChoreInput,
    onChoreChanged: (ChoreInput) -> Unit,
    hasAttemptedSubmit: Boolean,
) {
    val isNameError = hasAttemptedSubmit && chore.name.isBlank()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = chore.name,
            onValueChange = { onChoreChanged(chore.copy(name = it)) },
            label = { Text("Chore Name") },
            placeholder = { Text("e.g., Dishes") },
            modifier = Modifier.fillMaxWidth(),
            isError = isNameError,
            singleLine = true
        )
        if (isNameError) {
            Text(
                text = "Chore name cannot be empty",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedTextField(
            value = chore.description,
            onValueChange = { onChoreChanged(chore.copy(description = it)) },
            label = { Text("Description (Optional)") },
            placeholder = { Text("What needs to be done?") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
        )

        OutlinedTextField(
            value = if (chore.cycle==0) "" else chore.cycle.toString(),
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    onChoreChanged(chore.copy(cycle = newValue.toIntOrNull() ?: 0))
                }
            },
            label = { Text("Cycle (days)") },
            placeholder = { Text("How often?") },
            modifier = Modifier.fillMaxWidth(),
            isError = hasAttemptedSubmit && chore.cycle.toDouble() <= 0,
            singleLine = true
        )
        if (hasAttemptedSubmit && chore.cycle.toDouble() <= 0) {
            Text(
                text = "Cycle must be greater than 0",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun NewHouseholdPayment(viewModel: HouseholdViewModel, modifier: Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Text(
            "Recurring Payments",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            "Define recurring bills for your household",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(viewModel.paymentInputs) { index, payment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                "Payment ${index + 1}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (index > 0) {
                                OutlinedButton(onClick = {viewModel.removePayment(index)}) {
                                    Text("Remove")
                                }
                            }
                        }
                        PaymentSection(
                            payment = payment,
                            onPaymentChanged = { updated ->
                                viewModel.updatePayment(index, updated)
                            },
                            hasAttemptedSubmit = viewModel.hasAttemptedSubmit
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.addPayment() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Add Another Payment", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun PaymentSection(
    payment: PaymentInput,
    onPaymentChanged: (PaymentInput) -> Unit,
    hasAttemptedSubmit: Boolean,
) {
    val isNameError = hasAttemptedSubmit && payment.name.isBlank()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = payment.name,
            onValueChange = { onPaymentChanged(payment.copy(name = it)) },
            label = { Text("Payment Name") },
            placeholder = { Text("e.g., Rent") },
            modifier = Modifier.fillMaxWidth(),
            isError = isNameError,
            singleLine = true
        )
        if (isNameError) {
            Text(
                text = "Payment name cannot be empty",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedTextField(
            value = if (payment.amount == 0) "" else payment.amount.toString(),
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    onPaymentChanged(payment.copy(amount = newValue.toIntOrNull() ?: 0))
                }
            },
            label = { Text("Amount ($)") },
            placeholder = { Text("Total amount") },
            modifier = Modifier.fillMaxWidth(),
            isError = hasAttemptedSubmit && payment.amount.toDouble() <= 0,
            singleLine = true
        )
        if (hasAttemptedSubmit && payment.amount.toDouble() <= 0) {
            Text(
                text = "Amount must be greater than 0",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedTextField(
            value = if (payment.split == 0) "" else payment.split.toString(),
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    onPaymentChanged(payment.copy(split = newValue.toIntOrNull() ?: 0))
                }
            },
            label = { Text("Your Split (%)") },
            placeholder = { Text("Your percentage") },
            modifier = Modifier.fillMaxWidth(),
            isError = hasAttemptedSubmit && (payment.split.toDouble() !in 0.0..100.0),
            singleLine = true
        )
        if (hasAttemptedSubmit && (payment.split.toDouble() !in 0.0..100.0)) {
            Text(
                text = "Split must be between 0 and 100%",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedTextField(
            value = if (payment.cycle == 0) "" else payment.cycle.toString(),
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    onPaymentChanged(payment.copy(cycle = newValue.toIntOrNull() ?: 0))
                }
            },
            label = { Text("Cycle (days)") },
            placeholder = { Text("How often?") },
            modifier = Modifier.fillMaxWidth(),
            isError = hasAttemptedSubmit && payment.cycle.toDouble() <= 0,
            singleLine = true
        )
        if (hasAttemptedSubmit && payment.cycle.toDouble() <= 0) {
            Text(
                text = "Cycle must be greater than 0",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    "You pay this bill:",
                    style = MaterialTheme.typography.bodyMedium
                )
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
}

@Composable
fun NewHouseholdCalendar(viewModel: HouseholdViewModel, modifier: Modifier){
    val isNameError = viewModel.hasAttemptedSubmit && viewModel.calendarName.isBlank()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ){
        Text(
            "Shared Calendar",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            "Enter your shared Google calendar name",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = viewModel.calendarName,
                    onValueChange = { viewModel.updateCalendarName(it) },
                    label = { Text("Calendar Name") },
                    placeholder = { Text("e.g., Apartment Calendar") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isNameError,
                    singleLine = true
                )
                if (isNameError) {
                    Text(
                        text = "Calendar name cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewHouseholdDetails(viewModel: HouseholdViewModel, modifier: Modifier){
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                "Review Your Household",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Household Name",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        viewModel.householdName.ifBlank { "Not set" },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        item {
            Text(
                "Chores",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (viewModel.choreInputs.isEmpty()) {
            item {
                Text(
                    "No chores added.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            itemsIndexed(viewModel.choreInputs) { index, chore ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Chore ${index + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (viewModel.paymentInputs.isEmpty()) {
            item {
                Text(
                    "No payments added.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            itemsIndexed(viewModel.paymentInputs) { index, payment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Payment ${index + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Name: ${payment.name}")
                        Text("Amount: ${payment.amount}")
                        Text("Split: ${payment.split}%")
                        Text("Cycle: ${payment.cycle} days")
                        Text("Pays: ${if (payment.youPay) "You" else "Someone else"}")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Calendar Name",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        viewModel.calendarName.ifBlank { "Not set" },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdCreated(viewModel: HouseholdViewModel, modifier: Modifier, navController : NavController){
    val clipboardManager = LocalClipboardManager.current
    val householdId = viewModel.householdID

    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = { navController.navigate("Home") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Proceed to App", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding) // Apply Scaffold's inner padding
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ){
            Text(
                "Household Created!",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Household ID",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            householdId,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(householdId))
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.content_copy_24px),
                                contentDescription = "Copy Household ID",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        "Share this ID with your roommates so they can join",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FindHousehold(viewModel: HouseholdViewModel, modifier: Modifier, onBack: () -> Unit){
    var householdID by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_revert),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "Join Household",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = householdID,
                    onValueChange = { householdID = it },
                    label = { Text("Household ID") },
                    placeholder = { Text("Enter ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.getHouseholdForJoining(householdID) },
            enabled = !viewModel.isLoading && householdID.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ){
            Text("Search for Household", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun JoinHousehold(viewModel: HouseholdViewModel, modifier: Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    val error = viewModel.errorMessage

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.errorMessage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_revert),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = viewModel.householdName,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(viewModel.paymentsFromDB) { index, payment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Payment ${index + 1}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        PaymentItem(
                            payment = payment,
                            onPaymentChanged = { updated ->
                                viewModel.updatePaymentDB(index, updated)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.confirmJoinHousehold() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !viewModel.isLoading,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Confirm & Join Household", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun PaymentItem(
    payment: PaymentDB,
    onPaymentChanged: (PaymentDB) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = payment.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            "Occupied split: ${payment.occupiedSplit}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        OutlinedTextField(
            value = payment.split.toString(),
            onValueChange = { input ->
                val value = input.toIntOrNull() ?: 0
                onPaymentChanged(payment.copy(split = value))
            },
            label = { Text("Your Split (%)") },
            placeholder = { Text("Enter percentage") },
            modifier = Modifier.fillMaxWidth(),
            isError = payment.split.toDouble() !in 0.0..100.0,
            singleLine = true
        )

        if (payment.split.toDouble() + payment.occupiedSplit.toDouble() > 100) {
            Text(
                "Total split exceeds 100%",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if(payment.paid_by == "") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        "You pay this bill:",
                        style = MaterialTheme.typography.bodyMedium
                    )
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
    }
}