package com.example.data.preference

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gscan_prefs", Context.MODE_PRIVATE)

    private val _isPinLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_PIN_ENABLED, false))
    val isPinLockEnabled: StateFlow<Boolean> = _isPinLockEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _defaultPageFormat = MutableStateFlow(prefs.getString(KEY_PAGE_FORMAT, "A4") ?: "A4")
    val defaultPageFormat: StateFlow<String> = _defaultPageFormat.asStateFlow()

    private val _defaultQuality = MutableStateFlow(prefs.getString(KEY_QUALITY, "HIGH") ?: "HIGH")
    val defaultQuality: StateFlow<String> = _defaultQuality.asStateFlow()

    private val _defaultFilter = MutableStateFlow(prefs.getString(KEY_FILTER, "MAGIC_COLOR") ?: "MAGIC_COLOR")
    val defaultFilter: StateFlow<String> = _defaultFilter.asStateFlow()

    fun getPinCode(): String {
        return prefs.getString(KEY_PIN_CODE, "") ?: ""
    }

    fun setPinCode(pin: String) {
        prefs.edit().putString(KEY_PIN_CODE, pin).putBoolean(KEY_PIN_ENABLED, pin.isNotEmpty()).apply()
        _isPinLockEnabled.value = pin.isNotEmpty()
    }

    fun setPinEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PIN_ENABLED, enabled).apply()
        _isPinLockEnabled.value = enabled
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun setDefaultPageFormat(format: String) {
        prefs.edit().putString(KEY_PAGE_FORMAT, format).apply()
        _defaultPageFormat.value = format
    }

    fun setDefaultQuality(quality: String) {
        prefs.edit().putString(KEY_QUALITY, quality).apply()
        _defaultQuality.value = quality
    }

    fun setDefaultFilter(filter: String) {
        prefs.edit().putString(KEY_FILTER, filter).apply()
        _defaultFilter.value = filter
    }

    companion object {
        private const val KEY_PIN_ENABLED = "pin_enabled"
        private const val KEY_PIN_CODE = "pin_code"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PAGE_FORMAT = "default_page_format"
        private const val KEY_QUALITY = "default_quality"
        private const val KEY_FILTER = "default_filter"
    }
}
