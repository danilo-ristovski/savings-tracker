package com.savings.tracker.presentation.trends

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.savings.tracker.domain.model.AnalysisSection
import com.savings.tracker.domain.model.CategoryType
import com.savings.tracker.domain.model.Transaction
import com.savings.tracker.domain.model.TransactionType
import com.savings.tracker.presentation.balance.MonthlyBalanceViewModel
import com.savings.tracker.presentation.balance.MonthlyOverviewContent
import com.savings.tracker.presentation.components.BarChart
import com.savings.tracker.presentation.components.HorizontalComparisonBar
import com.savings.tracker.presentation.components.LineChart
import com.savings.tracker.presentation.components.PieChart
import com.savings.tracker.presentation.components.StackedAreaChart
import com.savings.tracker.presentation.main.TransactionDialog
import com.savings.tracker.presentation.main.formatAmountRsd
import com.savings.tracker.presentation.theme.feeOrange
import com.savings.tracker.presentation.theme.savingsGreen
import com.savings.tracker.presentation.theme.withdrawalRed
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val tabs = listOf("Table", "Charts", "Analysis")

private enum class DetailLevel { SIMPLE, SUMMARY, DETAILED }
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

    val tabIcons = listOf(Icons.Default.List, Icons.Default.BarChart, Icons.Default.PieChart)

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
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        icon = { Icon(tabIcons[index], contentDescription = title) },
                        label = { Text(title) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().padding(padding),
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TableTab(viewModel: TrendsViewModel, snackbarHostState: SnackbarHostState) {
    val uiState by viewModel.uiState.collectAsState()
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showFeeWarning by remember { mutableStateOf(false) }
    var pendingFeeTransaction by remember { mutableStateOf<Transaction?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()

    // Delete confirmation
    var deleteConfirmTransaction by remember { mutableStateOf<Transaction?>(null) }

    val rows = viewModel.tableRows()
    val hasFeeTransactions = uiState.transactions.any { it.type == TransactionType.FEE }
    val availableYears = viewModel.availableYears
    val visibleCategories = categories.filter { cat ->
        when (uiState.filterType) {
            TransactionType.DEPOSIT -> cat.type == CategoryType.DEPOSIT || cat.type == CategoryType.ANY
            TransactionType.WITHDRAWAL, TransactionType.FEE -> cat.type == CategoryType.WITHDRAWAL || cat.type == CategoryType.ANY
            null -> true
        }
    }
    val isFilterActive = uiState.filterType != null || uiState.filterCategoryIds.isNotEmpty() || uiState.filterNoCategory || uiState.filterYear != null
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDetailSheet by remember { mutableStateOf(false) }
    val detailLevel = remember(uiState.tableDetailLevel) {
        runCatching { DetailLevel.valueOf(uiState.tableDetailLevel) }.getOrDefault(DetailLevel.SIMPLE)
    }
    val monthlyVm: MonthlyBalanceViewModel = hiltViewModel()
    val monthlyUiState by monthlyVm.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                }
            }
    ) {
        // Toolbar: always visible across all view levels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showFilterSheet = true }) {
                BadgedBox(badge = { if (isFilterActive) Badge() }) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filters",
                        tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Expand/collapse all — only visible in Detailed view
            if (detailLevel == DetailLevel.DETAILED) {
                val allMonths = monthlyUiState.monthlySummaries.map { it.yearMonth }
                val allExpanded = allMonths.isNotEmpty() && allMonths.all { it in monthlyUiState.expandedMonths }
                IconButton(onClick = {
                    if (allExpanded) monthlyVm.collapseAllMonths()
                    else monthlyVm.expandAllMonths(allMonths)
                }) {
                    Icon(
                        if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                        contentDescription = if (allExpanded) "Collapse all" else "Expand all",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = { showDetailSheet = true }) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "View level",
                    tint = if (detailLevel != DetailLevel.SIMPLE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (detailLevel != DetailLevel.SIMPLE) {
            val initialTab = if (detailLevel == DetailLevel.SUMMARY) 0 else 1
            MonthlyOverviewContent(
                viewModel = monthlyVm,
                initialTab = initialTab,
                showTabSwitcher = false,
            )
        } else {

        Text(
            text = if (rows.isEmpty()) "No transactions" else "${rows.size} transaction${if (rows.size == 1) "" else "s"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )

        if (rows.isEmpty()) {
            EmptyMessage("No transactions to display")
        } else {

        val listState = rememberLazyListState()
        val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex >= 10 } }
        val showScrollToBottom by remember {
            derivedStateOf {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val total = listState.layoutInfo.totalItemsCount
                listState.firstVisibleItemIndex >= 10 && total > 0 && lastVisible < total - 4
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 72.dp),
        ) {
            // Sticky sortable header
            stickyHeader {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SortableHeaderCell("Date", SortField.DATE, uiState, Modifier.weight(1.3f), TextAlign.Start) {
                        val newOrder = if (uiState.sortField == SortField.DATE && uiState.sortOrder == SortOrder.DESC) SortOrder.ASC else SortOrder.DESC
                        viewModel.setSortOrder(SortField.DATE, newOrder)
                    }
                    SortableHeaderCell("Amount", SortField.AMOUNT, uiState, Modifier.weight(1.1f), TextAlign.End) {
                        val newOrder = if (uiState.sortField == SortField.AMOUNT && uiState.sortOrder == SortOrder.DESC) SortOrder.ASC else SortOrder.DESC
                        viewModel.setSortOrder(SortField.AMOUNT, newOrder)
                    }
                    SortableHeaderCell("Balance", SortField.BALANCE, uiState, Modifier.weight(1.1f), TextAlign.End) {
                        val newOrder = if (uiState.sortField == SortField.BALANCE && uiState.sortOrder == SortOrder.DESC) SortOrder.ASC else SortOrder.DESC
                        viewModel.setSortOrder(SortField.BALANCE, newOrder)
                    }
                    Spacer(modifier = Modifier.width(64.dp))
                }
            }

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
                                    if (txn.note.isBlank()) return@let
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(txn.note)
                                    }
                                }
                            },
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = row.date.format(dateFormatter),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1.3f),
                    )
                    Text(
                        text = "${fmtAmt(row.amount)} RSD",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.weight(1.1f),
                        textAlign = TextAlign.End,
                    )
                    Text(
                        text = "${fmtAmt(row.balanceAfter)} RSD",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1.1f),
                        textAlign = TextAlign.End,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (row.hasNote) {
                        Icon(
                            Icons.Default.Notes,
                            contentDescription = "Has note",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Spacer(modifier = Modifier.width(16.dp))
                    }
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

        // FAB scroll buttons
        if (showScrollToBottom) {
            SmallFloatingActionButton(
                onClick = { coroutineScope.launch { listState.scrollToItem(listState.layoutInfo.totalItemsCount - 1) } },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to bottom")
            }
        }
        if (showScrollToTop) {
            SmallFloatingActionButton(
                onClick = { coroutineScope.launch { listState.scrollToItem(0) } },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 12.dp,
                        bottom = if (showScrollToBottom) 60.dp else 12.dp,
                    ),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
            }
        }
        } // end Box
        } // end else (rows not empty)
        } // end else (simple view)
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

    // Detail level bottom sheet
    if (showDetailSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDetailSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "View level",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                DetailLevelOption(
                    icon = Icons.Default.List,
                    title = "Simple",
                    description = "All transactions in a flat table",
                    tooltip = "Shows every transaction in a chronological table. Ideal for searching, sorting, filtering, and reviewing individual entries.",
                    selected = detailLevel == DetailLevel.SIMPLE,
                    onClick = { viewModel.setTableDetailLevel(DetailLevel.SIMPLE.name); showDetailSheet = false },
                )
                DetailLevelOption(
                    icon = Icons.Default.BarChart,
                    title = "Monthly Summary",
                    description = "Deposits, withdrawals and net change per month",
                    tooltip = "Shows monthly totals grouped by year — deposits, withdrawals, and net balance change per month. Great for spotting savings trends at a glance.",
                    selected = detailLevel == DetailLevel.SUMMARY,
                    onClick = { viewModel.setTableDetailLevel(DetailLevel.SUMMARY.name); showDetailSheet = false },
                )
                DetailLevelOption(
                    icon = Icons.Default.CalendarMonth,
                    title = "Monthly Detailed",
                    description = "Individual transactions grouped by month",
                    tooltip = "Shows every transaction grouped by month with notes and categories. Only current month is expanded by default — tap any month header to collapse or expand it.",
                    selected = detailLevel == DetailLevel.DETAILED,
                    onClick = { viewModel.setTableDetailLevel(DetailLevel.DETAILED.name); showDetailSheet = false },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Remember choice",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Restore this view level on next app open",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = uiState.persistDetailLevel,
                        onCheckedChange = { viewModel.setPersistDetailLevel(it) },
                    )
                }
            }
        }
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = {
                        viewModel.setFilterType(null)
                        viewModel.clearCategoryFilter()
                        viewModel.setFilterYear(null)
                        viewModel.setFilterMonth(null)
                    }) { Text("Reset") }
                }
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))

                Text("Transaction type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = uiState.filterType == null, onClick = { viewModel.setFilterType(null) }, label = { Text("All") })
                    FilterChip(selected = uiState.filterType == TransactionType.DEPOSIT, onClick = { viewModel.setFilterType(TransactionType.DEPOSIT) }, label = { Text("Deposit") })
                    FilterChip(selected = uiState.filterType == TransactionType.WITHDRAWAL, onClick = { viewModel.setFilterType(TransactionType.WITHDRAWAL) }, label = { Text("Withdrawal") })
                    if (hasFeeTransactions) {
                        FilterChip(selected = uiState.filterType == TransactionType.FEE, onClick = { viewModel.setFilterType(TransactionType.FEE) }, label = { Text("Fee") })
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text("Category (multi-select)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = uiState.filterNoCategory,
                        onClick = { viewModel.toggleNoCategoryFilter() },
                        label = {
                            Text(
                                "No category",
                                fontStyle = FontStyle.Italic,
                                color = if (uiState.filterNoCategory) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    visibleCategories.forEach { category ->
                        FilterChip(
                            selected = category.id in uiState.filterCategoryIds,
                            onClick = { viewModel.setFilterCategory(category.id) },
                            label = { Text(category.name) },
                        )
                    }
                }

                if (availableYears.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Year", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val yearListState = rememberLazyListState()
                    LazyRow(
                        state = yearListState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(availableYears.size) { i ->
                            val year = availableYears[i]
                            FilterChip(
                                selected = uiState.filterYear == year,
                                onClick = {
                                    viewModel.setFilterYear(if (uiState.filterYear == year) null else year)
                                    coroutineScope.launch { yearListState.revealItem(i) }
                                },
                                label = { Text(year.toString()) },
                            )
                        }
                    }
                }

                if (uiState.filterYear != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("Month", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val monthListState = rememberLazyListState()
                    LazyRow(
                        state = monthListState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(monthNames.size) { idx ->
                            val monthNum = idx + 1
                            FilterChip(
                                selected = uiState.filterMonth == monthNum,
                                onClick = {
                                    viewModel.setFilterMonth(if (uiState.filterMonth == monthNum) null else monthNum)
                                    coroutineScope.launch { monthListState.revealItem(idx) }
                                },
                                label = { Text(monthNames[idx]) },
                            )
                        }
                    }
                }
            }
        }
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
        val categories by viewModel.categories.collectAsState()
        TransactionDialog(
            type = txn.type,
            currentBalance = currentBalance,
            editTransaction = txn,
            categories = categories,
            onConfirm = { amount, date, note, categoryId ->
                viewModel.updateTransaction(
                    txn.copy(amount = amount, date = date, note = note, categoryId = categoryId)
                )
                editingTransaction = null
            },
            onDismiss = { editingTransaction = null }
        )
    }
}

@Composable
private fun ChartsTab(viewModel: TrendsViewModel, uiState: TrendsUiState) {
    val availableYears = viewModel.availableYears
    val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val coroutineScope = rememberCoroutineScope()

    var showChartInfoDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Chart type selector — only show visible types
        val visibleChartTypes = ChartType.entries.filter { it.name !in uiState.hiddenChartTypes }
        // If the selected type got hidden, auto-select first visible
        if (uiState.selectedChartType.name in uiState.hiddenChartTypes && visibleChartTypes.isNotEmpty()) {
            viewModel.selectChartType(visibleChartTypes.first())
        }

        if (visibleChartTypes.isEmpty()) {
            EmptyMessage("All chart types are hidden.\nEnable them in Settings → Chart Types.", Icons.Default.BarChart)
            return@Column
        }

        // Chart type chips as LazyRow with auto-scroll to selected chip
        val chipListState = rememberLazyListState()
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LazyRow(
                state = chipListState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(visibleChartTypes.size) { i ->
                    val type = visibleChartTypes[i]
                    FilterChip(
                        selected = uiState.selectedChartType == type,
                        onClick = {
                            viewModel.selectChartType(type)
                            coroutineScope.launch { chipListState.revealItem(i) }
                        },
                        label = { Text(type.displayName) },
                    )
                }
            }
            IconButton(onClick = { showChartInfoDialog = true }) {
                Icon(Icons.Default.Info, contentDescription = "Chart info", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Year filter chips
        if (availableYears.isNotEmpty()) {
            val chartYearListState = rememberLazyListState()
            LazyRow(
                state = chartYearListState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(availableYears.size) { i ->
                    val year = availableYears[i]
                    FilterChip(
                        selected = uiState.chartFilterYear == year,
                        onClick = {
                            viewModel.setChartFilterYear(if (uiState.chartFilterYear == year) null else year)
                            coroutineScope.launch { chartYearListState.revealItem(i) }
                        },
                        label = { Text(year.toString()) },
                    )
                }
            }
        }

        // Month filter chips — only when a year is selected
        if (uiState.chartFilterYear != null) {
            val chartMonthListState = rememberLazyListState()
            LazyRow(
                state = chartMonthListState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(monthNames.size) { idx ->
                    val monthNum = idx + 1
                    FilterChip(
                        selected = uiState.chartFilterMonth == monthNum,
                        onClick = {
                            viewModel.setChartFilterMonth(if (uiState.chartFilterMonth == monthNum) null else monthNum)
                            coroutineScope.launch { chartMonthListState.revealItem(idx) }
                        },
                        label = { Text(monthNames[idx]) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter the balance-over-time data by chart year/month
        val filteredBalanceOverTime = viewModel.balanceOverTime.let { all ->
            var result = all
            uiState.chartFilterYear?.let { y -> result = result.filter { it.first.year == y } }
            uiState.chartFilterMonth?.let { m -> result = result.filter { it.first.monthValue == m } }
            result
        }

        val chartModifier = Modifier.fillMaxWidth().weight(1f)
        when (uiState.selectedChartType) {
            ChartType.LINE -> {
                val data = filteredBalanceOverTime.map { (dt, bal) ->
                    dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) to bal
                }
                LineChart(data = data, modifier = chartModifier)
            }
            ChartType.BAR -> {
                val data = viewModel.monthlyData.entries.map { (month, pair) ->
                    Triple(month, pair.first, pair.second)
                }
                BarChart(data = data, modifier = chartModifier)
            }
            ChartType.PIE -> {
                val txns = uiState.transactions.let { all ->
                    var result = all
                    uiState.chartFilterYear?.let { y -> result = result.filter { it.date.year == y } }
                    uiState.chartFilterMonth?.let { m -> result = result.filter { it.date.monthValue == m } }
                    result
                }
                val slices = listOf(
                    "Deposits" to txns.filter { it.type == TransactionType.DEPOSIT }.sumOf { it.amount },
                    "Withdrawals" to txns.filter { it.type == TransactionType.WITHDRAWAL }.sumOf { it.amount },
                    "Fees" to txns.filter { it.type == TransactionType.FEE }.sumOf { it.amount },
                ).filter { it.second > 0 }
                PieChart(slices = slices, modifier = chartModifier)
            }
            ChartType.STACKED_AREA -> StackedAreaChart(data = viewModel.stackedAreaData, modifier = chartModifier)
        }
    }

    if (showChartInfoDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showChartInfoDialog = false },
            title = { Text(uiState.selectedChartType.displayName) },
            text = { Text(uiState.selectedChartType.description) },
            confirmButton = {
                TextButton(onClick = { showChartInfoDialog = false }) { Text("Got it") }
            },
        )
    }
}

@Composable
private fun AnalysisTab(viewModel: TrendsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var analysisInfoDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    val analysisSectionInfo = mapOf(
        "Deposits vs Withdrawals" to "A horizontal bar comparing the total number of deposit vs withdrawal transactions.",
        "Average Transaction" to "The mean amount across all your deposit transactions and separately across all withdrawal transactions.",
        "Most Active Days" to "Which days of the week you deposit most frequently, shown as a bar chart.",
        "Monthly Summary" to "Deposits and withdrawals per month with net change highlighted in green or red.",
        "Income Frequency" to "How often you make deposits on average — daily, weekly, and monthly rates.",
    )
    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex >= 3 } }
    val showScrollToBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            listState.firstVisibleItemIndex >= 3 && total > 0 && lastVisible < total - 1
        }
    }

    val allHidden = AnalysisSection.entries.all { it.name in uiState.hiddenAnalysisSections }

    Box(modifier = Modifier.fillMaxSize()) {
    if (allHidden) {
        EmptyMessage("All sections are hidden.\nEnable them in Settings → Analysis Sections.", Icons.Default.PieChart)
    } else {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (AnalysisSection.STATISTICS.name !in uiState.hiddenAnalysisSections) {
        item {
            AnalysisCard("Deposits vs Withdrawals", onInfo = { analysisInfoDialog = "Deposits vs Withdrawals" to (analysisSectionInfo["Deposits vs Withdrawals"] ?: "") }) {
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
            AnalysisCard("Average Transaction", onInfo = { analysisInfoDialog = "Average Transaction" to (analysisSectionInfo["Average Transaction"] ?: "") }) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatItem("Avg Deposit", "${fmtAmt(viewModel.averageDeposit)} RSD", Modifier.weight(1f))
                    StatItem("Avg Withdrawal", "${fmtAmt(viewModel.averageWithdrawal)} RSD", Modifier.weight(1f))
                }
            }
        }
        } // end STATISTICS

        if (AnalysisSection.DEPOSITS_FREQUENCY.name !in uiState.hiddenAnalysisSections) {
        item {
            AnalysisCard("Most Active Days", onInfo = { analysisInfoDialog = "Most Active Days" to (analysisSectionInfo["Most Active Days"] ?: "") }) {
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
        } // end DEPOSITS_FREQUENCY

        if (AnalysisSection.MONTHLY_BREAKDOWN.name !in uiState.hiddenAnalysisSections) {
        item {
            var expandedMonthlySummaryKeys by remember { mutableStateOf(emptySet<String>()) }
            AnalysisCard("Monthly Summary", onInfo = { analysisInfoDialog = "Monthly Summary" to (analysisSectionInfo["Monthly Summary"] ?: "") }) {
                val data = viewModel.monthlyData
                if (data.isEmpty()) {
                    Text("No data yet", style = MaterialTheme.typography.bodyMedium)
                } else {
                    data.forEach { (month, pair) ->
                        val diff = pair.first - pair.second
                        val diffColor = if (diff >= 0) savingsGreen else withdrawalRed
                        val diffSign = if (diff >= 0) "+" else "-"
                        val isExpanded = month in expandedMonthlySummaryKeys
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedMonthlySummaryKeys = if (isExpanded)
                                            expandedMonthlySummaryKeys - month
                                        else
                                            expandedMonthlySummaryKeys + month
                                    }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(month, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically(),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, bottom = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            "+${fmtAmt(pair.first)} RSD",
                                            color = savingsGreen,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        Text(
                                            "-${fmtAmt(pair.second)} RSD",
                                            color = withdrawalRed,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    Text(
                                        text = "Net: $diffSign${fmtAmt(kotlin.math.abs(diff))} RSD",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = diffColor,
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
        } // end MONTHLY_BREAKDOWN

        if (AnalysisSection.INCOME_FREQUENCY.name !in uiState.hiddenAnalysisSections) {
        item {
            AnalysisCard("Income Frequency", onInfo = { analysisInfoDialog = "Income Frequency" to (analysisSectionInfo["Income Frequency"] ?: "") }) {
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
        } // end DEPOSITS_FREQUENCY (Income Frequency)
    }
    } // end else (not allHidden)

    if (showScrollToBottom) {
        SmallFloatingActionButton(
            onClick = { coroutineScope.launch { listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1) } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 12.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to bottom") }
    }
    if (showScrollToTop) {
        SmallFloatingActionButton(
            onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = if (showScrollToBottom) 60.dp else 12.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top") }
    }
    analysisInfoDialog?.let { (title, message) ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { analysisInfoDialog = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { analysisInfoDialog = null }) { Text("Got it") }
            },
        )
    }
    } // end Box
}

@Composable
private fun AnalysisCard(
    title: String,
    onInfo: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (onInfo != null) {
                    IconButton(
                        onClick = onInfo,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "More info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SortableHeaderCell(
    text: String,
    field: SortField,
    uiState: TrendsUiState,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Start,
    onClick: () -> Unit,
) {
    val isActive = uiState.sortField == field
    val arrow = if (isActive) (if (uiState.sortOrder == SortOrder.ASC) " ↑" else " ↓") else ""
    val arrangement = if (textAlign == TextAlign.End) Arrangement.End else Arrangement.Start
    Row(
        modifier = modifier.clickable(onClick = onClick),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$text$arrow",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
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
private fun EmptyMessage(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailLevelOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    tooltip: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(
                    tooltip,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        state = rememberTooltipState(),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                    Text(
                        description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (selected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * Scrolls a LazyRow the minimum amount needed to fully reveal the item at [index].
 * If already fully visible → no scroll. Partially clipped left → scroll left just enough.
 * Partially clipped right → scroll right just enough. Fully off-screen → falls back to animateScrollToItem.
 */
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
