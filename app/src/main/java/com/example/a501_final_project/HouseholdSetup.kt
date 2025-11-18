package com.example.a501_final_project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.a501_final_project.ui.theme._501_Final_ProjectTheme

@Composable
fun NewHousehold(){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight()
    ){
        Text(
            "New Household",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("Create a Household Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { /*TODO*/ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Household")
        }
    }
}

@Composable
fun NewHouseholdChore(viewModel: HouseholdViewModel){
    var chores by rememberSaveable { mutableStateOf(viewModel.choreInputs) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxHeight()
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
            value = chore.cycle,
            onValueChange = { onChoreChanged(chore.copy(cycle = it)) },
            label = { Text("Cycle (in days)") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}



@Preview(showBackground = true)
@Composable
fun HouseholdPreview() {
    _501_Final_ProjectTheme {
        val viewModel = remember { HouseholdViewModel() }
        NewHouseholdChore(viewModel)
    }
}
