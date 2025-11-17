package com.example.a501_final_project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
            label = { Text("Create a Household Name") }
        )
        Button(onClick = { /*TODO*/ }) {
            Text("Create Household")
        }
    }
}

@Composable
fun NewHouseholdChore(){

}


@Preview(showBackground = true)
@Composable
fun HouseholdPreview() {
    _501_Final_ProjectTheme {
        NewHousehold()
    }
}
