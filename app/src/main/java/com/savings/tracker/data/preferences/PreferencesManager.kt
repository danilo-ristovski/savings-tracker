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
    // In-memory timestamp — persists across Activity recreations within the same process
    @Volatile
    var lastInteractionTime: Long = System.currentTimeMillis()


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
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val SHAKE_LOGOUT_ENABLED = booleanPreferencesKey("shake_logout_enabled")
        val TRENDS_SORT_FIELD = stringPreferencesKey("trends_sort_field")
        val TRENDS_SORT_ORDER = stringPreferencesKey("trends_sort_order")
        val AUTO_BLUR_ENABLED = booleanPreferencesKey("auto_blur_enabled")
        val ANALYSIS_HIDDEN_SECTIONS = stringPreferencesKey("analysis_hidden_sections")
        val CHART_HIDDEN_TYPES = stringPreferencesKey("chart_hidden_types")
        val TABLE_DETAIL_LEVEL = stringPreferencesKey("table_detail_level")
        val PERSIST_DETAIL_LEVEL = booleanPreferencesKey("persist_detail_level")
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

    // Biometric Enabled
    val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.BIOMETRIC_ENABLED] ?: false
    }

    suspend fun setBiometricEnabled(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.BIOMETRIC_ENABLED] = value }
    }

    // Shake Logout
    val shakeLogoutEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHAKE_LOGOUT_ENABLED] ?: false
    }

    suspend fun setShakeLogoutEnabled(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.SHAKE_LOGOUT_ENABLED] = value }
    }

    // Trends Sort
    val trendsSortFieldFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.TRENDS_SORT_FIELD] ?: "DATE"
    }

    val trendsSortOrderFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.TRENDS_SORT_ORDER] ?: "ASC"
    }

    suspend fun setTrendsSortField(value: String) {
        context.dataStore.edit { prefs -> prefs[Keys.TRENDS_SORT_FIELD] = value }
    }

    suspend fun setTrendsSortOrder(value: String) {
        context.dataStore.edit { prefs -> prefs[Keys.TRENDS_SORT_ORDER] = value }
    }

    // Auto Blur
    val autoBlurEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_BLUR_ENABLED] ?: false
    }

    suspend fun setAutoBlurEnabled(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.AUTO_BLUR_ENABLED] = value }
    }

    // Analysis Hidden Sections
    val analysisHiddenSectionsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val csv = prefs[Keys.ANALYSIS_HIDDEN_SECTIONS] ?: ""
        if (csv.isBlank()) emptySet() else csv.split(",").toSet()
    }

    suspend fun setAnalysisHiddenSections(sections: Set<String>) {
        context.dataStore.edit { it[Keys.ANALYSIS_HIDDEN_SECTIONS] = sections.joinToString(",") }
    }

    // Chart Hidden Types
    val chartHiddenTypesFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val csv = prefs[Keys.CHART_HIDDEN_TYPES] ?: ""
        if (csv.isBlank()) emptySet() else csv.split(",").toSet()
    }

    suspend fun setChartHiddenTypes(types: Set<String>) {
        context.dataStore.edit { it[Keys.CHART_HIDDEN_TYPES] = types.joinToString(",") }
    }

    // Table Detail Level
    val tableDetailLevelFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.TABLE_DETAIL_LEVEL] ?: "SIMPLE"
    }

    suspend fun setTableDetailLevel(value: String) {
        context.dataStore.edit { prefs -> prefs[Keys.TABLE_DETAIL_LEVEL] = value }
    }

    val persistDetailLevelFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PERSIST_DETAIL_LEVEL] ?: false
    }

    suspend fun setPersistDetailLevel(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.PERSIST_DETAIL_LEVEL] = value }
    }

    // Clear All
    suspend fun clearAll() {
        context.dataStore.edit { prefs -> prefs.clear() }
    }
}
