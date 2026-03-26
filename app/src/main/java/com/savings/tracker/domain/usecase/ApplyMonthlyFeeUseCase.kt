package com.savings.tracker.domain.usecase

import com.savings.tracker.data.preferences.PreferencesManager
import com.savings.tracker.domain.model.Transaction
import com.savings.tracker.domain.model.TransactionType
import com.savings.tracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ApplyMonthlyFeeUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val preferencesManager: PreferencesManager
) {
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    suspend operator fun invoke() {
        val now = LocalDateTime.now()
        if (now.dayOfMonth != 1) return

        val currentMonth = now.format(monthFormatter)
        val lastFeeAppliedMonth = preferencesManager.lastFeeAppliedMonthFlow.first()
        val monthlyFee = preferencesManager.monthlyFeeFlow.first()

        if (currentMonth != lastFeeAppliedMonth && monthlyFee > 0.0) {
            val feeTransaction = Transaction(
                amount = monthlyFee,
                type = TransactionType.FEE,
                date = now,
                note = "Monthly fee for $currentMonth"
            )
            repository.addTransaction(feeTransaction)
            preferencesManager.setLastFeeAppliedMonth(currentMonth)
        }
    }
}
