package com.savings.tracker.domain.usecase

import com.savings.tracker.domain.model.Transaction
import com.savings.tracker.domain.repository.TransactionRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        repository.addTransaction(transaction)
    }
}
