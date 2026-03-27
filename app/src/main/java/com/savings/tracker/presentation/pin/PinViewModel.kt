package com.savings.tracker.presentation.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savings.tracker.data.preferences.PreferencesManager
import com.savings.tracker.data.security.PinEncryption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PinUiState(
    val pin: String = "",
    val confirmPin: String = "",
    val isConfirmStep: Boolean = false,
    val error: String? = null,
    val isPinSet: Boolean = false,
    val failedAttempts: Int = 0,
    val isLockedOut: Boolean = false,
    val lockoutEndTime: Long = 0L,
    val remainingLockoutSeconds: Int = 0,
    val isPostLockout: Boolean = false
)

sealed class PinEvent {
    data object PinSetSuccess : PinEvent()
    data object LoginSuccess : PinEvent()
}

private const val PIN_LENGTH = 6
private const val MAX_ATTEMPTS_BEFORE_LOCKOUT = 3
private const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L

@HiltViewModel
class PinViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(PinUiState())
    val state: StateFlow<PinUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PinEvent>()
    val events: SharedFlow<PinEvent> = _events.asSharedFlow()

    private var lockoutTimerJob: Job? = null

    fun onDigitEntered(digit: Char) {
        _state.update { current ->
            if (current.isLockedOut) return@update current

            val currentPin = if (current.isConfirmStep) current.confirmPin else current.pin
            if (currentPin.length >= PIN_LENGTH) return@update current

            val newPin = currentPin + digit
            val updated = if (current.isConfirmStep) {
                current.copy(confirmPin = newPin, error = null)
            } else {
                current.copy(pin = newPin, error = null)
            }

            updated
        }

        val currentState = _state.value
        val activePin = if (currentState.isConfirmStep) currentState.confirmPin else currentState.pin
        if (activePin.length == PIN_LENGTH) {
            if (currentState.isPinSet) {
                loginWithPin()
            } else {
                setupPin()
            }
        }
    }

    fun onBackspace() {
        _state.update { current ->
            if (current.isConfirmStep) {
                current.copy(confirmPin = current.confirmPin.dropLast(1), error = null)
            } else {
                current.copy(pin = current.pin.dropLast(1), error = null)
            }
        }
    }

    fun onClear() {
        _state.update { current ->
            if (current.isConfirmStep) {
                current.copy(confirmPin = "", error = null)
            } else {
                current.copy(pin = "", error = null)
            }
        }
    }

    fun initForSetup() {
        _state.update { it.copy(isPinSet = false) }
    }

    fun initForLogin() {
        _state.update { it.copy(isPinSet = true) }
    }

    suspend fun isBiometricEnabled(): Boolean {
        return preferencesManager.biometricEnabledFlow.first()
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            preferencesManager.setFailedAttempts(0)
            preferencesManager.setLockoutTimestamp(0)
            _state.update {
                it.copy(failedAttempts = 0, isPostLockout = false, error = null)
            }
            _events.emit(PinEvent.LoginSuccess)
        }
    }

    private fun setupPin() {
        val current = _state.value
        if (!current.isConfirmStep) {
            _state.update {
                it.copy(
                    isConfirmStep = true,
                    confirmPin = "",
                    error = null
                )
            }
        } else {
            if (current.pin == current.confirmPin) {
                val encrypted = PinEncryption.encrypt(current.pin)
                viewModelScope.launch {
                    preferencesManager.setEncryptedPin(encrypted)
                    preferencesManager.setIsPinSet(true)
                    _events.emit(PinEvent.PinSetSuccess)
                }
            } else {
                _state.update {
                    it.copy(
                        confirmPin = "",
                        error = "PINs don't match. Try again."
                    )
                }
            }
        }
    }

    private var storedEncryptedPin: String = ""

    init {
        viewModelScope.launch {
            preferencesManager.encryptedPinFlow.collect { pin ->
                storedEncryptedPin = pin
            }
        }
        viewModelScope.launch {
            preferencesManager.failedAttemptsFlow.collect { attempts ->
                _state.update { it.copy(failedAttempts = attempts) }
            }
        }
        viewModelScope.launch {
            preferencesManager.lockoutTimestampFlow.collect { timestamp ->
                if (timestamp > System.currentTimeMillis()) {
                    _state.update {
                        it.copy(
                            isLockedOut = true,
                            lockoutEndTime = timestamp,
                            isPostLockout = true
                        )
                    }
                    startLockoutTimer()
                }
            }
        }
    }

    private fun loginWithPin() {
        val current = _state.value

        if (current.isLockedOut) return

        if (storedEncryptedPin.isEmpty()) return

        if (PinEncryption.verify(current.pin, storedEncryptedPin)) {
            viewModelScope.launch {
                preferencesManager.setFailedAttempts(0)
                preferencesManager.setLockoutTimestamp(0)
            }
            _state.update {
                it.copy(failedAttempts = 0, isPostLockout = false, error = null)
            }
            viewModelScope.launch {
                _events.emit(PinEvent.LoginSuccess)
            }
        } else {
            val newAttempts = current.failedAttempts + 1
            val shouldLock = if (current.isPostLockout) {
                newAttempts >= 1
            } else {
                newAttempts >= MAX_ATTEMPTS_BEFORE_LOCKOUT
            }

            if (shouldLock) {
                val lockoutEnd = System.currentTimeMillis() + LOCKOUT_DURATION_MS
                viewModelScope.launch {
                    preferencesManager.setFailedAttempts(0)
                    preferencesManager.setLockoutTimestamp(lockoutEnd)
                }
                _state.update {
                    it.copy(
                        pin = "",
                        failedAttempts = 0,
                        isLockedOut = true,
                        lockoutEndTime = lockoutEnd,
                        remainingLockoutSeconds = (LOCKOUT_DURATION_MS / 1000).toInt(),
                        isPostLockout = true,
                        error = null
                    )
                }
                startLockoutTimer()
            } else {
                val remaining = if (current.isPostLockout) {
                    1 - newAttempts
                } else {
                    MAX_ATTEMPTS_BEFORE_LOCKOUT - newAttempts
                }
                _state.update {
                    it.copy(
                        pin = "",
                        failedAttempts = newAttempts,
                        error = "Incorrect PIN. $remaining attempt${if (remaining != 1) "s" else ""} remaining."
                    )
                }
            }
        }
    }

    private fun startLockoutTimer() {
        lockoutTimerJob?.cancel()
        lockoutTimerJob = viewModelScope.launch {
            while (_state.value.isLockedOut) {
                val remaining = (_state.value.lockoutEndTime - System.currentTimeMillis()) / 1000
                if (remaining <= 0) {
                    _state.update {
                        it.copy(
                            isLockedOut = false,
                            remainingLockoutSeconds = 0,
                            lockoutEndTime = 0L,
                            pin = ""
                        )
                    }
                    break
                }
                _state.update {
                    it.copy(remainingLockoutSeconds = remaining.toInt())
                }
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        lockoutTimerJob?.cancel()
    }
}
