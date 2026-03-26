package com.savings.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.savings.tracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL AND date BETWEEN :start AND :end ORDER BY date DESC")
    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN type = 'DEPOSIT' THEN amount " +
                "WHEN type IN ('WITHDRAWAL', 'FEE') THEN -amount ELSE 0 END), 0.0) " +
                "FROM transactions WHERE deletedAt IS NULL"
    )
    fun getTotalBalance(): Flow<Double>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY date DESC LIMIT 1")
    fun getLastTransaction(): Flow<TransactionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    @androidx.room.Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY date DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("UPDATE transactions SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long)

    @Query("UPDATE transactions SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreDeleted(id: Long)

    @Query("SELECT * FROM transactions WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedTransactions(): Flow<List<TransactionEntity>>

    @Query("DELETE FROM transactions WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun permanentlyDeleteOlderThan(cutoff: Long)

    @Query("DELETE FROM transactions WHERE id = :id AND deletedAt IS NOT NULL")
    suspend fun permanentlyDelete(id: Long)

    @Query("DELETE FROM transactions WHERE deletedAt IS NOT NULL")
    suspend fun emptyTrash()
}
