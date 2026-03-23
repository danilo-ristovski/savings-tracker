package com.savings.tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.savings.tracker.data.local.dao.TransactionDao
import com.savings.tracker.data.local.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SavingsDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}
