package com.savings.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.savings.tracker.domain.model.Category

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val notes: String = "",
    val isPredefined: Boolean = false
)

fun CategoryEntity.toCategory(): Category {
    return Category(
        id = id,
        name = name,
        notes = notes,
        isPredefined = isPredefined
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        notes = notes,
        isPredefined = isPredefined
    )
}
