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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zlt.companion.data.ConnectedStation
import dev.zlt.companion.data.WifiConfig
import dev.zlt.companion.ui.theme.DarkSurface
import dev.zlt.companion.ui.theme.DarkSurfaceBorder
import dev.zlt.companion.ui.theme.DarkSurfaceElevated
import dev.zlt.companion.ui.theme.NeonCyan
import dev.zlt.companion.ui.theme.NeonGreen
import dev.zlt.companion.ui.theme.NeonMagenta
import dev.zlt.companion.ui.theme.TextMuted
import dev.zlt.companion.ui.theme.TextPrimary
import dev.zlt.companion.ui.theme.TextSecondary

@Composable
fun WifiDevicesScreen(
    wifiConfig: WifiConfig?,
    stations: List<ConnectedStation>,
    isLoadingStations: Boolean,
    togglingWifi: Boolean,
    onToggleWifi: () -> Unit,
    onSaveWifiSettings: (String, String, String) -> Unit,
    onSwitchBand: (String) -> Unit = {},
    onSetSleepTime: (String) -> Unit = {},
    onRefreshStations: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedSleepMinutes by remember { mutableStateOf("-1") }

    // Band selection: defaults to whatever band is active on the router (e.g. 5GHz)
    val activeBand = wifiConfig?.activeBand ?: "5GHz"
    var viewingBand by remember(wifiConfig?.activeBand) { mutableStateOf(activeBand) }

    val clipboardManager = LocalClipboardManager.current
    val isWifiOn = wifiConfig?.isEnabled == true

    // Current credentials for the viewed band
    val is5g = viewingBand == "5GHz"
    val currentSsid = if (is5g) {
        wifiConfig?.ssid5g?.ifEmpty { wifiConfig.ssid } ?: ""
    } else {
        wifiConfig?.ssid24?.ifEmpty { wifiConfig.ssid } ?: ""
    }
    val currentPassword = if (is5g) {
        wifiConfig?.password5g?.ifEmpty { wifiConfig.password } ?: ""
    } else {
        wifiConfig?.password24?.ifEmpty { wifiConfig.password } ?: ""
    }
    val currentAuth = if (is5g) {
        wifiConfig?.authMode5g?.ifEmpty { "WPA3Personal" } ?: "WPA3Personal"
    } else {
        wifiConfig?.authMode24?.ifEmpty { "WPA2PSK" } ?: "WPA2PSK"
    }
    val currentMaxStations = if (is5g) {
        wifiConfig?.maxStations5g ?: "10"
    } else {
        wifiConfig?.maxStations24 ?: "10"
    }

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
            // Master Wi-Fi Switch Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isWifiOn) NeonGreen.copy(alpha = 0.4f) else DarkSurfaceBorder,
                ),
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
                    ) {
                        Icon(
                            imageVector = if (isWifiOn) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isWifiOn) NeonGreen else TextMuted,
                            modifier = Modifier.size(32.dp),
                        )
                        Column {
                            Text(
                                text = "Wi-Fi Hotspot Radio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                            Text(
                                text = if (isWifiOn) "Broadcasting on $activeBand (${stations.size} clients)" else "Radio Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isWifiOn) NeonGreen else TextMuted,
                            )
                        }
                    }

                    if (togglingWifi) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = NeonCyan)
                    } else {
                        Switch(
                            checked = isWifiOn,
                            onCheckedChange = { onToggleWifi() },
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

            // Wi-Fi Credentials & Dual-Band Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "HOTSPOT ACCESS POINT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = TextSecondary,
                        )
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Wi-Fi", tint = NeonCyan)
                        }
                    }

                    // Dual-Band Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = viewingBand == "5GHz",
                            onClick = { viewingBand = "5GHz" },
                            label = {
                                Text(
                                    text = if (activeBand == "5GHz") "5 GHz (Active)" else "5 GHz",
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = DarkSurfaceElevated,
                                labelColor = TextSecondary,
                            ),
                            modifier = Modifier.weight(1f),
                        )

                        FilterChip(
                            selected = viewingBand == "2.4GHz",
                            onClick = { viewingBand = "2.4GHz" },
                            label = {
                                Text(
                                    text = if (activeBand == "2.4GHz") "2.4 GHz (Active)" else "2.4 GHz",
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = DarkSurfaceElevated,
                                labelColor = TextSecondary,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Inactive band switch button
                    if (viewingBand != activeBand) {
                        OutlinedButton(
                            onClick = { onSwitchBand(viewingBand) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                        ) {
                            Text("Switch Hotspot Radio to $viewingBand", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // SSID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "SSID (Network Name)", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Text(
                            text = currentSsid.ifEmpty { "—" },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                    }

                    // Password
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Password", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (passwordVisible) currentPassword else "••••••••",
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                            )
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password",
                                    tint = TextSecondary,
                                )
                            }
                            if (currentPassword.isNotEmpty()) {
                                IconButton(onClick = { clipboardManager.setText(AnnotatedString(currentPassword)) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy password", tint = NeonCyan)
                                }
                            }
                        }
                    }

                    // Security Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Security Mode", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Text(
                            text = currentAuth,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                        )
                    }

                    // Max Clients
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Max Allowed Devices", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Text(
                            text = "$currentMaxStations clients",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                        )
                    }
                }
            }

            // Wi-Fi Sleep Mode Card
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Bedtime, contentDescription = null, tint = NeonMagenta, modifier = Modifier.size(20.dp))
                        Text(
                            text = "WI-FI SLEEP MODE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = TextSecondary,
                        )
                    }

                    Text(
                        text = "Automatically shut down Wi-Fi radio when no devices are connected to conserve battery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val options = listOf("Never" to "-1", "10 min" to "10", "20 min" to "20", "30 min" to "30")
                        for ((label, value) in options) {
                            FilterChip(
                                selected = selectedSleepMinutes == value,
                                onClick = {
                                    selectedSleepMinutes = value
                                    onSetSleepTime(value)
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonMagenta,
                                    selectedLabelColor = Color.Black,
                                    containerColor = DarkSurfaceElevated,
                                    labelColor = TextSecondary,
                                ),
                            )
                        }
                    }
                }
            }

            // Connected Clients Section
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Text(
                                text = "CONNECTED CLIENTS (${stations.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = TextSecondary,
                            )
                        }
                        IconButton(onClick = onRefreshStations, enabled = !isLoadingStations) {
                            if (isLoadingStations) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = NeonCyan)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh clients", tint = TextSecondary)
                            }
                        }
                    }

                    if (stations.isEmpty()) {
                        Text(
                            text = if (isWifiOn) "No client devices connected." else "Wi-Fi is off.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (station in stations) {
                                StationCard(station)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditWifiDialog(
            bandName = viewingBand,
            currentSsid = currentSsid,
            currentPassword = currentPassword,
            currentAuth = currentAuth,
            onDismiss = { showEditDialog = false },
            onConfirm = { newSsid, newPassword, newAuth ->
                onSaveWifiSettings(newSsid, newPassword, newAuth)
                showEditDialog = false
            },
        )
    }
}

