package com.savings.tracker.presentation.trends

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
private val amountFormat = "%.2f"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrendsScreen(
    navController: NavController,
    viewModel: TrendsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectTab(pagerState.currentPage)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trends & Analytics") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
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
                        0 -> TableTab(viewModel)
                        1 -> ChartsTab(viewModel, uiState)
                        2 -> AnalysisTab(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun TableTab(viewModel: TrendsViewModel) {
    val rows = viewModel.tableRows()
    val uiState by viewModel.uiState.collectAsState()
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    if (rows.isEmpty()) {
        EmptyMessage("No transactions to display")
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                HeaderCell("Date", Modifier.weight(1.4f), TextAlign.Start)
                HeaderCell("Type", Modifier.weight(1f), TextAlign.Start)
                HeaderCell("Amount", Modifier.weight(1f), TextAlign.End)
                HeaderCell("Balance", Modifier.weight(1f), TextAlign.End)
            }
        }

        itemsIndexed(rows) { index, row ->
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
            // Find the matching transaction for editing
            val transaction = uiState.transactions.getOrNull(index)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .clickable {
                        transaction?.let { editingTransaction = it }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = row.date.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1.4f),
                )
                Text(
                    text = row.type.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${amountFormat.format(row.amount)} RSD",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                )
                Text(
                    text = "${amountFormat.format(row.balanceAfter)} RSD",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                )
            }
        }
    }

    editingTransaction?.let { txn ->
        if (txn.type != TransactionType.FEE) {
            val currentBalance = rows.lastOrNull()?.balanceAfter ?: 0.0
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
                    StatItem("Avg Deposit", "${amountFormat.format(viewModel.averageDeposit)} RSD", Modifier.weight(1f))
                    StatItem("Avg Withdrawal", "${amountFormat.format(viewModel.averageWithdrawal)} RSD", Modifier.weight(1f))
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(month, style = MaterialTheme.typography.bodyMedium)
                            Row {
                                Text(
                                    "+${amountFormat.format(pair.first)} RSD",
                                    color = savingsGreen,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "-${amountFormat.format(pair.second)} RSD",
                                    color = withdrawalRed,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
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
                        FreqRow("Daily average", amountFormat.format(perDay))
                        FreqRow("Weekly average", amountFormat.format(perDay * 7))
                        FreqRow("Monthly average", amountFormat.format(perDay * 30))
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
        style = MaterialTheme.typography.labelMedium,
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
