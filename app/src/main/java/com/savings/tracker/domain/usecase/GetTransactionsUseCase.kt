package com.savings.tracker.domain.usecase

import com.savings.tracker.domain.model.Transaction
import com.savings.tracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> {
        return repository.getAllTransactions()
    }

    fun byDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<Transaction>> {
        return repository.getTransactionsByDateRange(start, end)
    }
}
