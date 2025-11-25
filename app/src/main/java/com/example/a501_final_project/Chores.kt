package com.example.a501_final_project

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import coil.compose.AsyncImage
import com.example.a501_final_project.BoxItem
import com.example.a501_final_project.ui.theme._501_Final_ProjectTheme
import com.google.android.libraries.places.api.model.LocalDate
import java.io.File
import java.util.*

data class ChoreTemp(
    val name: String,
    val description: String,
    val assignedTo: String,
    val houseHoldID: Number,
    val userID: Number,
    val dueDate: String,
    val status: String = "Pending"
)

@Composable
fun ChoresScreen(viewModel: MainViewModel, modifier: Modifier = Modifier){
    val chores by viewModel.choresList.collectAsState()
    val showPrevChores by viewModel.showPrevChores.collectAsState()
    Column(modifier = modifier
        .fillMaxHeight()
        .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)){
        if (showPrevChores) {
            PrevChores(chores, viewModel)
        }else {
            MyChoreWidget(chores, viewModel)
            RoommateChores(chores, viewModel)
        }
    }
}

// 1. First, update your MyChoreWidget composable:

@Composable
fun MyChoreWidget(chores: List<Chore>, viewModel: MainViewModel, modifier: Modifier = Modifier){
    val chore = chores.find { it.userID == viewModel.userID && it.householdID == viewModel.householdID }
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && imageUri != null) {
                // Photo captured successfully
                Toast.makeText(context, "Photo captured! Chore marked as complete.", Toast.LENGTH_SHORT).show()
                viewModel.completeChore(chore!!)
                // Keep the imageUri so it displays
            } else {
                Toast.makeText(context, "Photo capture cancelled.", Toast.LENGTH_SHORT).show()
                imageUri = null
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val uri = createImageUri(context)
            imageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission is required to take a picture.", Toast.LENGTH_SHORT).show()
        }
    }

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(2f, true)) {
            Text("My Chore", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
            Text(chore?.name ?: "No chore assigned", fontSize = MaterialTheme.typography.bodyLarge.fontSize)
            Text(
                "Due Date: ${chore?.dueDate}",
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                color = if (chore?.completed == true) Color.Green else MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Display the captured image
            imageUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Chore completion proof",
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f, true),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (chore?.completed == true) {
                        Toast.makeText(context, "Chore already completed!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.CAMERA
                        ) -> {
                            val uri = createImageUri(context)
                            imageUri = uri
                            cameraLauncher.launch(uri)
                        }
                        else -> {
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }
                },
                enabled = chore?.completed != true
            ) {
                Text("Complete with Photo")
            }
        }
    }
}

// 2. Update the createImageUri function to use unique filenames:

private fun createImageUri(context: Context): Uri {
    val timestamp = System.currentTimeMillis()
    val imageFile = File(context.filesDir, "chore_photo_$timestamp.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        imageFile
    )
}


@Composable
fun RoommateChores(chores: List<Chore>, viewModel: MainViewModel, modifier: Modifier = Modifier){
    val roommateChores = chores.filter { it.userID != viewModel.userID && it.householdID == viewModel.householdID }

    Column(modifier =  modifier
        .fillMaxHeight()
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.secondaryContainer)
        .padding(10.dp)){
        Text("Roommate Chores", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
        LazyColumn(){
            for(chore in roommateChores) {
                item {
                    Text(chore.assignedTo + ": " + chore.name, fontSize = MaterialTheme.typography.bodyLarge.fontSize)
                    Text(text = if (chore.completed) {"Status: Completed"} else {"Status: Pending"}, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                    HorizontalDivider(
                        color = Color.LightGray,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            item {
                Row( modifier = Modifier.fillParentMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(onClick = { viewModel.toggleShowPrevChores() }) {
                        Text("See Previous Chores")
                    }
                }
            }
        }
    }
}

@Composable
fun PrevChores(chores: List<Chore>, viewModel: MainViewModel) {
    // if due date < today, display on prev tasks
    Column(modifier =  Modifier
        .fillMaxHeight()
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.secondaryContainer)
        .padding(10.dp)){
        Text("Previous Chores", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
        LazyColumn(){
            for(chore in chores) {
                item {
                    Text(chore.assignedTo + ": " + chore.name, fontSize = MaterialTheme.typography.bodyLarge.fontSize)
                    Text(text = if (chore.completed) {"Status: Completed"} else {"Status: Pending"}, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                    HorizontalDivider(
                        color = Color.LightGray,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            item {
                Row( modifier = Modifier.fillParentMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(onClick = { viewModel.toggleShowPrevChores() }) {
                        Text("Back to Current Chores")
                    }
                }

            }
        }
    }
}


//@Preview(showBackground = true)
//@Composable
//fun ChoresPreview() {
//    val previewVM = MainViewModel()
//    _501_Final_ProjectTheme {
//        ChoresScreen(previewVM)
//    }
//}
