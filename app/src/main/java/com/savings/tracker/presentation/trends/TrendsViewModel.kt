package com.savings.tracker.presentation.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savings.tracker.data.preferences.PreferencesManager
import com.savings.tracker.domain.model.Category
import com.savings.tracker.domain.model.Transaction
import com.savings.tracker.domain.model.TransactionType
import com.savings.tracker.domain.repository.CategoryRepository
import com.savings.tracker.domain.repository.TransactionRepository
import com.savings.tracker.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

enum class ChartType(val displayName: String, val description: String) {
    LINE("Line", "Shows balance progression over time as a continuous line. Reveals growth trajectory and trends at a glance."),
    BAR("Bar", "Compares monthly deposits vs withdrawals as side-by-side bars. Reveals saving patterns month by month."),
    PIE("Pie", "Shows the proportion of deposits vs withdrawals vs fees. Good for understanding where your money goes."),
    STACKED_AREA("Stacked Area", "Cumulative deposits and withdrawals as filled areas over time. Immediately shows whether savings are growing or shrinking."),
}
enum class SortField { DATE, AMOUNT, BALANCE }
enum class SortOrder { ASC, DESC }

data class TrendsUiState(
    val transactions: List<Transaction> = emptyList(),
    val selectedTab: Int = 0,
    val selectedChartType: ChartType = ChartType.LINE,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val filterType: TransactionType? = null,
    val filterYear: Int? = null,
    val filterMonth: Int? = null,
    val chartFilterYear: Int? = null,
    val chartFilterMonth: Int? = null,
    val sortField: SortField = SortField.DATE,
    val sortOrder: SortOrder = SortOrder.ASC,
    val hiddenAnalysisSections: Set<String> = emptySet(),
    val filterCategoryIds: Set<Long> = emptySet(),
    val filterNoCategory: Boolean = false,
    val hiddenChartTypes: Set<String> = emptySet(),
    val tableDetailLevel: String = "SIMPLE",
    val persistDetailLevel: Boolean = false,
)

