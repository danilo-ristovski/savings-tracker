package com.savings.tracker.presentation.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.savings.tracker.domain.model.TransactionType
import com.savings.tracker.navigation.Routes
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionType by remember { mutableStateOf(TransactionType.DEPOSIT) }
    val snackbarHostState = remember { SnackbarHostState() }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm")
    }

    DisposableEffect(Unit) {
        viewModel.resetInactivityTimer()
        onDispose { viewModel.cancelInactivityTimer() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.resetInactivityTimer()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Observe cross-screen snackbar messages
    LaunchedEffect(Unit) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
        savedStateHandle?.getLiveData<String>("snackbar_message")?.observeForever { message ->
            if (!message.isNullOrEmpty()) {
                savedStateHandle.remove<String>("snackbar_message")
            }
        }
    }

    val currentEntry = navController.currentBackStackEntry
    val snackbarMessage = currentEntry?.savedStateHandle?.get<String>("snackbar_message")
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            currentEntry.savedStateHandle?.remove<String>("snackbar_message")
        }
    }

    var showLogoutConfirm by remember { mutableStateOf(false) }

    // Auto-logout when inactivity dialog times out
    val shouldNavigateToPin by viewModel.shouldNavigateToPin.collectAsState()
    LaunchedEffect(shouldNavigateToPin) {
        if (shouldNavigateToPin) {
            viewModel.consumeNavigateToPin()
            navController.navigate(Routes.PIN_LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Inactivity blur — timer is managed in the ViewModel's stable coroutine scope
    val inactivityTriggered by viewModel.inactivityTriggered.collectAsState()
    val dialogShowing = showLogoutConfirm || state.currentTip != null || (inactivityTriggered && state.autoBlurEnabled)

    val blurRadius: Dp by animateDpAsState(
        targetValue = if (dialogShowing) 16.dp else 0.dp,
        animationSpec = tween(durationMillis = 700),
        label = "blur",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            viewModel.resetInactivityTimer()
                        }
                    }
                }
            },
    ) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Savings Tracking",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider()
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    NavigationBarItem(
                        selected = false,
                        onClick = { viewModel.showNextTip() },
                        icon = {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                        },
                        label = { Text("Tip") },
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate(Routes.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { showLogoutConfirm = true },
                        icon = {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        label = {
                            Text("Logout", color = MaterialTheme.colorScheme.error)
                        },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .blur(blurRadius),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Routes.TRENDS) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Current Balance",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${formatAmountRsd(state.balance)} RSD",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        state.lastUpdateDate?.let { date ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Last updated: ${date.format(dateFormatter)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }

                        state.lastChange?.let { change ->
                            Spacer(modifier = Modifier.height(4.dp))
                            val isPositive = state.lastChangeType == TransactionType.DEPOSIT
                            val sign = if (isPositive) "+" else "-"
                            val color = if (isPositive) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                            Text(
                                text = "Last change: $sign${formatAmountRsd(change)} RSD",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = color
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap to view trends",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                        )
                    }
                }

                // Next fee payment info
                if (state.monthlyFee > 0) {
                    val now = LocalDate.now()
                    val nextFeeDate = if (now.dayOfMonth == 1) now else now.withDayOfMonth(1).plusMonths(1)
                    val daysUntil = ChronoUnit.DAYS.between(now, nextFeeDate)
                    val feeInfoText = when {
                        daysUntil == 0L -> "Today"
                        daysUntil == 1L -> "Tomorrow"
                        else -> {
                            val feeDateFormatted = nextFeeDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy."))
                            "$feeDateFormatted ($daysUntil days)"
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "Next bank fee",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = feeInfoText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                text = "-${formatAmountRsd(state.monthlyFee)} RSD",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                // Monthly overview button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Routes.MONTHLY_BALANCE) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column {
                            Text(
                                text = "Monthly Overview",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "See how each month went",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Add a deposit to your savings") } },
                        state = rememberTooltipState()
                    ) {
                        androidx.compose.material3.Button(
                            onClick = {
                                transactionType = TransactionType.DEPOSIT
                                showTransactionDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Add Savings")
                        }
                    }

                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Withdraw from your savings") } },
                        state = rememberTooltipState()
                    ) {
                        OutlinedButton(
                            onClick = {
                                transactionType = TransactionType.WITHDRAWAL
                                showTransactionDialog = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Take from Savings")
                        }
                    }
                }

            }
        }
    }

    if (showTransactionDialog) {
        TransactionDialog(
            type = transactionType,
            currentBalance = state.balance,
            categories = state.categories,
            onConfirm = { amount, date, note, categoryId ->
                viewModel.addTransaction(
                    amount = amount,
                    date = date,
                    type = transactionType,
                    note = note,
                    categoryId = categoryId,
                )
                showTransactionDialog = false
            },
            onDismiss = { showTransactionDialog = false },
            onInteraction = { viewModel.resetInactivityTimer() },
        )
    }

    // Logout confirmation dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = {
                viewModel.resetInactivityTimer()
                showLogoutConfirm = false
            },
            title = { Text("Log Out") },
            text = { Text("Confirm you want to log out from the application.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        navController.navigate(Routes.PIN_LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.resetInactivityTimer()
                    showLogoutConfirm = false
                }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Tip dialog
    state.currentTip?.let { tip ->
        AlertDialog(
            onDismissRequest = {
                viewModel.resetInactivityTimer()
                viewModel.dismissTip()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("Savings Tip", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text(tip) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetInactivityTimer()
                    viewModel.showNextTip()
                }) {
                    Text("Next Tip")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.resetInactivityTimer()
                    viewModel.dismissTip()
                }) {
                    Text("Close")
                }
            },
        )
    }
    // Inactivity blur dialog
    if (inactivityTriggered && state.autoBlurEnabled) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissInactivityDialog() },
            title = { Text("Inactivity detected", fontWeight = FontWeight.Bold) },
            text = { Text("The screen was locked due to inactivity.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        navController.navigate(Routes.PIN_LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissInactivityDialog() }) {
                    Text("Cancel")
                }
            },
        )
    }
    } // end outer Box
}
