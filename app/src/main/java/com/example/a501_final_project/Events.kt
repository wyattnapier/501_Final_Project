package com.example.a501_final_project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun EventsScreen(modifier: Modifier, viewModel: MainViewModel) {
    Column {
        Text(
            text = "Events",
            style = MaterialTheme.typography.headlineLarge,
            modifier = modifier
        )
    }
}

@Preview
@Composable
fun EventsScreenPreview() {
    EventsScreen(Modifier.fillMaxSize(), viewModel())
}