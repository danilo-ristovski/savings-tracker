package com.savings.tracker.presentation.trends

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.savings.tracker.domain.model.Transaction
import com.savings.tracker.domain.model.TransactionType
import com.savings.tracker.presentation.components.BarChart
import com.savings.tracker.presentation.components.HorizontalComparisonBar
import com.savings.tracker.presentation.components.LineChart
import com.savings.tracker.presentation.components.PieChart
import com.savings.tracker.presentation.main.TransactionDialog
import com.savings.tracker.presentation.main.formatAmountRsd
import android.widget.Toast
import com.savings.tracker.presentation.theme.feeOrange
import com.savings.tracker.presentation.theme.savingsGreen
import com.savings.tracker.presentation.theme.withdrawalRed
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val tabs = listOf("Table", "Charts", "Analysis")
private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy.")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private fun fmtAmt(v: Double) = formatAmountRsd(v)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrendsScreen(
    navController: NavController,
    viewModel: TrendsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectTab(pagerState.currentPage)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trends & Analytics") },
                navigationIcon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Go back") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(title) },
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (page) {
                        0 -> TableTab(viewModel, snackbarHostState)
                        1 -> ChartsTab(viewModel, uiState)
                        2 -> AnalysisTab(viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TableTab(viewModel: TrendsViewModel, snackbarHostState: SnackbarHostState) {
    val uiState by viewModel.uiState.collectAsState()
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showFeeWarning by remember { mutableStateOf(false) }
    var pendingFeeTransaction by remember { mutableStateOf<Transaction?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val groupedRows = viewModel.groupedTableRows()
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Delete confirmation
    var deleteConfirmTransaction by remember { mutableStateOf<Transaction?>(null) }

    // Initialize all groups as expanded
    LaunchedEffect(groupedRows.keys.toSet()) {
        groupedRows.keys.forEach { key ->
            if (key !in expandedGroups) {
                expandedGroups[key] = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                }
            }
    ) {
        // Search with clear button
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            label = { Text("Search transactions") },
            singleLine = true,
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = uiState.filterType == null,
                onClick = { viewModel.setFilterType(null) },
                label = { Text("All") },
            )
            FilterChip(
                selected = uiState.filterType == TransactionType.DEPOSIT,
                onClick = { viewModel.setFilterType(TransactionType.DEPOSIT) },
                label = { Text("Deposit") },
            )
            FilterChip(
                selected = uiState.filterType == TransactionType.WITHDRAWAL,
                onClick = { viewModel.setFilterType(TransactionType.WITHDRAWAL) },
                label = { Text("Withdrawal") },
            )
            FilterChip(
                selected = uiState.filterType == TransactionType.FEE,
                onClick = { viewModel.setFilterType(TransactionType.FEE) },
                label = { Text("Fee") },
            )
        }

        // Sort chips — toggle between ASC/DESC on click
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val dateSelected = uiState.sortField == SortField.DATE
            val dateArrow = if (dateSelected && uiState.sortOrder == SortOrder.ASC) "\u2191" else "\u2193"
            FilterChip(
                selected = dateSelected,
                onClick = {
                    if (dateSelected) {
                        val newOrder = if (uiState.sortOrder == SortOrder.DESC) SortOrder.ASC else SortOrder.DESC
                        viewModel.setSortOrder(SortField.DATE, newOrder)
                    } else {
                        viewModel.setSortOrder(SortField.DATE, SortOrder.DESC)
                    }
                },
                label = { Text("Date $dateArrow") },
            )

            val amountSelected = uiState.sortField == SortField.AMOUNT
            val amountArrow = if (amountSelected && uiState.sortOrder == SortOrder.ASC) "\u2191" else "\u2193"
            FilterChip(
                selected = amountSelected,
                onClick = {
                    if (amountSelected) {
                        val newOrder = if (uiState.sortOrder == SortOrder.DESC) SortOrder.ASC else SortOrder.DESC
                        viewModel.setSortOrder(SortField.AMOUNT, newOrder)
                    } else {
                        viewModel.setSortOrder(SortField.AMOUNT, SortOrder.DESC)
                    }
                },
                label = { Text("Amount $amountArrow") },
            )
        }

        if (groupedRows.isEmpty()) {
            EmptyMessage("No transactions to display")
            return
        }

        val groupOrder = listOf("Today", "This Week", "This Month", "Older")

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeaderCell("Date", Modifier.weight(1.1f), TextAlign.Start)
                    HeaderCell("Type", Modifier.weight(1.1f), TextAlign.Start)
                    HeaderCell("Amount", Modifier.weight(1f), TextAlign.End)
                    HeaderCell("Balance", Modifier.weight(1f), TextAlign.End)
                    // Space for delete button
                    Spacer(modifier = Modifier.width(40.dp))
                }
            }

            groupOrder.forEach { groupName ->
                val rows = groupedRows[groupName] ?: return@forEach
                val isExpanded = expandedGroups[groupName] ?: true

                // Sticky header
                stickyHeader {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable {
                                expandedGroups[groupName] = !isExpanded
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "$groupName (${rows.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }

                if (isExpanded) {
                    itemsIndexed(rows, key = { _, row -> row.id }) { index, row ->
                        val bgColor = if (index % 2 == 0) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                        val textColor = when (row.type) {
                            TransactionType.DEPOSIT -> savingsGreen
                            TransactionType.WITHDRAWAL -> withdrawalRed
                            TransactionType.FEE -> feeOrange
                        }
                        val transaction = uiState.transactions.find { it.id == row.id }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor)
                                .combinedClickable(
                                    onClick = {
                                        focusManager.clearFocus()
                                        transaction?.let { txn ->
                                            if (txn.type == TransactionType.FEE) {
                                                pendingFeeTransaction = txn
                                                showFeeWarning = true
                                            } else {
                                                editingTransaction = txn
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        transaction?.let { txn ->
                                            val note = txn.note.ifEmpty { "(no note)" }
                                            Toast.makeText(context, note, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = row.date.format(dateFormatter),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1.1f),
                            )
                            Text(
                                text = row.type.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor,
                                modifier = Modifier.weight(1.1f),
                            )
                            Text(
                                text = "${fmtAmt(row.amount)} RSD",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End,
                            )
                            Text(
                                text = "${fmtAmt(row.balanceAfter)} RSD",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End,
                            )
                            IconButton(
                                onClick = {
                                    transaction?.let { deleteConfirmTransaction = it }
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
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
            }
        }
    }

    // Delete confirmation dialog
    deleteConfirmTransaction?.let { txn ->
        AlertDialog(
            onDismissRequest = { deleteConfirmTransaction = null },
            title = { Text("Delete Transaction", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(onClick = {
                    deleteConfirmTransaction = null
                    viewModel.deleteTransaction(txn)
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Transaction deleted",
                            actionLabel = "Undo",
                            duration = androidx.compose.material3.SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restoreTransaction(txn.id)
                        }
                    }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTransaction = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Fee warning dialog
    if (showFeeWarning) {
        AlertDialog(
            onDismissRequest = {
                showFeeWarning = false
                pendingFeeTransaction = null
            },
            title = { Text("Edit Fee Transaction", fontWeight = FontWeight.Bold) },
            text = {
                Text("This is a predefined bank fee transaction. It is not meant to be updated manually. Are you sure you want to edit it?")
            },
            confirmButton = {
                TextButton(onClick = {
                    showFeeWarning = false
                    editingTransaction = pendingFeeTransaction
                    pendingFeeTransaction = null
                }) {
                    Text("Edit Anyway", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFeeWarning = false
                    pendingFeeTransaction = null
                }) {
                    Text("Cancel")
                }
            },
        )
    }

    editingTransaction?.let { txn ->
        val currentBalance = viewModel.totalBalance
        TransactionDialog(
            type = txn.type,
            currentBalance = currentBalance,
            editTransaction = txn,
            onConfirm = { amount, date, note ->
                viewModel.updateTransaction(
                    txn.copy(amount = amount, date = date, note = note)
                )
                editingTransaction = null
            },
            onDismiss = { editingTransaction = null }
        )
    }
}

@Composable
private fun ChartsTab(viewModel: TrendsViewModel, uiState: TrendsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChartType.entries.forEach { type ->
                FilterChip(
                    selected = uiState.selectedChartType == type,
                    onClick = { viewModel.selectChartType(type) },
                    label = { Text(type.name) },
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        when (uiState.selectedChartType) {
            ChartType.LINE -> {
                val data = viewModel.balanceOverTime.map { (dt, bal) ->
                    dt.format(DateTimeFormatter.ofPattern("dd.MM")) to bal
                }
                LineChart(
                    data = data,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                )
            }

            ChartType.BAR -> {
                val data = viewModel.monthlyData.map { (month, pair) ->
                    Triple(month, pair.first, pair.second)
                }
                BarChart(
                    data = data,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                )
            }

            ChartType.PIE -> {
                val txns = uiState.transactions
                val totalDeposits = txns.filter { it.type == TransactionType.DEPOSIT }.sumOf { it.amount }
                val totalWithdrawals = txns.filter { it.type == TransactionType.WITHDRAWAL }.sumOf { it.amount }
                val totalFees = txns.filter { it.type == TransactionType.FEE }.sumOf { it.amount }
                val slices = listOf(
                    "Deposits" to totalDeposits,
                    "Withdrawals" to totalWithdrawals,
                    "Fees" to totalFees,
                ).filter { it.second > 0 }
                PieChart(
                    slices = slices,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                )
            }
        }
    }
}

@Composable
private fun AnalysisTab(viewModel: TrendsViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AnalysisCard("Deposits vs Withdrawals") {
                HorizontalComparisonBar(
                    label1 = "Deposits",
                    value1 = viewModel.depositCount.toDouble(),
                    label2 = "Withdrawals",
                    value2 = viewModel.withdrawalCount.toDouble(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            AnalysisCard("Average Transaction") {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatItem("Avg Deposit", "${fmtAmt(viewModel.averageDeposit)} RSD", Modifier.weight(1f))
                    StatItem("Avg Withdrawal", "${fmtAmt(viewModel.averageWithdrawal)} RSD", Modifier.weight(1f))
                }
            }
        }

        item {
            AnalysisCard("Most Active Days") {
                val freq = viewModel.depositsFrequency
                if (freq.isEmpty()) {
                    Text("No data yet", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val maxCount = freq.values.maxOrNull() ?: 1
                    DayOfWeek.entries.forEach { day ->
                        val count = freq[day] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = day.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(40.dp),
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(16.dp),
                            ) {
                                if (count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(count.toFloat() / maxCount)
                                            .height(16.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.shapes.extraSmall,
                                            ),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        item {
            AnalysisCard("Monthly Summary") {
                val data = viewModel.monthlyData
                if (data.isEmpty()) {
                    Text("No data yet", style = MaterialTheme.typography.bodyMedium)
                } else {
                    data.forEach { (month, pair) ->
                        val diff = pair.first - pair.second
                        val diffColor = if (diff >= 0) savingsGreen else withdrawalRed
                        val diffSign = if (diff >= 0) "+" else "-"
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(month, style = MaterialTheme.typography.bodyMedium)
                                Row {
                                    Text(
                                        "+${fmtAmt(pair.first)}",
                                        color = savingsGreen,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "-${fmtAmt(pair.second)}",
                                        color = withdrawalRed,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                            Text(
                                text = "Net: $diffSign${fmtAmt(kotlin.math.abs(diff))} RSD",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = diffColor,
                            )
                        }
                    }
                }
            }
        }

        item {
            AnalysisCard("Income Frequency") {
                val txns = viewModel.uiState.value.transactions
                val deposits = txns.filter { it.type == TransactionType.DEPOSIT }
                if (deposits.size < 2) {
                    Text("Not enough data", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val sorted = deposits.sortedBy { it.date }
                    val totalDays = java.time.Duration.between(sorted.first().date, sorted.last().date).toDays().coerceAtLeast(1)
                    val perDay = deposits.size.toDouble() / totalDays
                    Column {
                        FreqRow("Daily average", "%.1f".format(perDay))
                        FreqRow("Weekly average", "%.1f".format(perDay * 7))
                        FreqRow("Monthly average", "%.1f".format(perDay * 30))
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalysisCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, textAlign: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        textAlign = textAlign,
        modifier = modifier,
    )
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FreqRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
