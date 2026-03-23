package com.savings.tracker.domain.usecase

import com.savings.tracker.data.preferences.PreferencesManager
import com.savings.tracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DeleteAllDataUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke() {
        repository.deleteAllTransactions()

        // Preserve PIN and theme, reset everything else
        val encryptedPin = preferencesManager.encryptedPinFlow.first()
        val isPinSet = preferencesManager.isPinSetFlow.first()
        val themeMode = preferencesManager.themeModeFlow.first()

        preferencesManager.clearAll()

        preferencesManager.setEncryptedPin(encryptedPin)
        preferencesManager.setIsPinSet(isPinSet)
        preferencesManager.setThemeMode(themeMode)
    }
}
