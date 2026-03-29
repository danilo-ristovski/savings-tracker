package com.savings.tracker.presentation.balance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savings.tracker.domain.model.Category
import com.savings.tracker.domain.model.Transaction
import com.savings.tracker.domain.model.TransactionType
import com.savings.tracker.domain.repository.CategoryRepository
import com.savings.tracker.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import javax.inject.Inject

data class MonthSummary(
    val yearMonth: YearMonth,
    val startingBalance: Double,
    val endingBalance: Double,
    val deposits: Double,
    val withdrawals: Double,
    val transactions: List<Transaction> = emptyList(),
) {
    val netChange: Double get() = endingBalance - startingBalance
}

enum class MonthlySortBy { MONTH, CHANGE, ENDING_BALANCE }

data class MonthlyBalanceUiState(
    val isLoading: Boolean = true,
    val years: List<Int> = emptyList(),
    val selectedYear: Int? = null,
    val monthlySummaries: List<MonthSummary> = emptyList(),
    val yearSummary: Double = 0.0,
    val sortBy: MonthlySortBy = MonthlySortBy.MONTH,
    val sortDescending: Boolean = true,
    val selectedMainTab: Int = 0,
    val transactionTypeFilter: Set<TransactionType> = emptySet(),
)

@HiltViewModel
class MonthlyBalanceViewModel @Inject constructor(
    getTransactionsUseCase: GetTransactionsUseCase,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthlyBalanceUiState())
    val uiState: StateFlow<MonthlyBalanceUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private var allSummaries: List<MonthSummary> = emptyList()

    init {
        categoryRepository.getAllCategories()
            .onEach { _categories.value = it }
            .launchIn(viewModelScope)

        getTransactionsUseCase()
            .onEach { transactions ->
                val sorted = transactions.sortedBy { it.date }

                // Build monthly summaries with running balance
                val byMonth = sorted.groupBy { YearMonth.from(it.date) }
                val allMonths = byMonth.keys.sorted()
                var runningBalance = 0.0
                val summaries = allMonths.map { ym ->
                    val txns = byMonth[ym] ?: emptyList()
                    val startBal = runningBalance
                    val deposits = txns.filter { it.type == TransactionType.DEPOSIT }.sumOf { it.amount }
                    val withdrawals = txns.filter { it.type != TransactionType.DEPOSIT }.sumOf { it.amount }
                    runningBalance += deposits - withdrawals
                    MonthSummary(
                        yearMonth = ym,
                        startingBalance = startBal,
                        endingBalance = runningBalance,
                        deposits = deposits,
                        withdrawals = withdrawals,
                        transactions = txns.sortedBy { it.date },
                    )
                }

                allSummaries = summaries
                val years = summaries.map { it.yearMonth.year }.distinct().sortedDescending()
                val selectedYear = _uiState.value.selectedYear ?: years.firstOrNull()

                _uiState.update { state ->
                    val filtered = summaries.filter { s -> s.yearMonth.year == selectedYear }
                    state.copy(
                        isLoading = false,
                        years = years,
                        selectedYear = selectedYear,
                        monthlySummaries = filtered.applySorting(state.sortBy, state.sortDescending),
                        yearSummary = filtered.sumOf { s -> s.netChange },
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun selectYear(year: Int) {
        _uiState.update {
            val filtered = allSummaries.filter { s -> s.yearMonth.year == year }
            it.copy(
                selectedYear = year,
                monthlySummaries = filtered.applySorting(it.sortBy, it.sortDescending),
                yearSummary = filtered.sumOf { s -> s.netChange },
            )
        }
    }

    fun selectMainTab(index: Int) {
        _uiState.update { it.copy(selectedMainTab = index) }
    }

    fun toggleTransactionTypeFilter(type: TransactionType) {
        _uiState.update { state ->
            val current = state.transactionTypeFilter
            val updated = if (type in current) current - type else current + type
            state.copy(transactionTypeFilter = updated)
        }
    }

    fun setSortBy(sortBy: MonthlySortBy) {
        _uiState.update { state ->
            val newDescending = if (state.sortBy == sortBy) !state.sortDescending else true
            val year = state.selectedYear
            val filtered = if (year != null) allSummaries.filter { it.yearMonth.year == year } else emptyList()
            state.copy(
                sortBy = sortBy,
                sortDescending = newDescending,
                monthlySummaries = filtered.applySorting(sortBy, newDescending),
            )
        }
    }
}

private fun List<MonthSummary>.applySorting(sortBy: MonthlySortBy, descending: Boolean): List<MonthSummary> {
    val sorted = when (sortBy) {
        MonthlySortBy.MONTH -> sortedBy { it.yearMonth }
        MonthlySortBy.CHANGE -> sortedBy { it.netChange }
        MonthlySortBy.ENDING_BALANCE -> sortedBy { it.endingBalance }
    }
    return if (descending) sorted.reversed() else sorted
}
