package com.savings.tracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ExportData(
    val version: Int = 1,
    val transactions: List<Transaction>
)
