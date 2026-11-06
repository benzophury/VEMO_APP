package dev.zlt.companion.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.zlt.companion.ui.theme.AmoledBlack
import dev.zlt.companion.ui.theme.DarkSurfaceBorder
import dev.zlt.companion.ui.theme.DarkSurfaceElevated
import dev.zlt.companion.ui.theme.NeonCyan
import dev.zlt.companion.ui.theme.TextMuted
import dev.zlt.companion.ui.theme.TextPrimary
import dev.zlt.companion.ui.theme.ZltTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TARGET_TAB = "extra_target_tab"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 1001)
            }
        }

        setContent {
            ZltTheme {
                AppRoot(initialTab = intent?.getStringExtra(EXTRA_TARGET_TAB))
            }
        }
    }
}

@Composable
private fun AppRoot(
    initialTab: String? = null,
    viewModel: RouterViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var settingsOpen by remember { mutableStateOf(false) }

    LaunchedEffect(initialTab) {
        if (initialTab == "SMS") {
            viewModel.selectTab(AppTab.SMS)
        }
    }

    val current = settings
    if (current == null) {
        Surface(color = AmoledBlack) {}
        return
    }

    if (settingsOpen || !current.isConfigured) {
        SettingsScreen(
            settings = current,
            showPollInterval = current.isConfigured,
            onSave = { host, username, password ->
                viewModel.saveSettings(host, username, password)
                settingsOpen = false
            },
            onPollSecondsChange = viewModel::setPollSeconds,
            onBack = if (current.isConfigured) ({ settingsOpen = false }) else null,
            onTryDemo = if (!current.isConfigured) ({ viewModel.setDemoMode(true) }) else null,
        )
        return
    }

    // Polling is tied to the RESUMED state
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, current.pollSeconds, current.credentials()) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                viewModel.refresh()
                delay(current.pollSeconds * 1_000L)
            }
        }
    }

    // Handle feedback snackbars
    val actionMsg = state.actionMessage
    LaunchedEffect(actionMsg) {
        if (actionMsg != null) {
            snackbarHostState.showSnackbar(actionMsg)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        containerColor = AmoledBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = AmoledBlack,
                tonalElevation = androidx.compose.ui.unit.Dp(0f),
            ) {
                NavigationBarItem(
                    selected = state.selectedTab == AppTab.DASHBOARD,
                    onClick = { viewModel.selectTab(AppTab.DASHBOARD) },
                    icon = { Icon(Icons.Default.SignalCellularAlt, contentDescription = null) },
                    label = { Text("Dashboard") },
                    colors = navigationItemColors(),
                )
                NavigationBarItem(
                    selected = state.selectedTab == AppTab.WIFI_DEVICES,
                    onClick = { viewModel.selectTab(AppTab.WIFI_DEVICES) },
                    icon = { Icon(Icons.Default.Wifi, contentDescription = null) },
                    label = { Text("Wi-Fi") },
                    colors = navigationItemColors(),
                )
                NavigationBarItem(
                    selected = state.selectedTab == AppTab.SMS,
                    onClick = { viewModel.selectTab(AppTab.SMS) },
                    icon = {
                        val unreadCount = state.smsList.count { it.isUnread }
                            .coerceAtLeast(state.status?.smsUnreadCount ?: 0)
                        if (unreadCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(unreadCount.toString())
                                    }
                                },
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null)
                            }
                        } else {
                            Icon(Icons.Default.Email, contentDescription = null)
                        }
                    },
                    label = { Text("SMS") },
                    colors = navigationItemColors(),
                )
                NavigationBarItem(
                    selected = state.selectedTab == AppTab.TOOLS,
                    onClick = { viewModel.selectTab(AppTab.TOOLS) },
                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                    label = { Text("Tools") },
                    colors = navigationItemColors(),
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AnimatedContent(
                targetState = state.selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200, delayMillis = 30)) +
                        scaleIn(initialScale = 0.98f, animationSpec = tween(200, delayMillis = 30)) togetherWith
                        fadeOut(animationSpec = tween(140))
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    AppTab.DASHBOARD -> {
                        StatusScreen(
                            state = state,
                            host = current.host,
                            demoMode = current.demoMode,
                            onRefresh = { viewModel.refresh() },
                            onOpenSettings = { settingsOpen = true },
                            onExitDemo = { viewModel.setDemoMode(false) },
                            onReboot = { viewModel.rebootRouter() },
                            onToggleData = { viewModel.toggleMobileData() },
                            onSaveQuota = { enabled, limitVal, unitMult, alertPct ->
                                viewModel.saveDataQuota(enabled, limitVal, unitMult, alertPct)
                            },
                            onResetTraffic = { viewModel.resetTrafficCalibration() },
                            modeControl = {
                                ModeControl(
                                    mode = state.mode,
                                    changingMode = state.changingMode,
                                    onSelect = viewModel::setMode,
                                )
                            },
                        )
                    }
                    AppTab.WIFI_DEVICES -> {
                        WifiDevicesScreen(
                            wifiConfig = state.wifiConfig,
                            stations = state.stations,
                            isLoadingStations = state.isLoadingStations,
                            togglingWifi = state.togglingWifi,
                            onToggleWifi = { viewModel.toggleWifi() },
                            onSaveWifiSettings = { ssid, pwd, auth -> viewModel.saveWifiSettings(ssid, pwd, auth) },
                            onSwitchBand = { band -> viewModel.setWifiBand(band) },
                            onSetSleepTime = { mins -> viewModel.setWifiSleepTime(mins) },
                            onRefreshStations = { viewModel.refreshStations() },
                        )
                    }
                    AppTab.SMS -> {
                        SmsScreen(
                            smsList = state.smsList,
                            isLoading = state.isLoadingSms,
                            isSending = state.isSendingSms,
                            onRefresh = { viewModel.refreshSms() },
                            onSendSms = { num, msg -> viewModel.sendSms(num, msg) },
                            onDeleteSms = { id -> viewModel.deleteSms(id) },
                            onMarkAllRead = { viewModel.markAllSmsAsRead() },
                            onMarkAsRead = { id -> viewModel.markSmsAsRead(id) },
                        )
                    }
                    AppTab.TOOLS -> {
                        ToolsSettingsScreen(
                            networkSettings = state.networkSettings,
                            lanSettings = state.lanSettings,
                            onSaveNetworkSettings = { mode, roam -> viewModel.saveNetworkSettings(mode, roam) },
                            onSaveLanSettings = { lan -> viewModel.saveLanSettings(lan) },
                            onSaveApn = { name, apn, auth -> viewModel.saveApn(name, apn, auth) },
                            onChangePassword = { old, new -> viewModel.changeAdminPassword(old, new) },
                            onFactoryReset = { viewModel.factoryReset() },
                            onReboot = { viewModel.rebootRouter() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    unselectedIconColor = TextMuted,
    unselectedTextColor = TextMuted,
    indicatorColor = DarkSurfaceElevated,
)