@Composable
private fun StationCard(station: ConnectedStation) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.hostname.ifBlank { "Unknown Device" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Text(
                    text = "IP: ${station.ip}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = NeonCyan,
                )
                Text(
                    text = "MAC: ${station.mac}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                )
            }
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = Color(0xFF1B2B34),
            ) {
                Text(
                    text = station.devType.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun EditWifiDialog(
    bandName: String,
    currentSsid: String,
    currentPassword: String,
    currentAuth: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
) {
    var ssid by remember { mutableStateOf(currentSsid) }
    var password by remember { mutableStateOf(currentPassword) }
    var authMode by remember { mutableStateOf(currentAuth) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Configure $bandName Wi-Fi", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("Network Name (SSID)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextMuted,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Wi-Fi Password (WPA/WPA2/WPA3)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextMuted,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextSecondary,
                            )
                        }
                    },
                )

                // Auth mode selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Security Mode", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val modes = if (bandName == "5GHz") {
                            listOf("WPA3Personal", "WPA2PSK", "WPA2WPA3")
                        } else {
                            listOf("WPA2PSK", "WPAPSKWPA2PSK")
                        }
                        for (mode in modes) {
                            FilterChip(
                                selected = authMode == mode,
                                onClick = { authMode = mode },
                                label = { Text(mode, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = Color.Black,
                                    containerColor = DarkSurface,
                                    labelColor = TextSecondary,
                                ),
                            )
                        }
                    }
                }

                Text(
                    text = "Saving will momentarily restart the $bandName Wi-Fi radio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(ssid, password, authMode) },
                enabled = ssid.isNotBlank() && password.length >= 8,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
            ) {
                Text("Save & Apply", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}