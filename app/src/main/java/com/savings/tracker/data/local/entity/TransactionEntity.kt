package com.savings.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.savings.tracker.domain.model.Transaction
import com.savings.tracker.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: String,
    val date: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

fun TransactionEntity.toTransaction(): Transaction {
    return Transaction(
        id = id,
        amount = amount,
        type = TransactionType.valueOf(type),
        date = LocalDateTime.ofInstant(Instant.ofEpochMilli(date), ZoneId.systemDefault()),
        note = note,
        createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneId.systemDefault()),
        deletedAt = deletedAt?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        amount = amount,
        type = type.name,
        date = date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        note = note,
        createdAt = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        deletedAt = deletedAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    )
}
