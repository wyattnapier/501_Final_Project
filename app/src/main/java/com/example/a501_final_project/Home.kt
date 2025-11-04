package com.example.a501_final_project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.navigation.NavController

/**
 * composable for the home screen
 * creates the 4 boxes as previews of the main screens
 * TODO: decide to keep/delete locate and rearrange accordingly
 * TODO: fill in the the screens with real content (build composables for each widget)
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier, navController: NavController) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            BoxItem(
                "Calendar",
                MaterialTheme.colorScheme.primaryContainer,
                onClick = { navController.navigate(Screen.Calendar.route) }
            )
            BoxItem(
                "Payment",
                MaterialTheme.colorScheme.secondaryContainer,
                onClick = { navController.navigate(Screen.Pay.route) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .weight(1f),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            BoxItem(
                "Chores",
                MaterialTheme.colorScheme.tertiaryContainer,
                onClick = { navController.navigate(Screen.Chores.route) }
            )
            BoxItem(
                "Locate/TBD",
                MaterialTheme.colorScheme.errorContainer,
                onClick = { navController.navigate(Screen.Error.route) }
            )
        }
    }
}

@Composable
fun RowScope.BoxItem(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(weight = 1f)
            .fillMaxSize()
            .padding(4.dp)
            .background(
                color = color,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}