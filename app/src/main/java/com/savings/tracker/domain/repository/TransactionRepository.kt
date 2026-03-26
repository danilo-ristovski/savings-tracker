package com.savings.tracker.domain.repository

import com.savings.tracker.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<Transaction>>
    fun getTotalBalance(): Flow<Double>
    fun getLastTransaction(): Flow<Transaction?>
    suspend fun addTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun deleteAllTransactions()
    suspend fun getAllTransactionsList(): List<Transaction>
    suspend fun softDeleteTransaction(id: Long)
    suspend fun restoreTransaction(id: Long)
    fun getDeletedTransactions(): Flow<List<Transaction>>
    suspend fun permanentlyDeleteOlderThan(cutoff: LocalDateTime)
    suspend fun permanentlyDeleteTransaction(transaction: Transaction)
    suspend fun emptyTrash()
    suspend fun upsertTransactions(transactions: List<Transaction>)
}
