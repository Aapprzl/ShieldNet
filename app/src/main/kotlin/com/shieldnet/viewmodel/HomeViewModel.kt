package com.shieldnet.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shieldnet.ShieldNetApp
import com.shieldnet.blocklist.BlocklistManager
import com.shieldnet.data.repository.LogRepository
import com.shieldnet.vpn.ShieldVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LogRepository
    private val blocklistManager: BlocklistManager

    private val _vpnState = MutableStateFlow(VpnState.DISCONNECTED)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _blocklistSize = MutableStateFlow(0)
    val blocklistSize: StateFlow<Int> = _blocklistSize.asStateFlow()

    init {
        val app = application as ShieldNetApp
        repository = app.logRepository
        blocklistManager = BlocklistManager(app)

        // Monitor VPN running state periodically
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val isRunning = ShieldVpnService.isVpnRunning
                _vpnState.value = if (isRunning) VpnState.CONNECTED else VpnState.DISCONNECTED
                delay(1000)
            }
        }

        // Load blocklist size
        viewModelScope.launch(Dispatchers.IO) {
            blocklistManager.loadBlocklist()
            _blocklistSize.value = blocklistManager.domainCount
        }
    }

    val blockedToday: StateFlow<Int> = flow {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midnight = calendar.timeInMillis
        emitAll(repository.getCountSince(midnight))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalBlocked: StateFlow<Int> = repository.getTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun startVpn(context: Context) {
        _vpnState.value = VpnState.CONNECTING
        ShieldVpnService.start(context)
    }

    fun stopVpn(context: Context) {
        ShieldVpnService.stop(context)
    }
    
    fun refreshBlocklistSize() {
        viewModelScope.launch(Dispatchers.IO) {
            blocklistManager.loadBlocklist()
            _blocklistSize.value = blocklistManager.domainCount
        }
    }
}
