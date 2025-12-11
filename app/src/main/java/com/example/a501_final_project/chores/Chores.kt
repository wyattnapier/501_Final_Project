package com.example.a501_final_project.chores

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.a501_final_project.MainViewModel
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChoresScreen(mainViewModel: MainViewModel, choresViewModel: ChoresViewModel, modifier: Modifier = Modifier){
    val chores by choresViewModel.choresList.collectAsState()
    val showPrevChores by choresViewModel.showPrevChores.collectAsState()
    val userId by mainViewModel.userId.collectAsState()
    val sharedHouseholdID by mainViewModel.householdId.collectAsState()

    val upcomingChores = choresViewModel.getUpcomingChores(chores)

    val context = LocalContext.current

    Log.d("ChoresScreen", "userId: $userId")
    Log.d("ChoresScreen", "sharedHouseholdID: $sharedHouseholdID")

    // Capture values in local variables for smart casting
    val currentUserId = userId
    val currentHouseholdId = sharedHouseholdID

    // Now you can smart cast the local variables
    if (currentUserId == null || currentHouseholdId == null) {
        Box(
            modifier = modifier.fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Loading chores data...",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        return
    }

    // At this point, Kotlin knows currentUserId and currentHouseholdId are non-null
    Column(modifier = modifier
        .fillMaxHeight()
        .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)){
        if (showPrevChores) {
            PrevChores(chores, context, choresViewModel)
        } else {
            MyChoreWidget(currentUserId, currentHouseholdId, upcomingChores, choresViewModel, context, Modifier.weight(1f))
            RoommateChores(currentUserId, currentHouseholdId, upcomingChores, choresViewModel, context, Modifier.weight(1f))
        }
    }
}

