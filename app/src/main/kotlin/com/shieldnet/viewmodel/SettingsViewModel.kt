package com.shieldnet.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shieldnet.blocklist.BlocklistUpdater
import com.shieldnet.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs: SharedPreferences = application.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    private val blocklistUpdater = BlocklistUpdater(application)

    private val _autoStart = MutableStateFlow(sharedPrefs.getBoolean(Constants.PREF_AUTO_START, false))
    val autoStart: StateFlow<Boolean> = _autoStart.asStateFlow()

    private val _dnsServer = MutableStateFlow(sharedPrefs.getString(Constants.PREF_DNS_SERVER, Constants.DEFAULT_DNS_SERVER) ?: Constants.DEFAULT_DNS_SERVER)
    val dnsServer: StateFlow<String> = _dnsServer.asStateFlow()

    private val _saveLogs = MutableStateFlow(sharedPrefs.getBoolean(Constants.PREF_SAVE_LOG, true))
    val saveLogs: StateFlow<Boolean> = _saveLogs.asStateFlow()

    private val _logRetentionDays = MutableStateFlow(sharedPrefs.getInt(Constants.PREF_LOG_RETENTION_DAYS, 7))
    val logRetentionDays: StateFlow<Int> = _logRetentionDays.asStateFlow()

    private val _blocklistLastUpdated = MutableStateFlow(getLastUpdatedText())
    val blocklistLastUpdated: StateFlow<String> = _blocklistLastUpdated.asStateFlow()

    private val _isUpdatingBlocklist = MutableStateFlow(false)
    val isUpdatingBlocklist: StateFlow<Boolean> = _isUpdatingBlocklist.asStateFlow()

    private val _updateResult = MutableStateFlow<String?>(null)
    val updateResult: StateFlow<String?> = _updateResult.asStateFlow()

    fun setAutoStart(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(Constants.PREF_AUTO_START, enabled).apply()
        _autoStart.value = enabled
    }

    fun setDnsServer(dns: String) {
        sharedPrefs.edit().putString(Constants.PREF_DNS_SERVER, dns).apply()
        _dnsServer.value = dns
    }

    fun setSaveLogs(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(Constants.PREF_SAVE_LOG, enabled).apply()
        _saveLogs.value = enabled
    }

    fun setLogRetentionDays(days: Int) {
        sharedPrefs.edit().putInt(Constants.PREF_LOG_RETENTION_DAYS, days).apply()
        _logRetentionDays.value = days
    }

    fun updateBlocklistNow(onComplete: (Boolean, Int) -> Unit = { _, _ -> }) {
        if (_isUpdatingBlocklist.value) return
        _isUpdatingBlocklist.value = true
        _updateResult.value = null
        
        viewModelScope.launch(Dispatchers.IO) {
            val result = blocklistUpdater.updateBlocklist()
            _isUpdatingBlocklist.value = false
            
            result.fold(
                onSuccess = { count ->
                    _blocklistLastUpdated.value = getLastUpdatedText()
                    _updateResult.value = "Successfully updated! Loaded $count domains."
                    viewModelScope.launch(Dispatchers.Main) {
                        onComplete(true, count)
                    }
                },
                onFailure = { error ->
                    _updateResult.value = "Failed to update: ${error.localizedMessage}"
                    viewModelScope.launch(Dispatchers.Main) {
                        onComplete(false, 0)
                    }
                }
            )
        }
    }

    fun clearUpdateResult() {
        _updateResult.value = null
    }

    private fun getLastUpdatedText(): String {
        val lastUpdate = blocklistUpdater.getLastUpdateTime()
        return if (lastUpdate > 0) {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            sdf.format(Date(lastUpdate))
        } else {
            "Never"
        }
    }
}
