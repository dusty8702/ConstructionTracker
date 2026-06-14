package com.constructiontracker.ui.screens.setup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(viewModel: SetupViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Contractors",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Edit contractor names and contract types.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                items(uiState.contractors) { contractor ->
                    ContractorEditCard(
                        state = contractor,
                        onNameChange = { viewModel.updateName(contractor.id, it) },
                        onTypeChange = { viewModel.updateContractType(contractor.id, it) },
                        onAmountChange = { viewModel.updateContractAmount(contractor.id, it) },
                        onSave = { viewModel.saveContractor(contractor.id) },
                        onSnackbar = { viewModel.clearSaveSuccess(contractor.id) },
                        snackbarHostState = snackbarHostState
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Add Contractor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Add a new contractor to track payments for.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                item {
                    AddContractorCard(
                        state = uiState.addContractor,
                        onNameChange = viewModel::updateNewName,
                        onTypeChange = viewModel::updateNewContractType,
                        onAmountChange = viewModel::updateNewContractAmount,
                        onContactNumberChange = viewModel::updateNewContactNumber,
                        onBankAccountNumberChange = viewModel::updateNewBankAccountNumber,
                        onBankNameChange = viewModel::updateNewBankName,
                        onBankBranchChange = viewModel::updateNewBankBranch,
                        onPhotoSelected = viewModel::updateNewPhotoUri,
                        onAdd = viewModel::addContractor,
                        onSnackbar = viewModel::clearAddSuccess,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddContractorCard(
    state: AddContractorState,
    onNameChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onContactNumberChange: (String) -> Unit,
    onBankAccountNumberChange: (String) -> Unit,
    onBankNameChange: (String) -> Unit,
    onBankBranchChange: (String) -> Unit,
    onPhotoSelected: (String) -> Unit,
    onAdd: () -> Unit,
    onSnackbar: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar("Contractor added!")
            onSnackbar()
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // --- Basic info ---
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text("Contractor Name *") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Contract Type", style = MaterialTheme.typography.labelMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.contractType == "OPEN_ENDED",
                    onClick = { onTypeChange("OPEN_ENDED") },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Open Ended") }
                SegmentedButton(
                    selected = state.contractType == "FIXED",
                    onClick = { onTypeChange("FIXED") },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Fixed Contract") }
            }

            if (state.contractType == "FIXED") {
                OutlinedTextField(
                    value = state.contractAmount,
                    onValueChange = onAmountChange,
                    label = { Text("Contract Amount (LKR)") },
                    prefix = { Text("Rs. ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // --- Contact ---
            OutlinedTextField(
                value = state.contactNumber,
                onValueChange = onContactNumberChange,
                label = { Text("Contact Number") },
                placeholder = { Text("+94 71 234 5678") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = state.contactNumberError != null,
                supportingText = state.contactNumberError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth()
            )

            // --- Bank details ---
            Text(
                text = "Bank Details",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            OutlinedTextField(
                value = state.bankAccountNumber,
                onValueChange = onBankAccountNumberChange,
                label = { Text("Bank Account Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.bankName,
                onValueChange = onBankNameChange,
                label = { Text("Bank Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.bankBranch,
                onValueChange = onBankBranchChange,
                label = { Text("Bank Branch") },
                modifier = Modifier.fillMaxWidth()
            )

            // --- Photo ---
            PhotoPickerSection(
                photoUri = state.photoUri,
                onPhotoSelected = onPhotoSelected
            )

            // --- Submit ---
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving && state.name.isNotBlank()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Add Contractor", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoPickerSection(photoUri: String, onPhotoSelected: (String) -> Unit) {
    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraLaunch by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraUri?.let { onPhotoSelected(it.toString()) }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            onPhotoSelected(it.toString())
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingCameraLaunch = true
    }

    LaunchedEffect(pendingCameraLaunch) {
        if (pendingCameraLaunch) {
            val dir = File(context.filesDir, "contractor_photos").also { it.mkdirs() }
            val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            cameraUri = uri
            cameraLauncher.launch(uri)
            pendingCameraLaunch = false
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val dir = File(context.filesDir, "contractor_photos").also { it.mkdirs() }
            val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val bitmap by produceState<ImageBitmap?>(null, photoUri) {
        value = if (photoUri.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(Uri.parse(photoUri))?.use {
                        BitmapFactory.decodeStream(it)?.asImageBitmap()
                    }
                } catch (_: Exception) { null }
            }
        } else null
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Contractor Photo", style = MaterialTheme.typography.labelMedium)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = "Contractor photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { launchCamera() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Camera")
            }
            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Photo, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Gallery")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContractorEditCard(
    state: ContractorEditState,
    onNameChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onSave: () -> Unit,
    onSnackbar: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar("${state.name} saved!")
            onSnackbar("saved")
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text("Contractor Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Contract Type", style = MaterialTheme.typography.labelMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.contractType == "OPEN_ENDED",
                    onClick = { onTypeChange("OPEN_ENDED") },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Open Ended") }
                SegmentedButton(
                    selected = state.contractType == "FIXED",
                    onClick = { onTypeChange("FIXED") },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Fixed Contract") }
            }

            if (state.contractType == "FIXED") {
                OutlinedTextField(
                    value = state.contractAmount,
                    onValueChange = onAmountChange,
                    label = { Text("Contract Amount (LKR)") },
                    prefix = { Text("Rs. ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving && state.name.isNotBlank()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
