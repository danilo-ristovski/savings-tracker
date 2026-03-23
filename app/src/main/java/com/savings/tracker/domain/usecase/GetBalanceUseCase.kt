package com.savings.tracker.domain.usecase

import com.savings.tracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBalanceUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<Double> {
        return repository.getTotalBalance()
    }
}
