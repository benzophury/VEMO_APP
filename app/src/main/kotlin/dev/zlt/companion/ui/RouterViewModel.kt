package dev.zlt.companion.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.zlt.companion.data.ConnectedStation
import dev.zlt.companion.data.ConnectionState
import dev.zlt.companion.data.DataQuotaConfig
import dev.zlt.companion.data.DeviceInfo
import dev.zlt.companion.data.LanSettings
import dev.zlt.companion.data.NetModeState
import dev.zlt.companion.data.NetworkSettings
import dev.zlt.companion.data.RouterRepository
import dev.zlt.companion.data.RouterSettings
import dev.zlt.companion.data.RouterStatus
import dev.zlt.companion.data.SettingsStore
import dev.zlt.companion.data.SmsItem
import dev.zlt.companion.data.SpeedSample
import dev.zlt.companion.data.StatusCache
import dev.zlt.companion.data.UssdResult
import dev.zlt.companion.data.WifiConfig
import dev.zlt.companion.net.NetworkMode
import dev.zlt.companion.net.ZltException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    DASHBOARD("Dashboard"),
    WIFI_DEVICES("Wi-Fi"),
    SMS("SMS"),
    USSD("USSD"),
    TOOLS("Tools"),
}

data class StatusUiState(
    val status: RouterStatus? = null,
    val mode: NetModeState? = null,
    val selectedTab: AppTab = AppTab.DASHBOARD,
    val stations: List<ConnectedStation> = emptyList(),
    val wifiConfig: WifiConfig? = null,
    val smsList: List<SmsItem> = emptyList(),
    val deviceInfo: DeviceInfo? = null,
    val ussdResult: UssdResult? = null,
    val networkSettings: NetworkSettings? = null,
    val lanSettings: LanSettings? = null,
    val dataQuotaConfig: DataQuotaConfig? = null,
    val speedSamples: List<SpeedSample> = emptyList(),
    val currentRxRate: Long = 0L,
    val currentTxRate: Long = 0L,
    val isLoadingUssd: Boolean = false,
    val isLoadingSms: Boolean = false,
    val isSendingSms: Boolean = false,
    val isLoadingStations: Boolean = false,
    val togglingData: Boolean = false,
    val togglingWifi: Boolean = false,
    val actionMessage: String? = null,
    val error: String? = null,
    val lastUpdatedEpochMs: Long? = null,
    val everLoaded: Boolean = false,
    val refreshing: Boolean = false,
    val changingMode: NetworkMode? = null,
) {
    companion object {
        const val FAIL_THRESHOLD = 2
    }
}

class RouterViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = SettingsStore(application)
    private val repository = RouterRepository.from(settingsStore)
    private val statusCache = StatusCache(application)

    val settings: StateFlow<RouterSettings?> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _state = MutableStateFlow(StatusUiState())
    val state: StateFlow<StatusUiState> = _state.asStateFlow()

    private var consecutiveFailures = 0
    private var inFlight: Job? = null

    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastTimestamp = 0L

    fun selectTab(tab: AppTab) {
        _state.update { it.copy(selectedTab = tab) }
        when (tab) {
            AppTab.DASHBOARD -> refresh()
            AppTab.WIFI_DEVICES -> {
                refreshWifiConfig()
                refreshStations()
            }
            AppTab.SMS -> refreshSms()
            AppTab.USSD -> {}
            AppTab.TOOLS -> {
                refreshTools()
                refreshDeviceInfo()
            }
        }
    }

    /**
     * Poll router status & compute live data rate
     */
    fun refresh() {
        if (inFlight?.isActive == true) return
        inFlight = viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            try {
                val demoMode = settingsStore.current().demoMode
                val snapshot = repository.snapshot()
                consecutiveFailures = 0

                val now = System.currentTimeMillis()
                val dt = if (lastTimestamp > 0) (now - lastTimestamp) / 1000f else 0f
                val currentRx = snapshot.status.realtimeRxBytes
                val currentTx = snapshot.status.realtimeTxBytes

                var rxRate = 0L
                var txRate = 0L
                if (dt > 0.5f && lastRxBytes > 0 && currentRx >= lastRxBytes) {
                    rxRate = ((currentRx - lastRxBytes) / dt).toLong().coerceAtLeast(0L)
                }
                if (dt > 0.5f && lastTxBytes > 0 && currentTx >= lastTxBytes) {
                    txRate = ((currentTx - lastTxBytes) / dt).toLong().coerceAtLeast(0L)
                }

                lastRxBytes = currentRx
                lastTxBytes = currentTx
                lastTimestamp = now

                val newSample = SpeedSample(now, rxRate, txRate)
                val updatedSamples = (_state.value.speedSamples + newSample).takeLast(24)

                if (!demoMode) {
                    statusCache.store(snapshot.status.reading)
                }

                _state.update {
                    it.copy(
                        status = snapshot.status,
                        mode = snapshot.mode ?: it.mode,
                        speedSamples = updatedSamples,
                        currentRxRate = rxRate,
                        currentTxRate = txRate,
                        error = null,
                        lastUpdatedEpochMs = now,
                        everLoaded = true,
                        refreshing = false,
                    )
                }

                // Refresh active tab secondary data
                when (_state.value.selectedTab) {
                    AppTab.WIFI_DEVICES -> {
                        refreshWifiConfig()
                        refreshStations()
                    }
                    AppTab.SMS -> refreshSms()
                    AppTab.TOOLS -> {
                        refreshTools()
                        refreshDeviceInfo()
                    }
                    else -> {}
                }
            } catch (e: ZltException) {
                consecutiveFailures++
                _state.update {
                    it.copy(
                        refreshing = false,
                        error = if (consecutiveFailures >= StatusUiState.FAIL_THRESHOLD) {
                            e.message
                        } else {
                            it.error
                        },
                    )
                }
            }
        }
    }

    fun toggleMobileData() {
        if (_state.value.togglingData) return
        val currentConnected = _state.value.status?.connection == ConnectionState.Connected
        _state.update { it.copy(togglingData = true, error = null) }
        viewModelScope.launch {
            try {
                repository.setMobileDataConnected(!currentConnected)
                _state.update {
                    it.copy(
                        togglingData = false,
                        actionMessage = if (!currentConnected) "Cellular data connected" else "Cellular data disconnected",
                    )
                }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(togglingData = false, error = "Failed to toggle data: ${e.message}") }
            }
        }
    }

    fun refreshStations() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingStations = true) }
            try {
                val stations = repository.getStations()
                _state.update { it.copy(stations = stations, isLoadingStations = false) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoadingStations = false) }
            }
        }
    }

    fun refreshWifiConfig() {
        viewModelScope.launch {
            try {
                val wifi = repository.getWifiConfig()
                _state.update { it.copy(wifiConfig = wifi) }
            } catch (_: Exception) {}
        }
    }

    fun toggleWifi() {
        if (_state.value.togglingWifi) return
        val currentEnabled = _state.value.wifiConfig?.isEnabled ?: true
        _state.update { it.copy(togglingWifi = true, error = null) }
        viewModelScope.launch {
            try {
                repository.setWifiEnabled(!currentEnabled)
                val updated = repository.getWifiConfig()
                _state.update {
                    it.copy(
                        wifiConfig = updated,
                        togglingWifi = false,
                        actionMessage = if (!currentEnabled) "Wi-Fi turned ON" else "Wi-Fi turned OFF",
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(togglingWifi = false, error = "Failed to toggle Wi-Fi: ${e.message}") }
            }
        }
    }

    fun saveWifiSettings(ssid: String, password: String, authMode: String = "WPA2PSK") {
        viewModelScope.launch {
            try {
                repository.setWifiSsidPassword(ssid, password, authMode)
                val updated = repository.getWifiConfig()
                _state.update {
                    it.copy(
                        wifiConfig = updated,
                        actionMessage = "Wi-Fi credentials saved",
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to save Wi-Fi: ${e.message}") }
            }
        }
    }

    fun setWifiSleepTime(minutes: String) {
        viewModelScope.launch {
            try {
                repository.setWifiSleepTime(minutes)
                _state.update { it.copy(actionMessage = "Wi-Fi sleep timeout updated") }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Sleep mode update failed: ${e.message}") }
            }
        }
    }

    fun refreshSms() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingSms = true) }
            try {
                val messages = repository.getSmsMessages()
                _state.update { it.copy(smsList = messages, isLoadingSms = false) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoadingSms = false) }
            }
        }
    }

    fun sendSms(number: String, message: String, onComplete: (Boolean) -> Unit = {}) {
        if (_state.value.isSendingSms) return
        _state.update { it.copy(isSendingSms = true, error = null) }
        viewModelScope.launch {
            try {
                val ok = repository.sendSms(number, message)
                if (ok) {
                    refreshSms()
                    _state.update {
                        it.copy(
                            isSendingSms = false,
                            actionMessage = "SMS sent to $number",
                        )
                    }
                } else {
                    _state.update { it.copy(isSendingSms = false, error = "Failed to send SMS.") }
                }
                onComplete(ok)
            } catch (e: Exception) {
                _state.update { it.copy(isSendingSms = false, error = "Send failed: ${e.message}") }
                onComplete(false)
            }
        }
    }

    fun deleteSms(id: String) {
        viewModelScope.launch {
            try {
                val ok = repository.deleteSms(id)
                if (ok) {
                    refreshSms()
                    _state.update { it.copy(actionMessage = "SMS #$id deleted") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Delete failed: ${e.message}") }
            }
        }
    }

    fun sendUssd(code: String, isReply: Boolean) {
        _state.update { it.copy(isLoadingUssd = true, error = null) }
        viewModelScope.launch {
            try {
                val res = repository.sendUssd(code, isReply)
                _state.update { it.copy(ussdResult = res, isLoadingUssd = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingUssd = false,
                        ussdResult = UssdResult(success = false, content = "USSD failed: ${e.message}"),
                    )
                }
            }
        }
    }

    fun clearUssd() {
        _state.update { it.copy(ussdResult = null) }
    }

    fun refreshTools() {
        viewModelScope.launch {
            try {
                val net = repository.getNetworkSettings()
                val lan = repository.getLanSettings()
                _state.update { it.copy(networkSettings = net, lanSettings = lan) }
            } catch (_: Exception) {}
        }
    }

    fun saveNetworkSettings(dialMode: String, isRoamingAllowed: Boolean) {
        viewModelScope.launch {
            try {
                repository.setNetworkSettings(dialMode, isRoamingAllowed)
                val net = repository.getNetworkSettings()
                _state.update { it.copy(networkSettings = net, actionMessage = "Network settings applied") }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Network settings failed: ${e.message}") }
            }
        }
    }

    fun saveLanSettings(settings: LanSettings) {
        viewModelScope.launch {
            try {
                repository.setLanSettings(settings)
                val lan = repository.getLanSettings()
                _state.update { it.copy(lanSettings = lan, actionMessage = "LAN & DHCP settings saved") }
            } catch (e: Exception) {
                _state.update { it.copy(error = "LAN settings failed: ${e.message}") }
            }
        }
    }

    fun saveApn(name: String, apn: String, auth: String) {
        viewModelScope.launch {
            try {
                repository.saveApnProfile(name, apn, auth)
                refreshDeviceInfo()
                _state.update { it.copy(actionMessage = "APN profile saved as default") }
            } catch (e: Exception) {
                _state.update { it.copy(error = "APN save failed: ${e.message}") }
            }
        }
    }

    fun changeAdminPassword(oldPass: String, newPass: String) {
        viewModelScope.launch {
            try {
                val ok = repository.changeAdminPassword(oldPass, newPass)
                if (ok) {
                    val currentHost = settingsStore.current().host
                    val currentUsername = settingsStore.current().username
                    settingsStore.save(currentHost, currentUsername, newPass)
                    _state.update { it.copy(actionMessage = "Admin password updated successfully") }
                } else {
                    _state.update { it.copy(error = "Router rejected password change.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Password change failed: ${e.message}") }
            }
        }
    }

    fun factoryReset() {
        viewModelScope.launch {
            try {
                repository.factoryReset()
                _state.update { it.copy(actionMessage = "Factory reset initiated. Modem resetting...") }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Factory reset failed: ${e.message}") }
            }
        }
    }

    fun refreshDeviceInfo() {
        viewModelScope.launch {
            try {
                val info = repository.getDeviceInfo()
                _state.update { it.copy(deviceInfo = info) }
            } catch (_: Exception) {}
        }
    }

    fun clearActionMessage() {
        _state.update { it.copy(actionMessage = null) }
    }

    fun setMode(mode: NetworkMode) {
        if (_state.value.changingMode != null) return
        _state.update { it.copy(changingMode = mode, error = null) }
        viewModelScope.launch {
            try {
                val applied = repository.setMode(mode)
                _state.update { it.copy(mode = applied, changingMode = null) }
            } catch (e: ZltException) {
                _state.update { it.copy(changingMode = null, error = e.message) }
            }
        }
    }

    fun rebootRouter() {
        viewModelScope.launch {
            try {
                repository.reboot()
                _state.update { it.copy(actionMessage = "Reboot command sent to modem...") }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Reboot failed: ${e.message}") }
            }
        }
    }

    fun saveSettings(host: String, username: String, password: String) {
        viewModelScope.launch {
            settingsStore.save(host, username, password)
            consecutiveFailures = 0
            _state.value = StatusUiState()
            refresh()
        }
    }

    fun setPollSeconds(seconds: Int) {
        viewModelScope.launch { settingsStore.setPollSeconds(seconds) }
    }

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setDemoMode(enabled)
            consecutiveFailures = 0
            _state.value = StatusUiState()
            refresh()
        }
    }
}
