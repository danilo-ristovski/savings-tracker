package com.savings.tracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "savings_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val ENCRYPTED_PIN = stringPreferencesKey("encrypted_pin")
        val IS_PIN_SET = booleanPreferencesKey("is_pin_set")
        val MONTHLY_FEE = floatPreferencesKey("monthly_fee")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val LOCKOUT_TIMESTAMP = longPreferencesKey("lockout_timestamp")
        val LAST_FEE_APPLIED_MONTH = stringPreferencesKey("last_fee_applied_month")
        val IS_LOCKED_PERMANENTLY = booleanPreferencesKey("is_locked_permanently")
        val TRASH_RETENTION_DAYS = intPreferencesKey("trash_retention_days")
        val DEMO_MODE = booleanPreferencesKey("demo_mode")
    }

    // Encrypted PIN
    val encryptedPinFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.ENCRYPTED_PIN] ?: ""
    }

    suspend fun setEncryptedPin(value: String) {
        context.dataStore.edit { prefs -> prefs[Keys.ENCRYPTED_PIN] = value }
    }

    // Is PIN Set
    val isPinSetFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_PIN_SET] ?: false
    }

    suspend fun setIsPinSet(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.IS_PIN_SET] = value }
    }

    // Monthly Fee
    val monthlyFeeFlow: Flow<Double> = context.dataStore.data.map { prefs ->
        (prefs[Keys.MONTHLY_FEE] ?: 0f).toDouble()
    }

    suspend fun setMonthlyFee(value: Double) {
        context.dataStore.edit { prefs -> prefs[Keys.MONTHLY_FEE] = value.toFloat() }
    }

    // Theme Mode
    val themeModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: "SYSTEM"
    }

    suspend fun setThemeMode(value: String) {
        context.dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = value }
    }

    // Failed Attempts
    val failedAttemptsFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.FAILED_ATTEMPTS] ?: 0
    }

    suspend fun setFailedAttempts(value: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.FAILED_ATTEMPTS] = value }
    }

    // Lockout Timestamp
    val lockoutTimestampFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOCKOUT_TIMESTAMP] ?: 0L
    }

    suspend fun setLockoutTimestamp(value: Long) {
        context.dataStore.edit { prefs -> prefs[Keys.LOCKOUT_TIMESTAMP] = value }
    }

    // Last Fee Applied Month
    val lastFeeAppliedMonthFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_FEE_APPLIED_MONTH] ?: ""
    }

    suspend fun setLastFeeAppliedMonth(value: String) {
        context.dataStore.edit { prefs -> prefs[Keys.LAST_FEE_APPLIED_MONTH] = value }
    }

    // Is Locked Permanently
    val isLockedPermanentlyFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_LOCKED_PERMANENTLY] ?: false
    }

    suspend fun setIsLockedPermanently(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.IS_LOCKED_PERMANENTLY] = value }
    }

    // Trash Retention Days
    val trashRetentionDaysFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.TRASH_RETENTION_DAYS] ?: 30
    }

    suspend fun setTrashRetentionDays(value: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.TRASH_RETENTION_DAYS] = value }
    }

    // Demo Mode
    val demoModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEMO_MODE] ?: false
    }

    suspend fun setDemoMode(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.DEMO_MODE] = value }
    }

    // Clear All
    suspend fun clearAll() {
        context.dataStore.edit { prefs -> prefs.clear() }
    }
}
