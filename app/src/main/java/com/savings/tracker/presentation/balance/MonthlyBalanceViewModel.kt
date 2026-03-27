package com.savings.tracker.presentation.balance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savings.tracker.domain.model.TransactionType
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
) {
    val netChange: Double get() = endingBalance - startingBalance
}

data class MonthlyBalanceUiState(
    val isLoading: Boolean = true,
    val years: List<Int> = emptyList(),
    val selectedYear: Int? = null,
    val monthlySummaries: List<MonthSummary> = emptyList(),
    val yearSummary: Double = 0.0,
)

@HiltViewModel
class MonthlyBalanceViewModel @Inject constructor(
    getTransactionsUseCase: GetTransactionsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthlyBalanceUiState())
    val uiState: StateFlow<MonthlyBalanceUiState> = _uiState.asStateFlow()

    private var allSummaries: List<MonthSummary> = emptyList()

    init {
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
                    )
                }

                allSummaries = summaries
                val years = summaries.map { it.yearMonth.year }.distinct().sortedDescending()
                val selectedYear = _uiState.value.selectedYear ?: years.firstOrNull()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        years = years,
                        selectedYear = selectedYear,
                        monthlySummaries = summaries.filter { s -> s.yearMonth.year == selectedYear }
                            .sortedByDescending { s -> s.yearMonth },
                        yearSummary = summaries.filter { s -> s.yearMonth.year == selectedYear }
                            .sumOf { s -> s.netChange },
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun selectYear(year: Int) {
        _uiState.update {
            it.copy(
                selectedYear = year,
                monthlySummaries = allSummaries.filter { s -> s.yearMonth.year == year }
                    .sortedByDescending { s -> s.yearMonth },
                yearSummary = allSummaries.filter { s -> s.yearMonth.year == year }
                    .sumOf { s -> s.netChange },
            )
        }
    }
}
