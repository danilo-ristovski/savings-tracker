package com.savings.tracker.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savings.tracker.data.preferences.PreferencesManager
import com.savings.tracker.data.security.PinEncryption
import com.savings.tracker.domain.model.AnalysisSection
import com.savings.tracker.domain.model.Category
import com.savings.tracker.domain.model.CategoryType
import com.savings.tracker.domain.model.ThemeMode
import com.savings.tracker.presentation.trends.ChartType
import com.savings.tracker.domain.model.Transaction
import com.savings.tracker.domain.model.TransactionType
import com.savings.tracker.domain.repository.CategoryRepository
import com.savings.tracker.domain.repository.TransactionRepository
import com.savings.tracker.domain.usecase.DeleteAllDataUseCase
import com.savings.tracker.domain.usecase.ExportDataUseCase
import com.savings.tracker.domain.usecase.ImportDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val monthlyFee: String = "",
    val showDeleteConfirmation: Boolean = false,
    val exportResult: String? = null,
    val importResult: String? = null,
    val isExporting: Boolean = false,
    val isDemoMode: Boolean = false,
    val categories: List<Category> = emptyList(),
    val isBiometricEnabled: Boolean = false,
    val isShakeLogoutEnabled: Boolean = false,
    val isAutoBlurEnabled: Boolean = false,
    val analysisHiddenSections: Set<String> = emptySet(),
    val hiddenChartTypes: Set<String> = emptySet(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val exportDataUseCase: ExportDataUseCase,
    private val deleteAllDataUseCase: DeleteAllDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val repository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
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
                    it.copy(monthlyFee = if (fee == 0.0) "" else fee.toLong().toString())
                }
            }
            .launchIn(viewModelScope)

        preferencesManager.demoModeFlow
            .onEach { enabled ->
                _uiState.update { it.copy(isDemoMode = enabled) }
            }
            .launchIn(viewModelScope)

        categoryRepository.getAllCategories()
            .onEach { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
            .launchIn(viewModelScope)

        preferencesManager.biometricEnabledFlow
            .onEach { enabled ->
                _uiState.update { it.copy(isBiometricEnabled = enabled) }
            }
            .launchIn(viewModelScope)

        preferencesManager.shakeLogoutEnabledFlow
            .onEach { enabled ->
                _uiState.update { it.copy(isShakeLogoutEnabled = enabled) }
            }
            .launchIn(viewModelScope)

        preferencesManager.autoBlurEnabledFlow
            .onEach { enabled ->
                _uiState.update { it.copy(isAutoBlurEnabled = enabled) }
            }
            .launchIn(viewModelScope)

        preferencesManager.analysisHiddenSectionsFlow
            .onEach { sections ->
                _uiState.update { it.copy(analysisHiddenSections = sections) }
            }
            .launchIn(viewModelScope)

        preferencesManager.chartHiddenTypesFlow
            .onEach { types ->
                _uiState.update { it.copy(hiddenChartTypes = types) }
            }
            .launchIn(viewModelScope)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode.name)
        }
    }

    fun setMonthlyFee(value: String) {
        val sanitized = value.replace(Regex("[^0-9]"), "")
        _uiState.update { it.copy(monthlyFee = sanitized) }
        val amount = sanitized.toDoubleOrNull() ?: 0.0
        viewModelScope.launch {
            preferencesManager.setMonthlyFee(amount)
        }
    }

    fun exportDataToUri(destinationUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportResult = null) }
            try {
                val encryptedPin = preferencesManager.encryptedPinFlow.first()
                val pin = PinEncryption.decrypt(encryptedPin)
                exportDataUseCase.exportZip(destinationUri, pin)
                _uiState.update { it.copy(isExporting = false, exportResult = "Export successful") }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isExporting = false, exportResult = "Export failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(importResult = null) }
            try {
                val encryptedPin = preferencesManager.encryptedPinFlow.first()
                val pin = PinEncryption.decrypt(encryptedPin)
                importDataUseCase(uri, pin)
                _uiState.update { it.copy(importResult = "Import successful") }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(importResult = "Import failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun clearImportResult() {
        _uiState.update { it.copy(importResult = null) }
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

    fun toggleDemoMode() {
        viewModelScope.launch {
            val current = _uiState.value.isDemoMode
            if (!current) {
                val now = LocalDateTime.now()
                val demoTransactions = listOf(
                    Transaction(note = "Initial deposit", amount = 50000.0, type = TransactionType.DEPOSIT, date = now.minusMonths(3)),
                    Transaction(note = "Salary savings", amount = 25000.0, type = TransactionType.DEPOSIT, date = now.minusMonths(2).plusDays(5)),
                    Transaction(note = "Freelance payment", amount = 15000.0, type = TransactionType.DEPOSIT, date = now.minusMonths(2).plusDays(15)),
                    Transaction(note = "Emergency fund", amount = 10000.0, type = TransactionType.DEPOSIT, date = now.minusMonths(1).plusDays(1)),
                    Transaction(note = "Phone bill", amount = 3500.0, type = TransactionType.WITHDRAWAL, date = now.minusMonths(1).plusDays(10)),
                    Transaction(note = "Monthly bank fee", amount = 200.0, type = TransactionType.FEE, date = now.minusMonths(1)),
                    Transaction(note = "Bonus savings", amount = 20000.0, type = TransactionType.DEPOSIT, date = now.minusWeeks(2)),
                    Transaction(note = "Groceries", amount = 5500.0, type = TransactionType.WITHDRAWAL, date = now.minusWeeks(1)),
                    Transaction(note = "Monthly bank fee", amount = 200.0, type = TransactionType.FEE, date = now.minusDays(3)),
                    Transaction(note = "Side project income", amount = 8000.0, type = TransactionType.DEPOSIT, date = now.minusDays(1)),
                )
                repository.upsertTransactions(demoTransactions)
            } else {
                repository.deleteAllTransactions()
            }
            preferencesManager.setDemoMode(!current)
        }
    }

    // Category CRUD
    fun addCategory(name: String, notes: String, type: CategoryType = CategoryType.ANY) {
        viewModelScope.launch {
            categoryRepository.addCategory(Category(name = name, notes = notes, type = type))
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    // Biometric
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setBiometricEnabled(enabled)
        }
    }

    // Shake logout
    fun setShakeLogoutEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setShakeLogoutEnabled(enabled)
        }
    }

    // Auto blur
    fun setAutoBlurEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAutoBlurEnabled(enabled)
        }
    }

    // Chart types
    fun toggleChartType(type: String) {
        viewModelScope.launch {
            val current = _uiState.value.hiddenChartTypes.toMutableSet()
            if (type in current) current.remove(type) else current.add(type)
            preferencesManager.setChartHiddenTypes(current)
        }
    }

    fun setAllChartTypesVisible(visible: Boolean) {
        viewModelScope.launch {
            val newSet = if (visible) emptySet() else ChartType.entries.map { it.name }.toSet()
            preferencesManager.setChartHiddenTypes(newSet)
        }
    }

    // Analysis sections
    fun toggleAnalysisSection(section: String) {
        viewModelScope.launch {
            val current = _uiState.value.analysisHiddenSections.toMutableSet()
            if (section in current) current.remove(section) else current.add(section)
            preferencesManager.setAnalysisHiddenSections(current)
        }
    }

    fun setAllAnalysisSectionsVisible(visible: Boolean) {
        viewModelScope.launch {
            val newSet = if (visible) emptySet() else AnalysisSection.entries.map { it.name }.toSet()
            preferencesManager.setAnalysisHiddenSections(newSet)
        }
    }
}