@Composable
fun MyChoreWidget(userID: String, householdID: String, chores: List<Chore>, choresViewModel: ChoresViewModel, context: Context, modifier: Modifier = Modifier){
    val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)

    var myChores = chores.filter { it.assignedToId == userID && it.householdID == householdID }

    val incompleteChores = myChores.filter {!it.completed}.sortedBy {
        try {
            dateFormat.parse(it.dueDate)
        } catch (e: ParseException) {
            Date(Long.MAX_VALUE)
        }
    }
    val completeChores = myChores.filter {it.completed}

    myChores = incompleteChores + completeChores

    // Get URIs from ViewModel
    val tempImageUri by choresViewModel.tempImageUri.collectAsState()

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(10.dp)
    ) {
        Text("My Chores", fontSize = MaterialTheme.typography.headlineMedium.fontSize)

        Spacer(modifier = Modifier.height(8.dp))

        if (myChores.isEmpty()){
            Text("No chores assigned!", fontSize = MaterialTheme.typography.bodyLarge.fontSize)
        } else{
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ){
                items(myChores.size) { index ->
                    val chore = myChores[index]

                    val cameraLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.TakePicture(),
                        onResult = { success ->
                            if (success && tempImageUri != null) {
                                // Photo captured successfully
                                Toast.makeText(context, "Photo captured! Chore marked as complete.", Toast.LENGTH_SHORT).show()
                                choresViewModel.completeChoreWithPhoto(chore, tempImageUri!!, context)
                            } else {
                                Toast.makeText(context, "Photo capture cancelled.", Toast.LENGTH_SHORT).show()
                                choresViewModel.clearTempImageUri()
                            }
                        }
                    )

                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted: Boolean ->
                        if (isGranted) {
                            val uri = createImageUri(context)
                            choresViewModel.setTempImageUri(uri)
                            cameraLauncher.launch(uri)
                        } else {
                            Toast.makeText(context, "Camera permission is required to take a picture.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    MyChoreItem(chore, context, cameraLauncher, permissionLauncher, choresViewModel)

                    if (index < myChores.lastIndex) {
                        HorizontalDivider( modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MyChoreItem(
    chore: Chore,
    context: Context,
    cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    choresViewModel: ChoresViewModel,
){
    val isOverdue = choresViewModel.isChoreOverdue(chore)

    val choreImageUris by choresViewModel.choreImageUris.collectAsState<Map<String, Uri>>()
    val capturedImageUri = chore.let { choreImageUris[it.choreID] }

    if(chore.completed) {
        LaunchedEffect(chore.choreID) {
            choresViewModel.getChoreImageUri(chore.choreID, chore.householdID)
        }
    }

    // First Row: Task info and button
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(2f, true)) {
            Text(chore.name, fontSize = MaterialTheme.typography.bodyLarge.fontSize)
            Text(
                "Due Date: ${chore.dueDate}",
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                color = if (isOverdue && !chore.completed) Color.Red else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Column(
            modifier = Modifier.weight(1f, true),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) -> {
                            val uri = createImageUri(context)
                            choresViewModel.setTempImageUri(uri)
                            cameraLauncher.launch(uri)
                        }
                        else -> {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                },
                enabled = !chore.completed
            ) {
                val buttonText = if (chore.completed) "Chore Completed" else "Complete with Photo"
                Text(text = buttonText, textAlign = TextAlign.Center)
            }
        }
    }

    // Second Row: Captured photo (only shown if photo exists)
    capturedImageUri?.let { uri ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(top = 6.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(Color.LightGray)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(uri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Chore completion proof",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = {
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) -> {
                            val newUri = createImageUri(context)
                            choresViewModel.setTempImageUri(newUri)
                            cameraLauncher.launch(newUri)
                        }
                        else -> {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retake Photo",
                    tint = Color.White // Makes the icon itself white
                )
            }
        }
    }
}


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
fun RoommateChores(userID: String, householdID: String?, chores: List<Chore>, choresViewModel: ChoresViewModel, context: Context, modifier: Modifier = Modifier){
    val roommateChores = chores.filter { it.assignedToId != userID && it.householdID == householdID }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(10.dp)
    ) {
        Text("Roommate Chores", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
        LazyColumn() {
            for (chore in roommateChores) {
                item {
                    RoommateChoreItem(chore, context, choresViewModel)
                    HorizontalDivider()
                }
            }
            item {
                Row(
                    modifier = Modifier.fillParentMaxWidth().padding(4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { choresViewModel.toggleShowPrevChores() }) {
                        Text("See Previous Chores")
                    }
                }
            }
        }
    }
}

@Composable
fun RoommateChoreItem(
    chore: Chore,
    context: Context,
    choresViewModel: ChoresViewModel,
    modifier: Modifier = Modifier
) {
    //the fetched image url
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(key1 = chore.choreID) {
        if (chore.completed) {
            imageUri = choresViewModel.getChoreImageUri(chore.choreID, chore.householdID)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier.weight(3f)) {
            Text(
                chore.assignedToName + ": " + chore.name,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize
            )
            Text(
                text = if (chore.completed) {
                    "Status: Completed"
                } else {
                    "Status: Incomplete"
                },
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                color = if (choresViewModel.isChoreOverdue(chore) && !chore.completed) Color.Red else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        imageUri?.let {
            AsyncImage(
                model = ImageRequest.Builder(context).data(it).crossfade(true).build(),
                contentDescription = "Proof for ${chore.name}",
                modifier = Modifier.weight(1f).size(64.dp).clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun PrevChores(
    chores: List<Chore>,
    context: Context,
    choresViewModel: ChoresViewModel,
    modifier: Modifier = Modifier
) {
    // if due date < today or complete, display on prev tasks
    val prevChores = chores.filter { choresViewModel.isChoreOverdue(it) || it.completed }
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Previous Chores", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = { choresViewModel.toggleShowPrevChores() }) {
                Text("Back to Current")
            }
        }
        LazyColumn() {
            for (chore in prevChores) {
                item {
                    PrevChoreItem(chore, context, choresViewModel, modifier)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun PrevChoreItem(
    chore: Chore,
    context: Context,
    viewModel: ChoresViewModel,
    modifier: Modifier = Modifier
) {
    //the fetched image url
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(key1 = chore.choreID) {
        if (chore.completed) {
            imageUri = viewModel.getChoreImageUri(chore.choreID, chore.householdID)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier.weight(3f)) {
            Text(
                chore.assignedToName + ": " + chore.name,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize
            )
            Text(
                text = if (chore.completed) {
                    "Status: Completed"
                } else {
                    "Status: Incomplete"
                },
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                color = if (viewModel.isChoreOverdue(chore) && !chore.completed) Color.Red else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        imageUri?.let {
            AsyncImage(
                model = ImageRequest.Builder(context).data(it).crossfade(true).build(),
                contentDescription = "Proof for ${chore.name}",
                modifier = Modifier.weight(1f).size(64.dp).clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
        }
    }
}