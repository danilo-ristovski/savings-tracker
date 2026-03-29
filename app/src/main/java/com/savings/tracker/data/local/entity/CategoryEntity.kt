package com.savings.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.savings.tracker.domain.model.Category
import com.savings.tracker.domain.model.CategoryType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val notes: String = "",
    val isPredefined: Boolean = false,
    val type: String = "ANY",
)

fun CategoryEntity.toCategory(): Category {
    return Category(
        id = id,
        name = name,
        notes = notes,
        isPredefined = isPredefined,
        type = CategoryType.valueOf(type),
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        notes = notes,
        isPredefined = isPredefined,
        type = type.name,
    )
}
