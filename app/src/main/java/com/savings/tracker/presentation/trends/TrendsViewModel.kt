package com.savings.tracker.presentation.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savings.tracker.domain.model.Transaction
import com.savings.tracker.domain.model.TransactionType
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class ChartType { LINE, BAR, PIE }

data class TrendsUiState(
    val transactions: List<Transaction> = emptyList(),
    val selectedTab: Int = 0,
    val selectedChartType: ChartType = ChartType.LINE,
    val isLoading: Boolean = true,
)

@HiltViewModel
class TrendsViewModel @Inject constructor(
    getTransactionsUseCase: GetTransactionsUseCase,
    private val repository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendsUiState())
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

    private val monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

    init {
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

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
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
        var balance = 0.0
        return _uiState.value.transactions.map { txn ->
            balance += when (txn.type) {
                TransactionType.DEPOSIT -> txn.amount
                TransactionType.WITHDRAWAL, TransactionType.FEE -> -txn.amount
            }
            TableRow(
                id = txn.id,
                date = txn.date,
                type = txn.type,
                amount = txn.amount,
                balanceAfter = balance,
            )
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
