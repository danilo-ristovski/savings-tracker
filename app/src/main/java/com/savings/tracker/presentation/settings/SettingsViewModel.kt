package com.savings.tracker.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savings.tracker.data.preferences.PreferencesManager
import com.savings.tracker.data.security.PinEncryption
import com.savings.tracker.domain.model.ThemeMode
import com.savings.tracker.domain.usecase.DeleteAllDataUseCase
import com.savings.tracker.domain.usecase.ExportDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val monthlyFee: String = "",
    val showDeleteConfirmation: Boolean = false,
    val exportResult: String? = null,
    val isExporting: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val exportDataUseCase: ExportDataUseCase,
    private val deleteAllDataUseCase: DeleteAllDataUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        preferencesManager.themeModeFlow
            .onEach { name ->
                val mode = try { ThemeMode.valueOf(name) } catch (_: Exception) { ThemeMode.SYSTEM }
                _uiState.update { it.copy(themeMode = mode) }
            }
            .launchIn(viewModelScope)

        preferencesManager.monthlyFeeFlow
            .onEach { fee ->
                _uiState.update {
                    it.copy(monthlyFee = if (fee == 0.0) "" else fee.toBigDecimal().stripTrailingZeros().toPlainString())
                }
            }
            .launchIn(viewModelScope)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode.name)
        }
    }

    fun setMonthlyFee(value: String) {
        val sanitized = value.replace(Regex("[^0-9.]"), "")
        if (sanitized.count { it == '.' } > 1) return
        _uiState.update { it.copy(monthlyFee = sanitized) }
        val amount = sanitized.toDoubleOrNull() ?: 0.0
        viewModelScope.launch {
            preferencesManager.setMonthlyFee(amount)
        }
    }

    fun exportData(onShareUri: (Uri) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportResult = null) }
            try {
                val encryptedPin = preferencesManager.encryptedPinFlow.first()
                val pin = PinEncryption.decrypt(encryptedPin)
                val uri = exportDataUseCase(pin)
                _uiState.update { it.copy(isExporting = false, exportResult = "Export successful") }
                onShareUri(uri)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isExporting = false, exportResult = "Export failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun backupData(onShareUri: (Uri) -> Unit) {
        exportData(onShareUri)
    }

    fun requestDeleteAllData() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun confirmDelete() {
        viewModelScope.launch {
            deleteAllDataUseCase()
            _uiState.update { it.copy(showDeleteConfirmation = false) }
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportResult = null) }
    }

    fun resetPin(newPin: String) {
        viewModelScope.launch {
            val encrypted = PinEncryption.encrypt(newPin)
            preferencesManager.setEncryptedPin(encrypted)
            preferencesManager.setFailedAttempts(0)
            preferencesManager.setLockoutTimestamp(0)
        }
    }
}
