package com.shieldnet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shieldnet.ShieldNetApp
import com.shieldnet.data.db.BlockedLogEntity
import com.shieldnet.data.db.DomainCount
import com.shieldnet.data.repository.LogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LogViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LogRepository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        val app = application as ShieldNetApp
        repository = app.logRepository
    }

    val recentLogs: StateFlow<List<BlockedLogEntity>> = combine(
        repository.getRecentLogs(),
        _searchQuery
    ) { logs, query ->
        if (query.isBlank()) {
            logs
        } else {
            logs.filter { it.domain.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topBlockedDomains: StateFlow<List<DomainCount>> = repository.getTopBlockedDomains()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAll()
        }
    }
    
    fun deleteOlderThan(days: Int) {
        if (days <= 0) return
        val beforeTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteOlderThan(beforeTime)
        }
    }
}
