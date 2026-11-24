package dev.zlt.companion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zlt.companion.data.LanSettings
import dev.zlt.companion.data.NetworkSettings
import dev.zlt.companion.ui.theme.DarkSurface
import dev.zlt.companion.ui.theme.DarkSurfaceBorder
import dev.zlt.companion.ui.theme.DarkSurfaceElevated
import dev.zlt.companion.ui.theme.NeonCyan
import dev.zlt.companion.ui.theme.NeonGreen
import dev.zlt.companion.ui.theme.NeonMagenta
import dev.zlt.companion.ui.theme.NeonRed
import dev.zlt.companion.ui.theme.NeonYellow
import dev.zlt.companion.ui.theme.TextMuted
import dev.zlt.companion.ui.theme.TextPrimary
import dev.zlt.companion.util.AppLauncher
import dev.zlt.companion.ui.theme.TextSecondary

@Composable
fun ToolsSettingsScreen(
    networkSettings: NetworkSettings?,
    lanSettings: LanSettings?,
    carrierAppName: String = AppLauncher.DEFAULT_CARRIER_NAME,
    carrierPackageName: String = AppLauncher.DEFAULT_CARRIER_PACKAGE,
    autoMatchCarrierApp: Boolean = true,
    fallbackRouterAppName: String = AppLauncher.DEFAULT_ROUTER_APP_NAME,
    fallbackRouterPackageName: String = AppLauncher.DEFAULT_ROUTER_APP_PACKAGE,
    onSaveNetworkSettings: (String, Boolean) -> Unit,
    onSaveLanSettings: (LanSettings) -> Unit,
    onSaveApn: (String, String, String) -> Unit,
    onSaveCarrierApp: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onSaveFallbackRouterApp: (String, String) -> Unit = { _, _ -> },
    onChangePassword: (String, String) -> Unit,
    onFactoryReset: () -> Unit,
    onReboot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCarrierDialog by remember { mutableStateOf(false) }
    var showFallbackRouterDialog by remember { mutableStateOf(false) }
    var showApnDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showRebootConfirm by remember { mutableStateOf(false) }

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
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.Build, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(28.dp))
                Column {
                    Text(
                        text = "ROUTER CONTROL & TOOLS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Network routing, roaming, APN and modem security",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            }

            // Carrier Self-Care App Card
            val context = LocalContext.current
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = NeonYellow)
                            Text(
                                text = "CARRIER APP (${carrierAppName.uppercase()})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp,
                            )
                        }

                        OutlinedButton(
                            onClick = { showCarrierDialog = true },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
                            Text("Change", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Text(
                        text = "Quickly launch $carrierAppName self-care app to reload balance, buy data bundles, and check validity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )

                    Text(
                        text = if (autoMatchCarrierApp) {
                            "Package: $carrierPackageName (Auto-matched from SIM)"
                        } else {
                            "Package: $carrierPackageName"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    )

                    Button(
                        onClick = { AppLauncher.launchCarrierApp(context, carrierPackageName) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonYellow, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
                        androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                        Text("Open $carrierAppName App", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Home / Other Router App Card (fallback when U30S+ isn't reachable)
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.Lan, contentDescription = null, tint = NeonCyan)
                            Text(
                                text = "OTHER ROUTER APP",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp,
                            )
                        }

                        OutlinedButton(
                            onClick = { showFallbackRouterDialog = true },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
                            Text("Change", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Text(
                        text = "One-tap shortcut shown when the U30S+ isn't reachable (e.g. when you're on your home Wi-Fi).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )

                    Text(
                        text = "$fallbackRouterAppName • $fallbackRouterPackageName",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    )

                    OutlinedButton(
                        onClick = { AppLauncher.launchApp(context, fallbackRouterPackageName) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
                        androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                        Text("Open $fallbackRouterAppName")
                    }
                }
            }

            // Mobile Network & Roaming Card
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = NeonCyan)
                        Text(
                            text = "CELLULAR CONNECTION SETTINGS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp,
                        )
                    }

                    // Roaming switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Data Roaming",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                            )
                            Text(
                                text = "Allow connection when roaming on partner networks",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                        Switch(
                            checked = networkSettings?.isRoamingAllowed ?: true,
                            onCheckedChange = { allowed ->
                                onSaveNetworkSettings(networkSettings?.dialMode ?: "auto_dial", allowed)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonGreen,
                            ),
                        )
                    }

                    // Dial Mode switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto Connect",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                            )
                            Text(
                                text = "Automatically connect to cellular on boot",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                        Switch(
                            checked = networkSettings?.dialMode != "manual_dial",
                            onCheckedChange = { auto ->
                                onSaveNetworkSettings(if (auto) "auto_dial" else "manual_dial", networkSettings?.isRoamingAllowed ?: true)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonCyan,
                            ),
                        )
                    }

                    FilledTonalButton(
                        onClick = { showApnDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = DarkSurfaceElevated,
                            contentColor = NeonCyan,
                        ),
                    ) {
                        Text("Configure APN Profile")
                    }
                }
            }

            // LAN / DHCP Settings Card
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
                        Icon(Icons.Default.Lan, contentDescription = null, tint = NeonMagenta)
                        Text(
                            text = "LAN & DHCP SERVER",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp,
                        )
                    }

                    lanSettings?.let { lan ->
                        InfoItem("Gateway IP", lan.ipAddress)
                        InfoItem("Subnet Mask", lan.subnetMask)
                        InfoItem("DHCP Range", "${lan.dhcpStart} - ${lan.dhcpEnd}")
                        InfoItem("Lease Time", "${lan.leaseHours} hour(s)")
                        InfoItem("DNS Server", lan.dnsIp)
                    }
                }
            }

            // Modem Administration Card
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
                        Icon(Icons.Default.Lock, contentDescription = null, tint = NeonYellow)
                        Text(
                            text = "MODEM SECURITY & SYSTEM",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp,
                        )
                    }

                    OutlinedButton(
                        onClick = { showPasswordDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Change WebUI Admin Password", modifier = Modifier.padding(start = 4.dp))
                    }

                    OutlinedButton(
                        onClick = { showRebootConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Reboot Modem", modifier = Modifier.padding(start = 4.dp))
                    }

                    Button(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF330005),
                            contentColor = NeonRed,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Factory Reset Modem", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCarrierDialog) {
        var autoMatch by remember { mutableStateOf(autoMatchCarrierApp) }
        AppConfigDialog(
            title = "Configure Carrier App",
            description = "Select a carrier preset or enter a custom package name for your pocket router's SIM provider.",
            presets = AppLauncher.PRESETS,
            currentName = carrierAppName,
            currentPackage = carrierPackageName,
            namePlaceholder = "e.g. Skitto, MyGP, MyBL",
            packagePlaceholder = "e.g. com.skitto",
            extraContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-match by SIM",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Automatically switch app based on active carrier",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = autoMatch,
                        onCheckedChange = { autoMatch = it },
                    )
                }
            },
            onDismiss = { showCarrierDialog = false },
            onConfirm = { newName, newPkg ->
                showCarrierDialog = false
                onSaveCarrierApp(newName, newPkg, autoMatch)
            },
        )
    }

    if (showFallbackRouterDialog) {
        AppConfigDialog(
            title = "Configure Other Router App",
            description = "Select a router app to launch in one tap when the U30S+ isn't reachable.",
            presets = AppLauncher.ROUTER_PRESETS,
            currentName = fallbackRouterAppName,
            currentPackage = fallbackRouterPackageName,
            namePlaceholder = "e.g. TP-Link Tether",
            packagePlaceholder = "e.g. com.tplink.tether",
            onDismiss = { showFallbackRouterDialog = false },
            onConfirm = { newName, newPkg ->
                showFallbackRouterDialog = false
                onSaveFallbackRouterApp(newName, newPkg)
            },
        )
    }

    if (showApnDialog) {
        ApnDialog(
            onDismiss = { showApnDialog = false },
            onConfirm = { name, apn, auth ->
                showApnDialog = false
                onSaveApn(name, apn, auth)
            },
        )
    }

    if (showPasswordDialog) {
        PasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { oldPass, newPass ->
                showPasswordDialog = false
                onChangePassword(oldPass, newPass)
            },
        )
    }

    if (showRebootConfirm) {
        AlertDialog(
            onDismissRequest = { showRebootConfirm = false },
            title = { Text("Reboot Hotspot?") },
            text = { Text("The modem will restart and all devices will momentarily disconnect.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRebootConfirm = false
                        onReboot()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.Black),
                ) {
                    Text("Reboot")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRebootConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Factory Reset Modem?", color = NeonRed) },
            text = { Text("Warning: All Wi-Fi passwords, APN profiles, and custom settings will be wiped back to stock factory defaults.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onFactoryReset()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.Black),
                ) {
                    Text("Yes, Wipe & Reset")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
    }
}

@Composable
private fun ApnDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
) {
    var profileName by remember { mutableStateOf("skittonet") }
    var apn by remember { mutableStateOf("skittonet") }
    var authMode by remember { mutableStateOf("none") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("APN Configuration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "BD Carrier Presets",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppLauncher.APN_PRESETS.forEach { preset ->
                        val isSelected = apn.equals(preset.apn, ignoreCase = true) &&
                            profileName.equals(preset.profileName, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                profileName = preset.profileName
                                apn = preset.apn
                                authMode = preset.authMode
                            },
                            label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = apn,
                    onValueChange = { apn = it },
                    label = { Text("APN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(profileName.trim(), apn.trim(), authMode) },
                enabled = profileName.isNotBlank() && apn.isNotBlank(),
            ) {
                Text("Save & Apply")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun PasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Admin Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = oldPass,
                    onValueChange = { oldPass = it },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(oldPass, newPass) },
                enabled = oldPass.isNotBlank() && newPass.length >= 6,
            ) {
                Text("Update Password")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun AppConfigDialog(
    title: String,
    description: String,
    presets: List<dev.zlt.companion.util.CarrierPreset>,
    currentName: String,
    currentPackage: String,
    namePlaceholder: String,
    packagePlaceholder: String,
    extraContent: (@Composable () -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    var packageName by remember { mutableStateOf(currentPackage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = "Presets",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presets.forEach { preset ->
                        val isSelected = packageName.equals(preset.packageName, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                name = preset.name.substringBefore(" (")
                                packageName = preset.packageName
                            },
                            label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("App Display Name") },
                    placeholder = { Text(namePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Android Package Name") },
                    placeholder = { Text(packagePlaceholder) },
                    supportingText = { Text("Must match the installed APK's package name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                extraContent?.invoke()
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), packageName.trim()) },
                enabled = name.isNotBlank() && packageName.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
