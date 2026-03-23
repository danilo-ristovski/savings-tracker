package com.savings.tracker.data.repository

import com.savings.tracker.data.local.dao.TransactionDao
import com.savings.tracker.data.local.entity.toTransaction
import com.savings.tracker.data.local.entity.toEntity
import com.savings.tracker.domain.model.Transaction
import com.savings.tracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    override fun getTransactionsByDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<Transaction>> {
        val startMillis = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return transactionDao.getTransactionsByDateRange(startMillis, endMillis).map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    override fun getTotalBalance(): Flow<Double> {
        return transactionDao.getTotalBalance()
    }

    override fun getLastTransaction(): Flow<Transaction?> {
        return transactionDao.getLastTransaction().map { it?.toTransaction() }
    }

    override suspend fun addTransaction(transaction: Transaction) {
        transactionDao.insert(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction.toEntity())
    }

    override suspend fun deleteAllTransactions() {
        transactionDao.deleteAll()
    }

    override suspend fun getAllTransactionsList(): List<Transaction> {
        return transactionDao.getAllTransactionsList().map { it.toTransaction() }
    }
}
