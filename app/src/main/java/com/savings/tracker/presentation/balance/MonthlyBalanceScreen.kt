package com.savings.tracker.presentation.balance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Scaffold
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
    var drilldownMonth by remember { mutableStateOf<MonthSummary?>(null) }

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
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (uiState.monthlySummaries.isEmpty() && uiState.years.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No transactions yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            // Year selector — LazyRow with auto-scroll to selected year
            val yearListState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            LazyRow(
                state = yearListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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

            // Year summary card
            val yearNet = uiState.yearSummary
            val yearNetColor = if (yearNet >= 0) savingsGreen else withdrawalRed
            val yearNetSign = if (yearNet >= 0) "+" else "-"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Year ${uiState.selectedYear} Net",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "$yearNetSign${formatAmountRsd(kotlin.math.abs(yearNet))} RSD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = yearNetColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sort buttons
            val arrowFor: (MonthlySortBy) -> String = { field ->
                if (uiState.sortBy == field) (if (uiState.sortDescending) " ↓" else " ↑") else ""
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                FilterChip(
                    selected = uiState.sortBy == MonthlySortBy.MONTH,
                    onClick = { viewModel.setSortBy(MonthlySortBy.MONTH) },
                    label = { Text("Month${arrowFor(MonthlySortBy.MONTH)}") },
                )
                FilterChip(
                    selected = uiState.sortBy == MonthlySortBy.CHANGE,
                    onClick = { viewModel.setSortBy(MonthlySortBy.CHANGE) },
                    label = { Text("Change${arrowFor(MonthlySortBy.CHANGE)}") },
                )
                FilterChip(
                    selected = uiState.sortBy == MonthlySortBy.ENDING_BALANCE,
                    onClick = { viewModel.setSortBy(MonthlySortBy.ENDING_BALANCE) },
                    label = { Text("Balance${arrowFor(MonthlySortBy.ENDING_BALANCE)}") },
                )
            }

            // Monthly cards — flat, no accordion
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.monthlySummaries, key = { it.yearMonth.toString() }) { summary ->
                    val netColor = if (summary.netChange >= 0) savingsGreen else withdrawalRed
                    val netSign = if (summary.netChange >= 0) "+" else "-"
                    val monthName = summary.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { drilldownMonth = summary },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = monthName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            DetailRow(
                                label = "Starting balance",
                                value = "${formatAmountRsd(summary.startingBalance)} RSD",
                            )
                            DetailRow(
                                label = "Change",
                                value = "$netSign${formatAmountRsd(kotlin.math.abs(summary.netChange))} RSD",
                                valueColor = netColor,
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = Color.White.copy(alpha = 0.4f),
                            )
                            DetailRow(
                                label = "Ending balance",
                                value = "${formatAmountRsd(summary.endingBalance)} RSD",
                            )
                        }
                    }
                }
            }
        }
    }

    // Drilldown dialog
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
                    DetailRow(
                        label = "Starting balance",
                        value = "${formatAmountRsd(summary.startingBalance)} RSD",
                    )
                    DetailRow(
                        label = "Change",
                        value = "$netSign${formatAmountRsd(kotlin.math.abs(summary.netChange))} RSD",
                        valueColor = netColor,
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DetailRow(
                        label = "Ending balance",
                        value = "${formatAmountRsd(summary.endingBalance)} RSD",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(rows) { (txn, delta, balAfter) ->
                            val amtColor = when {
                                txn.type == TransactionType.DEPOSIT -> savingsGreen
                                txn.type == TransactionType.FEE -> feeOrange
                                else -> withdrawalRed
                            }
                            val amtSign = if (delta >= 0) "+" else "-"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = txn.date.format(txnFmt),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1.4f),
                                )
                                Text(
                                    text = "$amtSign${formatAmountRsd(kotlin.math.abs(delta))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = amtColor,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "${formatAmountRsd(balAfter)} RSD",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1.1f),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { drilldownMonth = null }) {
                    Text("Close")
                }
            },
        )
    }
}

private suspend fun androidx.compose.foundation.lazy.LazyListState.revealItem(index: Int) {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        animateScrollToItem(index)
        return
    }
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
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = valueColor,
        )
    }
}
