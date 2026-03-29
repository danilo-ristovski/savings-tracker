package com.savings.tracker.di

import android.content.Context
import androidx.room.Room
import com.savings.tracker.data.local.SavingsDatabase
import com.savings.tracker.data.local.dao.CategoryDao
import com.savings.tracker.data.local.dao.TransactionDao
import com.savings.tracker.data.repository.CategoryRepositoryImpl
import com.savings.tracker.data.repository.TransactionRepositoryImpl
import com.savings.tracker.domain.repository.CategoryRepository
import com.savings.tracker.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl
    ): CategoryRepository

    companion object {

        @Provides
        @Singleton
        fun provideSavingsDatabase(
            @ApplicationContext context: Context
        ): SavingsDatabase {
            return Room.databaseBuilder(
                context,
                SavingsDatabase::class.java,
                "savings_database"
            ).addMigrations(
                SavingsDatabase.MIGRATION_1_2,
                SavingsDatabase.MIGRATION_2_3,
                SavingsDatabase.MIGRATION_3_4,
                SavingsDatabase.MIGRATION_4_5,
                SavingsDatabase.MIGRATION_5_6,
            ).build()
        }

        @Provides
        @Singleton
        fun provideTransactionDao(database: SavingsDatabase): TransactionDao {
            return database.transactionDao()
        }

        @Provides
        @Singleton
        fun provideCategoryDao(database: SavingsDatabase): CategoryDao {
            return database.categoryDao()
        }
    }
}
