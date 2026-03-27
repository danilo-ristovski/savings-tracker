package com.savings.tracker.presentation.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

enum class ChartType { LINE, BAR, PIE }
enum class SortField { DATE, AMOUNT }
enum class SortOrder { ASC, DESC }

data class TrendsUiState(
    val transactions: List<Transaction> = emptyList(),
    val selectedTab: Int = 0,
    val selectedChartType: ChartType = ChartType.LINE,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val filterType: TransactionType? = null,
    val sortField: SortField = SortField.DATE,
    val sortOrder: SortOrder = SortOrder.ASC,
)

@HiltViewModel
class TrendsViewModel @Inject constructor(
    getTransactionsUseCase: GetTransactionsUseCase,
    private val repository: TransactionRepository,
    categoryRepository: CategoryRepository,
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
        _uiState.update { it.copy(filterType = type) }
    }

    fun setSortOrder(field: SortField, order: SortOrder) {
        _uiState.update { it.copy(sortField = field, sortOrder = order) }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
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

        // Apply filter
        state.filterType?.let { type ->
            filtered = filtered.filter { it.type == type }
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
        }

        var balance = 0.0
        // Recalculate running balance based on all transactions in date order
        val allSorted = state.transactions.sortedBy { it.date }
        val balanceMap = mutableMapOf<Long, Double>()
        for (txn in allSorted) {
            balance += when (txn.type) {
                TransactionType.DEPOSIT -> txn.amount
                TransactionType.WITHDRAWAL, TransactionType.FEE -> -txn.amount
            }
            balanceMap[txn.id] = balance
        }

        return filtered.map { txn ->
            TableRow(
                id = txn.id,
                date = txn.date,
                type = txn.type,
                amount = txn.amount,
                balanceAfter = balanceMap[txn.id] ?: 0.0,
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
)
