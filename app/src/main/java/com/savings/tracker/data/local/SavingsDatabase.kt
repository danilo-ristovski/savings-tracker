package com.savings.tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.savings.tracker.data.local.dao.CategoryDao
import com.savings.tracker.data.local.dao.TransactionDao
import com.savings.tracker.data.local.entity.CategoryEntity
import com.savings.tracker.data.local.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class],
    version = 6,
    exportSchema = false
)
abstract class SavingsDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create categories table
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS categories (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "notes TEXT NOT NULL DEFAULT '', " +
                            "isPredefined INTEGER NOT NULL DEFAULT 0)"
                )
                // Add categoryId column to transactions
                db.execSQL("ALTER TABLE transactions ADD COLUMN categoryId INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN type TEXT NOT NULL DEFAULT 'ANY'")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Remove predefined status from savings_part_1 and savings_part_2
                db.execSQL("UPDATE categories SET isPredefined = 0 WHERE name IN ('savings_part_1', 'savings_part_2')")
            }
        }
    }
}
