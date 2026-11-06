package dev.zlt.companion.ui

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
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.zlt.companion.data.DeviceInfo

@Composable
fun DeviceInfoScreen(
    deviceInfo: DeviceInfo?,
    host: String,
    pollSeconds: Int,
    onPollSecondsChange: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onReboot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRebootConfirm by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hardware Specs Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Device & SIM Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    InfoRow(label = "Model", value = "ZTE U30S+ (Vemo 4G)")
                    deviceInfo?.let { info ->
                        if (info.provider.isNotBlank()) {
                            InfoRow(label = "Operator", value = info.provider)
                        }
                        if (info.apn.isNotBlank()) {
                            InfoRow(label = "APN Profile", value = info.apn)
                        }
                        if (info.wanIp.isNotBlank()) {
                            InfoRow(label = "WAN IP", value = info.wanIp)
                        }
                        if (info.imei.isNotBlank()) {
                            CopyableInfoRow(
                                label = "IMEI",
                                value = info.imei,
                                onCopy = { clipboardManager.setText(AnnotatedString(info.imei)) },
                            )
                        }
                        if (info.simImsi.isNotBlank() || info.imsi.isNotBlank()) {
                            val imsi = info.simImsi.ifBlank { info.imsi }
                            CopyableInfoRow(
                                label = "SIM IMSI",
                                value = imsi,
                                onCopy = { clipboardManager.setText(AnnotatedString(imsi)) },
                            )
                        }
                        if (info.firmwareVersion.isNotBlank()) {
                            InfoRow(label = "Firmware", value = info.firmwareVersion)
                        }
                        if (info.hardwareVersion.isNotBlank()) {
                            InfoRow(label = "Hardware Ver", value = info.hardwareVersion)
                        }
                        if (info.batteryPercent.isNotBlank()) {
                            InfoRow(
                                label = "Power Status",
                                value = "${info.batteryPercent}% (USB Powered)",
                            )
                        }
                    }
                }
            }

            // Connection & Refresh Settings Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Connection & Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    InfoRow(label = "Router Host", value = host)

                    Text(
                        text = "Polling Interval",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(1, 2, 5).forEach { seconds ->
                            val selected = seconds == pollSeconds
                            if (selected) {
                                Button(
                                    onClick = { onPollSecondsChange(seconds) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("${seconds}s")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onPollSecondsChange(seconds) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("${seconds}s")
                                }
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Edit Router Password & IP", modifier = Modifier.padding(start = 4.dp))
                    }

                    OutlinedButton(
                        onClick = { showRebootConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Reboot Hotspot", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            // Privacy Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "This app communicates exclusively via LAN HTTP with your ZTE modem ($host). No analytics, no cloud backends, zero external network requests.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
    }

    if (showRebootConfirm) {
        AlertDialog(
            onDismissRequest = { showRebootConfirm = false },
            title = { Text("Reboot Hotspot?") },
            text = { Text("The hotspot will restart and temporarily disconnect all devices.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRebootConfirm = false
                        onReboot()
                    },
                ) {
                    Text("Reboot", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRebootConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun CopyableInfoRow(label: String, value: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy $label",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
