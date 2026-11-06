package dev.zlt.companion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import dev.zlt.companion.R
import dev.zlt.companion.data.ConnectionState
import dev.zlt.companion.data.NetModeState
import dev.zlt.companion.data.RouterStatus
import dev.zlt.companion.data.SignalLevel
import dev.zlt.companion.data.SignalReading
import dev.zlt.companion.ui.theme.DarkSurface
import dev.zlt.companion.ui.theme.DarkSurfaceBorder
import dev.zlt.companion.ui.theme.NeonCyan
import dev.zlt.companion.ui.theme.NeonGreen
import dev.zlt.companion.ui.theme.NeonRed
import dev.zlt.companion.ui.theme.NeonYellow
import dev.zlt.companion.ui.theme.QualityColors
import dev.zlt.companion.ui.theme.TextMuted
import dev.zlt.companion.ui.theme.TextPrimary
import dev.zlt.companion.ui.theme.TextSecondary
import dev.zlt.companion.ui.widgets.DataQuotaGauge
import dev.zlt.companion.ui.widgets.LiveSpeedChart
import java.text.DateFormat
import java.util.Date

@Composable
fun StatusScreen(
    state: StatusUiState,
    host: String,
    demoMode: Boolean,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onExitDemo: () -> Unit,
    onReboot: () -> Unit = {},
    onToggleData: () -> Unit = {},
    onSaveQuota: (Boolean, String, String, Int) -> Unit = { _, _, _, _ -> },
    onResetTraffic: () -> Unit = {},
    modeControl: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(
                host = host,
                operator = state.status?.networkProvider.orEmpty().ifEmpty { "GrameenPhone" },
                onOpenSettings = onOpenSettings,
                onReboot = onReboot,
                onRefresh = onRefresh,
                isRefreshing = state.refreshing,
            )

            // Demo mode banner
            AnimatedVisibility(
                visible = demoMode,
                enter = fadeIn(tween(300)) + expandIn(tween(300)),
                exit = fadeOut(tween(200)) + shrinkOut(tween(200)),
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF221100)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonYellow.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 14.dp, end = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.demo_banner),
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonYellow,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 10.dp),
                        )
                        TextButton(onClick = onExitDemo) {
                            Text(stringResource(R.string.demo_exit), color = NeonYellow)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn(tween(300)) + expandIn(tween(300)),
                exit = fadeOut(tween(200)) + shrinkOut(tween(200)),
            ) {
                state.error?.let { message ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF330005)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonRed,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }

            // 1. Live Throughput Waveform Widget
            LiveSpeedChart(
                samples = state.speedSamples,
                currentRxRate = state.currentRxRate,
                currentTxRate = state.currentTxRate,
            )

            // 2. Data Quota Donut Gauge Widget
            val monthlyBytes = state.status?.monthlyTotalBytes ?: 0L
            DataQuotaGauge(
                monthlyBytes = monthlyBytes,
                quotaConfig = state.dataQuotaConfig,
                onSaveQuota = onSaveQuota,
                onResetTraffic = onResetTraffic,
            )

            // 3. Cellular Data Connection Switch Card
            state.status?.let { status ->
                CellularSwitchCard(
                    connection = status.connection,
                    wanIp = status.wanIp,
                    togglingData = state.togglingData,
                    onToggleData = onToggleData,
                )

                // 4. Signal Meter Card
                SignalCard(
                    reading = status.reading,
                    operator = status.networkProvider,
                    networkType = status.networkType,
                    everLoaded = state.everLoaded,
                )

                // 5. Network Mode Card
                ModeCard(mode = state.mode, control = modeControl)

                // 6. Diagnostics & RF Details
                DetailsSection(status)
            }

            // Footer updated time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.lastUpdatedEpochMs?.let {
                        "Updated ${formatTime(it)}"
                    } ?: "Connecting...",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
                Text(
                    text = "LAN: $host",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                )
            }
        }
    }
}

