package com.savings.tracker.data.repository

import com.savings.tracker.data.local.dao.CategoryDao
import com.savings.tracker.data.local.entity.CategoryEntity
import com.savings.tracker.data.local.entity.toCategory
import com.savings.tracker.data.local.entity.toEntity
import com.savings.tracker.domain.model.Category
import com.savings.tracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toCategory() }
        }
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toCategory()
    }

    override suspend fun addCategory(category: Category): Long {
        return categoryDao.insert(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.update(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category.toEntity())
    }

    override suspend fun seedPredefinedIfEmpty() {
        if (categoryDao.count() == 0) {
            categoryDao.insert(
                CategoryEntity(
                    name = "savings_part_1",
                    notes = "After the payment on the last day of the ongoing month",
                    isPredefined = false
                )
            )
            categoryDao.insert(
                CategoryEntity(
                    name = "savings_part_2",
                    notes = "After the payment on the 10th day of the following month",
                    isPredefined = false
                )
            )
        }
    }
}
