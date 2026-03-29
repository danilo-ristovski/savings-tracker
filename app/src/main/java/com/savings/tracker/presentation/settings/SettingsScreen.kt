package com.savings.tracker.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.savings.tracker.data.biometric.BiometricHelper
import com.savings.tracker.domain.model.AnalysisSection
import com.savings.tracker.domain.model.Category
import com.savings.tracker.domain.model.CategoryType
import com.savings.tracker.domain.model.ThemeMode
import com.savings.tracker.presentation.trends.ChartType
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
    val context = LocalContext.current
    var showResetPinDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var isConfirmStep by remember { mutableStateOf(false) }
    var resetPinError by remember { mutableStateOf<String?>(null) }
    var showDemoModeDialog by remember { mutableStateOf(false) }
    var infoDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Category state
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var deletingCategory by remember { mutableStateOf<Category?>(null) }

    val biometricAvailable = remember { BiometricHelper.isBiometricAvailable(context) }

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
                        ThemeMode.LIGHT to "Light",
                        ThemeMode.DARK to "Dark",
                        ThemeMode.SYSTEM to "System",
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        modes.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = uiState.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = modes.size,
                                ),
                                icon = {},
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

            // Categories Section
            SectionHeader("Transaction Categories")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    uiState.categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (category.notes.isNotEmpty()) {
                                    Text(
                                        text = category.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (category.isPredefined) {
                                    Text(
                                        text = "Predefined",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            IconButton(onClick = { editingCategory = category }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                            }
                            if (!category.isPredefined) {
                                IconButton(onClick = { deletingCategory = category }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    ElevatedButton(
                        onClick = { showAddCategoryDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Add Category")
                    }
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

                    // Biometric toggle
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Biometric Login",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = if (biometricAvailable) "Use fingerprint or face to unlock" else "Not available on this device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.isBiometricEnabled,
                            onCheckedChange = { viewModel.setBiometricEnabled(it) },
                            enabled = biometricAvailable,
                        )
                    }

                    // Shake to logout toggle
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Shake to Logout",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "Shake your phone to lock the app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.isShakeLogoutEnabled,
                            onCheckedChange = { viewModel.setShakeLogoutEnabled(it) },
                        )
                    }

                    // Auto blur toggle
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-blur after inactivity",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "Blurs the screen after 15 seconds of inactivity",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.isAutoBlurEnabled,
                            onCheckedChange = { viewModel.setAutoBlurEnabled(it) },
                        )
                    }
                }
            }

            // Analysis Sections
            SectionHeader("Analysis Sections")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Choose which sections to show in the Analysis tab",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val allAnalysisVisible = AnalysisSection.entries.none { it.name in uiState.analysisHiddenSections }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Show all", style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        Switch(
                            checked = allAnalysisVisible,
                            onCheckedChange = { showAll -> viewModel.setAllAnalysisSectionsVisible(showAll) },
                        )
                    }
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    AnalysisSection.entries.forEach { section ->
                        val sectionDescription = when (section) {
                            AnalysisSection.STATISTICS -> "Count comparison of deposits vs withdrawals, plus average deposit and withdrawal amounts."
                            AnalysisSection.DEPOSITS_FREQUENCY -> "Which days of the week you deposit most frequently, shown as a bar chart."
                            AnalysisSection.MONTHLY_BREAKDOWN -> "Deposits and withdrawals per month with net change highlighted in green or red."
                            AnalysisSection.INCOME_FREQUENCY -> "How often you make deposits on average — daily, weekly, and monthly rates."
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = section.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                IconButton(
                                    onClick = { infoDialog = section.displayName to sectionDescription },
                                    modifier = Modifier.size(20.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "More info",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            Switch(
                                checked = section.name !in uiState.analysisHiddenSections,
                                onCheckedChange = { viewModel.toggleAnalysisSection(section.name) },
                            )
                        }
                    }
                }
            }

            // Data Management Section
            // Chart Types Section
            SectionHeader("Chart Types")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Choose which chart types are available on the Charts tab",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val allChartTypesVisible = ChartType.entries.none { it.name in uiState.hiddenChartTypes }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Show all", style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        Switch(
                            checked = allChartTypesVisible,
                            onCheckedChange = { showAll -> viewModel.setAllChartTypesVisible(showAll) },
                        )
                    }
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    ChartType.entries.forEach { ct ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(ct.displayName, style = MaterialTheme.typography.bodyMedium)
                                IconButton(
                                    onClick = { infoDialog = ct.displayName to ct.description },
                                    modifier = Modifier.size(20.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "More info",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            Switch(
                                checked = ct.name !in uiState.hiddenChartTypes,
                                onCheckedChange = { viewModel.toggleChartType(ct.name) },
                            )
                        }
                    }
                }
            }

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

    // Add category dialog
    if (showAddCategoryDialog) {
        CategoryDialog(
            title = "Add Category",
            autoFocus = true,
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, notes, type ->
                viewModel.addCategory(name, notes, type)
                showAddCategoryDialog = false
            },
        )
    }

    // Edit category dialog
    editingCategory?.let { category ->
        CategoryDialog(
            title = "Edit Category",
            initialName = category.name,
            initialNotes = category.notes,
            initialType = category.type,
            isEditing = true,
            onDismiss = { editingCategory = null },
            onConfirm = { name, notes, type ->
                viewModel.updateCategory(category.copy(name = name, notes = notes, type = type))
                editingCategory = null
            },
        )
    }

    // Info dialog for toggle items
    infoDialog?.let { (title, message) ->
        AlertDialog(
            onDismissRequest = { infoDialog = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { infoDialog = null }) { Text("Got it") }
            },
        )
    }

    // Delete category confirmation
    deletingCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Delete Category?") },
            text = { Text("Are you sure you want to delete \"${category.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(category)
                        deletingCategory = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun CategoryDialog(
    title: String,
    initialName: String = "",
    initialNotes: String = "",
    initialType: CategoryType = CategoryType.ANY,
    isEditing: Boolean = false,
    autoFocus: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (name: String, notes: String, type: CategoryType) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var notes by remember { mutableStateOf(initialNotes) }
    var selectedType by remember { mutableStateOf(initialType) }
    var nameError by remember { mutableStateOf<String?>(null) }
    val nameFocusRequester = remember { FocusRequester() }

    if (autoFocus) {
        LaunchedEffect(Unit) {
            nameFocusRequester.requestFocus()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { input ->
                        // Auto-format: lowercase, replace spaces with underscores
                        name = input.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")
                        nameError = null
                    },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocusRequester),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Applies to", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                if (isEditing) {
                    val typeLabel = when (selectedType) {
                        CategoryType.DEPOSIT -> "Deposit"
                        CategoryType.WITHDRAWAL -> "Withdrawal"
                        CategoryType.ANY -> "Any"
                    }
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    val typeOptions = listOf(CategoryType.DEPOSIT, CategoryType.ANY, CategoryType.WITHDRAWAL)
                    val typeLabels = listOf("Deposit", "Any", "Withdrawal")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        typeOptions.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = selectedType == option,
                                onClick = { selectedType = option },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = typeOptions.size),
                                icon = {},
                                label = { Text(typeLabels[index]) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "Name is required"
                    } else {
                        onConfirm(name, notes, selectedType)
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}
