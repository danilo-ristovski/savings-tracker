package com.savings.tracker.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val notes: String = "",
    val isPredefined: Boolean = false,
    val type: CategoryType = CategoryType.ANY,
)