@Composable
private fun Header(
    host: String,
    operator: String,
    onOpenSettings: () -> Unit,
    onReboot: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
) {
    var showRebootConfirm by remember { mutableStateOf(false) }

    if (showRebootConfirm) {
        AlertDialog(
            onDismissRequest = { showRebootConfirm = false },
            title = { Text("Reboot Hotspot?") },
            text = { Text("The hotspot will restart and temporarily disconnect all devices.") },
            confirmButton = {
                TextButton(onClick = {
                    showRebootConfirm = false
                    onReboot()
                }) {
                    Text("Reboot", color = NeonRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "VEMO APP",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = TextPrimary,
            )
            Text(
                text = "$operator • $host",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
                }
            }
            IconButton(onClick = { showRebootConfirm = true }) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Reboot", tint = TextSecondary)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
            }
        }
    }
}

@Composable
private fun CellularSwitchCard(
    connection: ConnectionState,
    wanIp: String,
    togglingData: Boolean,
    onToggleData: () -> Unit,
) {
    val isConnected = connection == ConnectionState.Connected
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    Icons.Default.Public,
                    contentDescription = null,
                    tint = if (isConnected) NeonGreen else NeonRed,
                    modifier = Modifier.size(28.dp),
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Cellular Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = if (isConnected) NeonGreen.copy(alpha = 0.2f) else NeonRed.copy(alpha = 0.2f),
                        ) {
                            Text(
                                text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) NeonGreen else NeonRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (wanIp.isNotBlank() && isConnected) {
                        Text(
                            text = "WAN IP: $wanIp",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                        )
                    }
                }
            }

            if (togglingData) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = NeonCyan)
            } else {
                Switch(
                    checked = isConnected,
                    onCheckedChange = { onToggleData() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = NeonGreen,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF222222),
                    ),
                )
            }
        }
    }
}

@Composable
private fun SignalCard(
    reading: SignalReading?,
    operator: String,
    networkType: String,
    everLoaded: Boolean,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SIGNAL QUALITY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = TextSecondary,
                )
                if (networkType.isNotBlank()) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = NeonCyan.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = networkType.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            if (reading == null) {
                Text(
                    text = if (everLoaded) "No signal" else "Checking…",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                )
                return@Column
            }

            val tint = when (reading.level) {
                SignalLevel.EXCELLENT, SignalLevel.GOOD -> NeonGreen
                SignalLevel.FAIR -> NeonYellow
                SignalLevel.WEAK -> NeonRed
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SignalBars(
                    bars = reading.bars,
                    tint = tint,
                    contentDescription = "Signal ${reading.bars} of 5 bars",
                )
                Column {
                    Text(
                        text = reading.level.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = tint,
                    )
                    Text(
                        text = "${reading.metric.label} ${reading.dbm} dBm",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { reading.fraction },
                color = tint,
                trackColor = Color(0xFF1E1E1E),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ModeCard(mode: NetModeState?, control: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "NETWORK SELECTION MODE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = TextSecondary,
            )
            control()
        }
    }
}

@Composable
private fun DetailsSection(status: RouterStatus) {
    var expanded by remember { mutableStateOf(false) }

    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (expanded) "▲ HIDE RF DIAGNOSTICS" else "▼ VIEW RF DIAGNOSTICS & CELL DETAILS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            letterSpacing = 1.sp,
        )
    }

    if (!expanded) return

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DetailRow("Operator", status.networkProvider)
            DetailRow("WAN IP", status.wanIp)
            DetailRow("RSRP", status.rsrpDbm?.let { "$it dBm" })
            DetailRow("RSSI", status.rssiDbm?.let { "$it dBm" })
            DetailRow("LTE Band", status.band)
            DetailRow("SNR", status.snr)
            DetailRow("RSRQ", status.rsrq)
            DetailRow("PCI (Physical Cell ID)", status.pci)
            DetailRow("Modem Hardware Bars", "${status.deviceBars} / 5")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.End,
        )
    }
}

private fun formatTime(epochMs: Long): String =
    DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(epochMs))
