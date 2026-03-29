package com.savings.tracker.presentation.balance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.savings.tracker.domain.model.TransactionType
import com.savings.tracker.presentation.main.formatAmountRsd
import com.savings.tracker.presentation.theme.feeOrange
import com.savings.tracker.presentation.theme.savingsGreen
import com.savings.tracker.presentation.theme.withdrawalRed
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyBalanceScreen(
    navController: NavController,
    viewModel: MonthlyBalanceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val categoryMap = remember(categories) { categories.associate { it.id to it.name } }
    var drilldownMonth by remember { mutableStateOf<MonthSummary?>(null) }

    val yearListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly Overview") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = uiState.selectedMainTab == 0,
                    onClick = { viewModel.selectMainTab(0) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text("Overview") },
                )
                NavigationBarItem(
                    selected = uiState.selectedMainTab == 1,
                    onClick = { viewModel.selectMainTab(1) },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Transactions") },
                )
            }
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (uiState.monthlySummaries.isEmpty() && uiState.years.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(
                    "No transactions yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            // Shared year selector — present on both tabs
            LazyRow(
                state = yearListState,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.years.size) { i ->
                    val year = uiState.years[i]
                    FilterChip(
                        selected = year == uiState.selectedYear,
                        onClick = {
                            viewModel.selectYear(year)
                            coroutineScope.launch { yearListState.revealItem(i) }
                        },
                        label = { Text(year.toString()) },
                    )
                }
            }

            when (uiState.selectedMainTab) {
                0 -> OverviewTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    onMonthClick = { drilldownMonth = it },
                )
                1 -> TransactionsTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    categoryMap = categoryMap,
                )
            }
        }
    }

    // Month drilldown dialog
    drilldownMonth?.let { summary ->
        val monthName = summary.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val netColor = if (summary.netChange >= 0) savingsGreen else withdrawalRed
        val netSign = if (summary.netChange >= 0) "+" else "-"
        val txnFmt = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm") }

        var runBal = summary.startingBalance
        val rows = summary.transactions.map { txn ->
            val delta = if (txn.type == TransactionType.DEPOSIT) txn.amount else -txn.amount
            runBal += delta
            Triple(txn, delta, runBal)
        }

        AlertDialog(
            onDismissRequest = { drilldownMonth = null },
            title = { Text("$monthName ${summary.yearMonth.year}") },
            text = {
                Column {
                    DetailRow("Starting balance", "${formatAmountRsd(summary.startingBalance)} RSD")
                    DetailRow(
                        "Change",
                        "$netSign${formatAmountRsd(kotlin.math.abs(summary.netChange))} RSD",
                        netColor,
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DetailRow("Ending balance", "${formatAmountRsd(summary.endingBalance)} RSD")
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    Text("Transactions", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(rows) { (txn, delta, balAfter) ->
                            val amtColor = when (txn.type) {
                                TransactionType.DEPOSIT -> savingsGreen
                                TransactionType.FEE -> feeOrange
                                else -> withdrawalRed
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(txn.date.format(txnFmt), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.4f))
                                Text(
                                    "${if (delta >= 0) "+" else "-"}${formatAmountRsd(kotlin.math.abs(delta))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = amtColor,
                                    modifier = Modifier.weight(1f),
                                )
                                Text("${formatAmountRsd(balAfter)} RSD", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.1f))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { drilldownMonth = null }) { Text("Close") } },
        )
    }
}

// ─── Overview tab ────────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(
    uiState: MonthlyBalanceUiState,
    viewModel: MonthlyBalanceViewModel,
    onMonthClick: (MonthSummary) -> Unit,
) {
    val yearNet = uiState.yearSummary
    val yearNetColor = if (yearNet >= 0) savingsGreen else withdrawalRed
    val yearNetSign = if (yearNet >= 0) "+" else "-"
    val arrowFor: (MonthlySortBy) -> String = { field ->
        if (uiState.sortBy == field) (if (uiState.sortDescending) " ↓" else " ↑") else ""
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Year net card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Year ${uiState.selectedYear} Net", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "$yearNetSign${formatAmountRsd(kotlin.math.abs(yearNet))} RSD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = yearNetColor,
                    )
                }
            }
        }

        // Sort chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = uiState.sortBy == MonthlySortBy.MONTH, onClick = { viewModel.setSortBy(MonthlySortBy.MONTH) }, label = { Text("Month${arrowFor(MonthlySortBy.MONTH)}") })
                FilterChip(selected = uiState.sortBy == MonthlySortBy.CHANGE, onClick = { viewModel.setSortBy(MonthlySortBy.CHANGE) }, label = { Text("Change${arrowFor(MonthlySortBy.CHANGE)}") })
                FilterChip(selected = uiState.sortBy == MonthlySortBy.ENDING_BALANCE, onClick = { viewModel.setSortBy(MonthlySortBy.ENDING_BALANCE) }, label = { Text("Balance${arrowFor(MonthlySortBy.ENDING_BALANCE)}") })
            }
        }

        // Monthly cards
        items(uiState.monthlySummaries, key = { it.yearMonth.toString() }) { summary ->
            val netColor = if (summary.netChange >= 0) savingsGreen else withdrawalRed
            val netSign = if (summary.netChange >= 0) "+" else "-"
            val monthName = summary.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

            Card(
                modifier = Modifier.fillMaxWidth().clickable { onMonthClick(summary) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(monthName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "$netSign${formatAmountRsd(kotlin.math.abs(summary.netChange))} RSD",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = netColor,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Deposits", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("+${formatAmountRsd(summary.deposits)} RSD", style = MaterialTheme.typography.bodySmall, color = savingsGreen, fontWeight = FontWeight.Medium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Withdrawals", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("-${formatAmountRsd(summary.withdrawals)} RSD", style = MaterialTheme.typography.bodySmall, color = withdrawalRed, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.4f))
                    Spacer(Modifier.height(6.dp))
                    DetailRow("Starting balance", "${formatAmountRsd(summary.startingBalance)} RSD")
                    DetailRow("Ending balance", "${formatAmountRsd(summary.endingBalance)} RSD")
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ─── Transactions tab ─────────────────────────────────────────────────────────

@Composable
private fun TransactionsTab(
    uiState: MonthlyBalanceUiState,
    viewModel: MonthlyBalanceViewModel,
    categoryMap: Map<Long, String>,
) {
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy.") }
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }

    // All txns for the selected year's months, grouped newest-first
    val allTxnsForYear = uiState.monthlySummaries
        .sortedByDescending { it.yearMonth }
        .flatMap { summary -> summary.transactions.map { summary to it } }

    // Determine which types actually exist so we only show relevant chips
    val existingTypes = remember(allTxnsForYear) {
        allTxnsForYear.map { (_, txn) -> txn.type }.toSet()
    }

    val activeFilter = uiState.transactionTypeFilter  // empty set = show all

    val filteredTxns = if (activeFilter.isEmpty()) allTxnsForYear
    else allTxnsForYear.filter { (_, txn) -> txn.type in activeFilter }

    val grouped = filteredTxns.groupBy { (summary, _) -> summary.yearMonth }
    val months = grouped.keys.sortedDescending()

    Column(Modifier.fillMaxSize()) {
        // Type filter chips (multi-select)
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (TransactionType.DEPOSIT in existingTypes) {
                item {
                    FilterChip(
                        selected = TransactionType.DEPOSIT in activeFilter,
                        onClick = { viewModel.toggleTransactionTypeFilter(TransactionType.DEPOSIT) },
                        label = { Text("Deposits") },
                    )
                }
            }
            if (TransactionType.WITHDRAWAL in existingTypes) {
                item {
                    FilterChip(
                        selected = TransactionType.WITHDRAWAL in activeFilter,
                        onClick = { viewModel.toggleTransactionTypeFilter(TransactionType.WITHDRAWAL) },
                        label = { Text("Withdrawals") },
                    )
                }
            }
            if (TransactionType.FEE in existingTypes) {
                item {
                    FilterChip(
                        selected = TransactionType.FEE in activeFilter,
                        onClick = { viewModel.toggleTransactionTypeFilter(TransactionType.FEE) },
                        label = { Text("Fees") },
                    )
                }
            }
        }

        if (filteredTxns.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No transactions", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            months.forEach { ym ->
                val entries = grouped[ym] ?: return@forEach
                val monthName = ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

                // Month section header
                item(key = "header_$ym") {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "$monthName ${ym.year}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "${entries.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Month card containing all matching transactions
                item(key = "card_$ym") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column {
                            entries.forEachIndexed { idx, (_, txn) ->
                                val amtColor = when (txn.type) {
                                    TransactionType.DEPOSIT -> savingsGreen
                                    TransactionType.FEE -> feeOrange
                                    TransactionType.WITHDRAWAL -> withdrawalRed
                                }
                                val amtSign = if (txn.type == TransactionType.DEPOSIT) "+" else "-"
                                val categoryName = txn.categoryId?.let { categoryMap[it] }
                                val typeLabel = when (txn.type) {
                                    TransactionType.DEPOSIT -> "Deposit"
                                    TransactionType.WITHDRAWAL -> "Withdrawal"
                                    TransactionType.FEE -> "Fee"
                                }

                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        // Left: date + type/category
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = txn.date.format(dateFmt),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                                Text(
                                                    text = txn.date.format(timeFmt),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                // Type badge
                                                Surface(
                                                    color = amtColor.copy(alpha = 0.15f),
                                                    shape = MaterialTheme.shapes.small,
                                                ) {
                                                    Text(
                                                        text = typeLabel,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = amtColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    )
                                                }
                                                // Category badge
                                                if (categoryName != null) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                                        shape = MaterialTheme.shapes.small,
                                                    ) {
                                                        Text(
                                                            text = categoryName,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Right: amount
                                        Text(
                                            text = "$amtSign${formatAmountRsd(txn.amount)} RSD",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = amtColor,
                                        )
                                    }

                                    // Note
                                    if (txn.note.isNotBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = txn.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                if (idx < entries.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private suspend fun androidx.compose.foundation.lazy.LazyListState.revealItem(index: Int) {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) { animateScrollToItem(index); return }
    val viewportStart = info.viewportStartOffset
    val viewportEnd = info.viewportEndOffset
    val itemStart = item.offset
    val itemEnd = item.offset + item.size
    when {
        itemStart < viewportStart -> animateScrollBy((itemStart - viewportStart).toFloat())
        itemEnd > viewportEnd -> animateScrollBy((itemEnd - viewportEnd).toFloat())
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = valueColor)
    }
}
