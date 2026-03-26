package com.savings.tracker.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.savings.tracker.domain.model.ThemeMode
import com.savings.tracker.presentation.pin.PinDots
import com.savings.tracker.presentation.pin.PinKeypad
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val PIN_LENGTH = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showResetPinDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var isConfirmStep by remember { mutableStateOf(false) }
    var resetPinError by remember { mutableStateOf<String?>(null) }
    var showDemoModeDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportDataToUri(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importData(it) }
    }

    LaunchedEffect(uiState.exportResult) {
        uiState.exportResult?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearExportResult()
        }
    }

    LaunchedEffect(uiState.importResult) {
        uiState.importResult?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearImportResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Appearance Section
            SectionHeader("Appearance")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val modes = listOf(
                        Triple(ThemeMode.LIGHT, "Light", Icons.Default.LightMode),
                        Triple(ThemeMode.DARK, "Dark", Icons.Default.DarkMode),
                        Triple(ThemeMode.SYSTEM, "System", Icons.Default.SettingsBrightness),
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        modes.forEachIndexed { index, (mode, label, icon) ->
                            SegmentedButton(
                                selected = uiState.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = modes.size,
                                ),
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = uiState.themeMode == mode) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                        )
                                    }
                                },
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }

            // Bank Fees Section
            SectionHeader("Bank Fees")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = uiState.monthlyFee,
                        onValueChange = { viewModel.setMonthlyFee(it) },
                        label = { Text("Monthly Fee") },
                        suffix = { Text("RSD") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This amount is deducted from your savings at the start of each month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Security Section
            SectionHeader("Security")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Change your 6-digit PIN") } },
                        state = rememberTooltipState(),
                    ) {
                        ElevatedButton(
                            onClick = {
                                newPin = ""
                                confirmNewPin = ""
                                isConfirmStep = false
                                resetPinError = null
                                showResetPinDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Reset PIN")
                        }
                    }
                }
            }

            // Data Management Section
            SectionHeader("Data Management")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Export all data as a PIN-encrypted backup") } },
                        state = rememberTooltipState(),
                    ) {
                        ElevatedButton(
                            onClick = {
                                val timestamp = LocalDateTime.now()
                                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                                exportLauncher.launch("savings_backup_$timestamp.zip")
                            },
                            enabled = !uiState.isExporting,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (uiState.isExporting) "Exporting..." else "Export / Backup Data")
                        }
                    }

                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Import data from a backup file") } },
                        state = rememberTooltipState(),
                    ) {
                        ElevatedButton(
                            onClick = { importLauncher.launch(arrayOf("application/zip")) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Import Data")
                        }
                    }

                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Permanently delete all transactions and settings") } },
                        state = rememberTooltipState(),
                    ) {
                        ElevatedButton(
                            onClick = { viewModel.requestDeleteAllData() },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Reset Data")
                        }
                    }
                }
            }

            // Demo Mode Section
            SectionHeader("Demo Mode")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (uiState.isDemoMode) "Demo mode is ON" else "Demo mode is OFF",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Toggle demo mode with sample data") } },
                        state = rememberTooltipState(),
                    ) {
                        ElevatedButton(
                            onClick = { showDemoModeDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (uiState.isDemoMode) "Exit Demo Mode" else "Enter Demo Mode")
                        }
                    }
                }
            }
        }
    }

    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Delete All Data?") },
            text = {
                Text("This will permanently remove all transactions and settings. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showResetPinDialog) {
        val activePin = if (isConfirmStep) confirmNewPin else newPin

        AlertDialog(
            onDismissRequest = { showResetPinDialog = false },
            title = { Text("Reset PIN") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (isConfirmStep) "Confirm your new PIN" else "Enter new PIN",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PinDots(filledCount = activePin.length, total = PIN_LENGTH)
                    Spacer(modifier = Modifier.height(16.dp))

                    resetPinError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    PinKeypad(
                        onDigit = { digit ->
                            resetPinError = null
                            if (isConfirmStep) {
                                if (confirmNewPin.length < PIN_LENGTH) {
                                    confirmNewPin += digit
                                    if (confirmNewPin.length == PIN_LENGTH) {
                                        if (confirmNewPin == newPin) {
                                            viewModel.resetPin(newPin)
                                            showResetPinDialog = false
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("PIN has been reset successfully")
                                            }
                                        } else {
                                            resetPinError = "PINs don't match"
                                            confirmNewPin = ""
                                        }
                                    }
                                }
                            } else {
                                if (newPin.length < PIN_LENGTH) {
                                    newPin += digit
                                    if (newPin.length == PIN_LENGTH) {
                                        isConfirmStep = true
                                    }
                                }
                            }
                        },
                        onBackspace = {
                            if (isConfirmStep) {
                                if (confirmNewPin.isNotEmpty()) {
                                    confirmNewPin = confirmNewPin.dropLast(1)
                                }
                            } else {
                                if (newPin.isNotEmpty()) {
                                    newPin = newPin.dropLast(1)
                                }
                            }
                            resetPinError = null
                        },
                        onClear = {
                            if (isConfirmStep) {
                                confirmNewPin = ""
                            } else {
                                newPin = ""
                            }
                            resetPinError = null
                        },
                        enabled = true,
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showResetPinDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showDemoModeDialog) {
        val enteringDemo = !uiState.isDemoMode
        AlertDialog(
            onDismissRequest = { showDemoModeDialog = false },
            title = { Text(if (enteringDemo) "Enter Demo Mode?" else "Exit Demo Mode?") },
            text = {
                Text(
                    if (enteringDemo) {
                        "Demo mode will load sample data for demonstration purposes."
                    } else {
                        "Exiting demo mode will remove the sample data."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.toggleDemoMode()
                        showDemoModeDialog = false
                    },
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDemoModeDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}
