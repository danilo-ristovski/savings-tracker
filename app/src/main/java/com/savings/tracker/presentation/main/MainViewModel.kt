package com.savings.tracker.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savings.tracker.data.preferences.PreferencesManager
import com.savings.tracker.domain.model.Category
import com.savings.tracker.domain.model.SavingsTips
import com.savings.tracker.domain.model.TransactionType
import com.savings.tracker.domain.model.Transaction
import com.savings.tracker.domain.repository.CategoryRepository
import com.savings.tracker.domain.usecase.AddTransactionUseCase
import com.savings.tracker.domain.usecase.ApplyMonthlyFeeUseCase
import com.savings.tracker.domain.usecase.GetBalanceUseCase
import com.savings.tracker.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class MainUiState(
    val balance: Double = 0.0,
    val lastUpdateDate: LocalDateTime? = null,
    val lastChange: Double? = null,
    val lastChangeType: TransactionType? = null,
    val isLoading: Boolean = true,
    val monthlyFee: Double = 0.0,
    val categories: List<Category> = emptyList(),
    val currentTip: String? = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getBalanceUseCase: GetBalanceUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val applyMonthlyFeeUseCase: ApplyMonthlyFeeUseCase,
    private val preferencesManager: PreferencesManager,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private var tipIndex = -1

    init {
        viewModelScope.launch {
            applyMonthlyFeeUseCase()
        }

        viewModelScope.launch {
            getBalanceUseCase().collect { balance ->
                _state.update {
                    it.copy(balance = balance, isLoading = false)
                }
            }
        }

        viewModelScope.launch {
            getTransactionsUseCase().collect { transactions ->
                val lastTransaction = transactions.firstOrNull()
                _state.update {
                    it.copy(
                        lastUpdateDate = lastTransaction?.date,
                        lastChange = lastTransaction?.amount,
                        lastChangeType = lastTransaction?.type
                    )
                }
            }
        }

        viewModelScope.launch {
            preferencesManager.monthlyFeeFlow.collect { fee ->
                _state.update { it.copy(monthlyFee = fee) }
            }
        }

        categoryRepository.getAllCategories()
            .onEach { cats ->
                _state.update { it.copy(categories = cats) }
            }
            .launchIn(viewModelScope)
    }

    fun addTransaction(
        amount: Double,
        date: LocalDateTime,
        type: TransactionType,
        note: String,
        categoryId: Long? = null,
    ) {
        viewModelScope.launch {
            addTransactionUseCase(
                Transaction(
                    amount = amount,
                    date = date,
                    type = type,
                    note = note,
                    categoryId = categoryId,
                )
            )
        }
    }

    fun showNextTip() {
        tipIndex = (tipIndex + 1) % SavingsTips.tips.size
        _state.update { it.copy(currentTip = SavingsTips.tips[tipIndex]) }
    }

    fun dismissTip() {
        _state.update { it.copy(currentTip = null) }
    }
}