@HiltViewModel
class TrendsViewModel @Inject constructor(
    getTransactionsUseCase: GetTransactionsUseCase,
    private val repository: TransactionRepository,
    categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendsUiState())
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

    init {
        categoryRepository.getAllCategories()
            .onEach { _categories.value = it }
            .launchIn(viewModelScope)

        preferencesManager.trendsSortFieldFlow
            .onEach { field ->
                val sortField = runCatching { SortField.valueOf(field) }.getOrDefault(SortField.DATE)
                _uiState.update { it.copy(sortField = sortField) }
            }
            .launchIn(viewModelScope)

        preferencesManager.trendsSortOrderFlow
            .onEach { order ->
                val sortOrder = runCatching { SortOrder.valueOf(order) }.getOrDefault(SortOrder.ASC)
                _uiState.update { it.copy(sortOrder = sortOrder) }
            }
            .launchIn(viewModelScope)

        getTransactionsUseCase()
            .onEach { txns ->
                _uiState.update {
                    it.copy(
                        transactions = txns.sortedBy { t -> t.date },
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)

        preferencesManager.analysisHiddenSectionsFlow
            .onEach { sections ->
                _uiState.update { it.copy(hiddenAnalysisSections = sections) }
            }
            .launchIn(viewModelScope)

        preferencesManager.chartHiddenTypesFlow
            .onEach { types ->
                _uiState.update { it.copy(hiddenChartTypes = types) }
            }
            .launchIn(viewModelScope)

        preferencesManager.persistDetailLevelFlow
            .onEach { persist ->
                _uiState.update { it.copy(persistDetailLevel = persist) }
            }
            .launchIn(viewModelScope)

        // Load persisted detail level only if persist toggle is on
        viewModelScope.launch {
            val persist = preferencesManager.persistDetailLevelFlow.first()
            val level = if (persist) preferencesManager.tableDetailLevelFlow.first() else "SIMPLE"
            _uiState.update { it.copy(tableDetailLevel = level) }
        }
    }

    fun setTableDetailLevel(level: String) {
        _uiState.update { it.copy(tableDetailLevel = level) }
        viewModelScope.launch { preferencesManager.setTableDetailLevel(level) }
    }

    fun setPersistDetailLevel(persist: Boolean) {
        _uiState.update { it.copy(persistDetailLevel = persist) }
        viewModelScope.launch {
            preferencesManager.setPersistDetailLevel(persist)
            if (persist) preferencesManager.setTableDetailLevel(_uiState.value.tableDetailLevel)
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun selectChartType(type: ChartType) {
        _uiState.update { it.copy(selectedChartType = type) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilterType(type: TransactionType?) {
        _uiState.update { it.copy(filterType = type, filterCategoryIds = emptySet()) }
    }

    fun setFilterCategory(id: Long) {
        _uiState.update { state ->
            val current = state.filterCategoryIds.toMutableSet()
            if (id in current) current.remove(id) else current.add(id)
            state.copy(filterCategoryIds = current)
        }
    }

    fun toggleNoCategoryFilter() {
        _uiState.update { it.copy(filterNoCategory = !it.filterNoCategory) }
    }

    fun clearCategoryFilter() {
        _uiState.update { it.copy(filterCategoryIds = emptySet(), filterNoCategory = false) }
    }

    fun setSortOrder(field: SortField, order: SortOrder) {
        _uiState.update { it.copy(sortField = field, sortOrder = order) }
        viewModelScope.launch {
            preferencesManager.setTrendsSortField(field.name)
            preferencesManager.setTrendsSortOrder(order.name)
        }
    }

    fun setFilterYear(year: Int?) {
        _uiState.update { it.copy(filterYear = year, filterMonth = null) }
    }

    fun setFilterMonth(month: Int?) {
        _uiState.update { it.copy(filterMonth = month) }
    }

    fun setChartFilterYear(year: Int?) {
        _uiState.update { it.copy(chartFilterYear = year, chartFilterMonth = null) }
    }

    fun setChartFilterMonth(month: Int?) {
        _uiState.update { it.copy(chartFilterMonth = month) }
    }

    val availableYears: List<Int>
        get() = _uiState.value.transactions
            .map { it.date.year }
            .distinct()
            .sortedDescending()

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction.copy(updatedAt = java.time.LocalDateTime.now()))
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.softDeleteTransaction(transaction.id)
        }
    }

    fun restoreTransaction(id: Long) {
        viewModelScope.launch {
            repository.restoreTransaction(id)
        }
    }

    val monthlyData: Map<String, Pair<Double, Double>>
        get() {
            val txns = _uiState.value.transactions
            return txns.groupBy { it.date.format(monthFormatter) }
                .mapValues { (_, list) ->
                    val deposits = list.filter { it.type == TransactionType.DEPOSIT }.sumOf { it.amount }
                    val withdrawals = list.filter { it.type != TransactionType.DEPOSIT }.sumOf { it.amount }
                    deposits to withdrawals
                }
        }

    val stackedAreaData: List<Triple<String, Double, Double>>
        get() {
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yy")
            var cumDep = 0.0
            var cumWd = 0.0
            return _uiState.value.transactions.sortedBy { it.date }.map { txn ->
                when (txn.type) {
                    TransactionType.DEPOSIT -> cumDep += txn.amount
                    TransactionType.WITHDRAWAL, TransactionType.FEE -> cumWd += txn.amount
                }
                Triple(txn.date.format(formatter), cumDep, cumWd)
            }
        }

    val balanceOverTime: List<Pair<LocalDateTime, Double>>
        get() {
            val txns = _uiState.value.transactions
            var balance = 0.0
            return txns.map { txn ->
                balance += when (txn.type) {
                    TransactionType.DEPOSIT -> txn.amount
                    TransactionType.WITHDRAWAL, TransactionType.FEE -> -txn.amount
                }
                txn.date to balance
            }
        }

    val totalBalance: Double
        get() {
            var balance = 0.0
            for (txn in _uiState.value.transactions.sortedBy { it.date }) {
                balance += when (txn.type) {
                    TransactionType.DEPOSIT -> txn.amount
                    TransactionType.WITHDRAWAL, TransactionType.FEE -> -txn.amount
                }
            }
            return balance
        }

    val depositCount: Int
        get() = _uiState.value.transactions.count { it.type == TransactionType.DEPOSIT }

    val withdrawalCount: Int
        get() = _uiState.value.transactions.count { it.type != TransactionType.DEPOSIT }

    val averageDeposit: Double
        get() {
            val deposits = _uiState.value.transactions.filter { it.type == TransactionType.DEPOSIT }
            return if (deposits.isEmpty()) 0.0 else deposits.sumOf { it.amount } / deposits.size
        }

    val averageWithdrawal: Double
        get() {
            val withdrawals = _uiState.value.transactions.filter { it.type != TransactionType.DEPOSIT }
            return if (withdrawals.isEmpty()) 0.0 else withdrawals.sumOf { it.amount } / withdrawals.size
        }

    val depositsFrequency: Map<DayOfWeek, Int>
        get() = _uiState.value.transactions
            .filter { it.type == TransactionType.DEPOSIT }
            .groupBy { it.date.dayOfWeek }
            .mapValues { it.value.size }

    fun tableRows(): List<TableRow> {
        val state = _uiState.value
        var filtered = state.transactions.toList()

        // Apply filters
        state.filterType?.let { type ->
            filtered = filtered.filter { it.type == type }
        }
        state.filterYear?.let { year ->
            filtered = filtered.filter { it.date.year == year }
        }
        state.filterMonth?.let { month ->
            filtered = filtered.filter { it.date.monthValue == month }
        }
        if (state.filterCategoryIds.isNotEmpty() || state.filterNoCategory) {
            filtered = filtered.filter {
                (state.filterNoCategory && it.categoryId == null) ||
                        it.categoryId in state.filterCategoryIds
            }
        }

        // Apply search
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()
            filtered = filtered.filter { txn ->
                txn.note.lowercase().contains(query) ||
                        txn.type.name.lowercase().contains(query) ||
                        txn.amount.toString().contains(query)
            }
        }

        // Build balance map first (needed for BALANCE sort)
        var balance = 0.0
        val allSorted = state.transactions.sortedBy { it.date }
        val balanceMap = mutableMapOf<Long, Double>()
        for (txn in allSorted) {
            balance += when (txn.type) {
                TransactionType.DEPOSIT -> txn.amount
                TransactionType.WITHDRAWAL, TransactionType.FEE -> -txn.amount
            }
            balanceMap[txn.id] = balance
        }

        // Apply sort
        filtered = when (state.sortField) {
            SortField.DATE -> when (state.sortOrder) {
                SortOrder.ASC -> filtered.sortedBy { it.date }
                SortOrder.DESC -> filtered.sortedByDescending { it.date }
            }
            SortField.AMOUNT -> when (state.sortOrder) {
                SortOrder.ASC -> filtered.sortedBy { it.amount }
                SortOrder.DESC -> filtered.sortedByDescending { it.amount }
            }
            SortField.BALANCE -> when (state.sortOrder) {
                SortOrder.ASC -> filtered.sortedBy { balanceMap[it.id] ?: 0.0 }
                SortOrder.DESC -> filtered.sortedByDescending { balanceMap[it.id] ?: 0.0 }
            }
        }

        return filtered.map { txn ->
            TableRow(
                id = txn.id,
                date = txn.date,
                type = txn.type,
                amount = txn.amount,
                balanceAfter = balanceMap[txn.id] ?: 0.0,
                hasNote = txn.note.isNotBlank(),
            )
        }
    }

    fun groupedTableRows(): Map<String, List<TableRow>> {
        val rows = tableRows()
        val today = LocalDate.now()
        val startOfWeek = today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
        val startOfMonth = today.withDayOfMonth(1)

        return rows.groupBy { row ->
            val rowDate = row.date.toLocalDate()
            when {
                rowDate == today -> "Today"
                rowDate >= startOfWeek -> "This Week"
                rowDate >= startOfMonth -> "This Month"
                else -> "Older"
            }
        }
    }
}

data class TableRow(
    val id: Long = 0,
    val date: LocalDateTime,
    val type: TransactionType,
    val amount: Double,
    val balanceAfter: Double,
    val hasNote: Boolean = false,
)
