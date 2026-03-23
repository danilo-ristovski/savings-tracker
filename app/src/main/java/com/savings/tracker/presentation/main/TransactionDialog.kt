package com.savings.tracker.presentation.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.savings.tracker.domain.model.Transaction
import com.savings.tracker.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDialog(
    type: TransactionType,
    currentBalance: Double,
    onConfirm: (amount: Double, date: LocalDateTime, note: String) -> Unit,
    onDismiss: () -> Unit,
    editTransaction: Transaction? = null
) {
    var amountText by remember { mutableStateOf(editTransaction?.amount?.let { "%.2f".format(it) } ?: "") }
    var note by remember { mutableStateOf(editTransaction?.note ?: "") }
    var selectedDate by remember { mutableStateOf(editTransaction?.date?.toLocalDate() ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy.") }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val editOriginalEffect = if (editTransaction != null) {
        when (editTransaction.type) {
            TransactionType.DEPOSIT -> -editTransaction.amount
            TransactionType.WITHDRAWAL, TransactionType.FEE -> editTransaction.amount
        }
    } else 0.0
    val adjustedBalance = currentBalance + editOriginalEffect
    val balanceAfter = if (type == TransactionType.DEPOSIT) {
        adjustedBalance + amount
    } else {
        adjustedBalance - amount
    }
    val isWithdrawalExceedsBalance = type == TransactionType.WITHDRAWAL && balanceAfter < 0
    val isValid = amount > 0

    val title = if (editTransaction != null) {
        "Edit Transaction"
    } else if (type == TransactionType.DEPOSIT) {
        "Add to Savings"
    } else {
        "Withdraw from Savings"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text("Select transaction date") } },
                    state = rememberTooltipState()
                ) {
                    FilledTonalButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(selectedDate.format(dateFormatter))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amountText = newValue
                        }
                    },
                    label = { Text("Amount") },
                    suffix = { Text("RSD") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                val balanceAfterColor = if (balanceAfter >= 0) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.error
                }

                Text(
                    text = "Balance after: ${"%.2f".format(balanceAfter)} RSD",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = balanceAfterColor
                )

                if (isWithdrawalExceedsBalance) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Warning: Withdrawal exceeds current balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text("Confirm transaction") } },
                state = rememberTooltipState()
            ) {
                TextButton(
                    onClick = {
                        onConfirm(
                            amount,
                            LocalDateTime.of(selectedDate, editTransaction?.date?.toLocalTime() ?: LocalTime.now()),
                            note
                        )
                    },
                    enabled = isValid
                ) {
                    Text(if (editTransaction != null) "Save" else "Confirm")
                }
            }
        },
        dismissButton = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text("Cancel transaction") } },
                state = rememberTooltipState()
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text("Confirm date") } },
                    state = rememberTooltipState()
                ) {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                selectedDate = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text("Cancel date selection") } },
                    state = rememberTooltipState()
                ) {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
