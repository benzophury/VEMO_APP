package dev.zlt.companion.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zlt.companion.data.DataQuotaConfig
import dev.zlt.companion.data.RouterStatus
import dev.zlt.companion.ui.theme.DarkSurface
import dev.zlt.companion.ui.theme.DarkSurfaceBorder
import dev.zlt.companion.ui.theme.DarkSurfaceElevated
import dev.zlt.companion.ui.theme.NeonCyan
import dev.zlt.companion.ui.theme.NeonGreen
import dev.zlt.companion.ui.theme.NeonRed
import dev.zlt.companion.ui.theme.NeonYellow
import dev.zlt.companion.ui.theme.TextMuted
import dev.zlt.companion.ui.theme.TextPrimary
import dev.zlt.companion.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun DataQuotaGauge(
    monthlyBytes: Long,
    quotaConfig: DataQuotaConfig? = null,
    onSaveQuota: (Boolean, String, String, Int) -> Unit = { _, _, _, _ -> },
    onResetTraffic: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    val isEnabled = quotaConfig?.isEnabled ?: true
    val limitBytes = if (isEnabled) {
        quotaConfig?.totalLimitBytes ?: (50L * 1024L * 1024L * 1024L)
    } else {
        0L
    }

    val fraction = if (limitBytes > 0) {
        (monthlyBytes.toFloat() / limitBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val percentInt = (fraction * 100).toInt()
    val remainingBytes = if (limitBytes > 0) (limitBytes - monthlyBytes).coerceAtLeast(0L) else 0L

    val gaugeColor = when {
        !isEnabled -> TextMuted
        fraction >= 0.90f -> NeonRed
        fraction >= 0.75f -> NeonYellow
        else -> NeonGreen
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
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
                    Text(
                        text = "DATA USAGE QUOTA",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextSecondary,
                    )
                    if (!isEnabled) {
                        Text(
                            text = "(OFF)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isEnabled) "${percentInt}% USED" else "UNLIMITED",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = gaugeColor,
                    )
                    IconButton(onClick = { showDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Configure Quota",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .padding(vertical = 4.dp),
            ) {
                Canvas(modifier = Modifier.size(130.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val arcSize = size.width - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Background track (240 degree sweep from 150 to 390)
                    drawArc(
                        color = Color(0xFF1E1E1E),
                        startAngle = 150f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )

                    // Active progress arc
                    if (isEnabled && fraction > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(NeonCyan, gaugeColor),
                            ),
                            startAngle = 150f,
                            sweepAngle = 240f * fraction,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                    }
                }

                // Center readout
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isEnabled) "${percentInt}%" else "∞",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                    )
                    Text(
                        text = if (isEnabled) {
                            "OF ${RouterStatus.formatBytes(limitBytes)}"
                        } else {
                            "NO LIMIT SET"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp,
                    )
                }
            }

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                QuotaMetric(
                    label = "MONTHLY USED",
                    value = RouterStatus.formatBytes(monthlyBytes),
                    color = TextPrimary,
                )
                QuotaMetric(
                    label = "REMAINING",
                    value = if (isEnabled) RouterStatus.formatBytes(remainingBytes) else "Unlimited",
                    color = if (isEnabled) NeonGreen else TextMuted,
                )
                QuotaMetric(
                    label = "WARNING AT",
                    value = if (isEnabled) "${quotaConfig?.alertPercent ?: 90}%" else "Disabled",
                    color = if (isEnabled) NeonYellow else TextMuted,
                )
            }
        }
    }

    if (showDialog) {
        DataQuotaSetupDialog(
            currentConfig = quotaConfig ?: DataQuotaConfig(),
            onDismiss = { showDialog = false },
            onSave = { enabled, limitVal, unitMult, alertPct ->
                onSaveQuota(enabled, limitVal, unitMult, alertPct)
                showDialog = false
            },
            onResetTraffic = {
                onResetTraffic()
                showDialog = false
            },
        )
    }
}

@Composable
fun DataQuotaSetupDialog(
    currentConfig: DataQuotaConfig,
    onDismiss: () -> Unit,
    onSave: (Boolean, String, String, Int) -> Unit,
    onResetTraffic: () -> Unit,
) {
    var enabled by remember { mutableStateOf(currentConfig.isEnabled) }
    var limitValue by remember { mutableStateOf(currentConfig.limitValue.ifEmpty { "50" }) }
    var isGbUnit by remember { mutableStateOf(currentConfig.limitUnitMultiplier != "1") }
    var alertSlider by remember { mutableFloatStateOf(currentConfig.alertPercent.toFloat().coerceIn(50f, 100f)) }
    var showResetConfirm by remember { mutableStateOf(false) }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = DarkSurfaceElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Reset Traffic Counter?") },
            text = { Text("This will calibrate and reset the monthly traffic statistics counter on the router to zero.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetTraffic()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.Black),
                ) {
                    Text("Reset to 0", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "DATA USAGE QUOTA SETUP",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Enable Quota Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Enforce Data Limit",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Monitor monthly usage and alert",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonGreen,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF222222),
                        ),
                    )
                }

                if (enabled) {
                    // Limit Value and Unit Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = limitValue,
                            onValueChange = { limitValue = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Monthly Limit") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkSurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = TextMuted,
                            ),
                        )

                        // Unit Selector (GB / MB)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = isGbUnit,
                                onClick = { isGbUnit = true },
                                label = { Text("GB", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = Color.Black,
                                    containerColor = DarkSurface,
                                    labelColor = TextSecondary,
                                ),
                            )
                            FilterChip(
                                selected = !isGbUnit,
                                onClick = { isGbUnit = false },
                                label = { Text("MB", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = Color.Black,
                                    containerColor = DarkSurface,
                                    labelColor = TextSecondary,
                                ),
                            )
                        }
                    }

                    // Alert threshold slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Alert Warning Threshold",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                            Text(
                                text = "${alertSlider.roundToInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonYellow,
                            )
                        }
                        Slider(
                            value = alertSlider,
                            onValueChange = { alertSlider = it },
                            valueRange = 50f..100f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonYellow,
                                activeTrackColor = NeonYellow,
                                inactiveTrackColor = Color(0xFF333333),
                            ),
                        )
                    }
                }

                // Traffic Calibration / Reset section
                OutlinedButton(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.5f)),
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                        text = "  CALIBRATE / RESET MONTHLY TRAFFIC",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val unitMult = if (isGbUnit) "1024" else "1"
                    val alertPct = alertSlider.roundToInt()
                    onSave(enabled, limitValue.ifEmpty { "50" }, unitMult, alertPct)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
            ) {
                Text("Save Quota", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}

@Composable
private fun QuotaMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
        )
    }
}