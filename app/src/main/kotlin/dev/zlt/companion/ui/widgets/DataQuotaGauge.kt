package dev.zlt.companion.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zlt.companion.data.RouterStatus
import dev.zlt.companion.ui.theme.DarkSurface
import dev.zlt.companion.ui.theme.DarkSurfaceBorder
import dev.zlt.companion.ui.theme.NeonCyan
import dev.zlt.companion.ui.theme.NeonGreen
import dev.zlt.companion.ui.theme.NeonRed
import dev.zlt.companion.ui.theme.NeonYellow
import dev.zlt.companion.ui.theme.TextMuted
import dev.zlt.companion.ui.theme.TextPrimary
import dev.zlt.companion.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun DataQuotaGauge(
    monthlyBytes: Long,
    limitBytes: Long = 50L * 1024L * 1024L * 1024L, // Default 50 GB
    modifier: Modifier = Modifier,
) {
    val fraction = if (limitBytes > 0) (monthlyBytes.toFloat() / limitBytes.toFloat()).coerceIn(0f, 1f) else 0f
    val percentInt = (fraction * 100).toInt()
    val remainingBytes = (limitBytes - monthlyBytes).coerceAtLeast(0L)

    val gaugeColor = when {
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
                Text(
                    text = "DATA USAGE QUOTA",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = TextSecondary,
                )
                Text(
                    text = "${percentInt}% USED",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = gaugeColor,
                )
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
                    if (fraction > 0f) {
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
                        text = "${percentInt}%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                    )
                    Text(
                        text = "OF 50 GB",
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
                    label = "USED",
                    value = RouterStatus.formatBytes(monthlyBytes),
                    color = TextPrimary,
                )
                QuotaMetric(
                    label = "REMAINING",
                    value = RouterStatus.formatBytes(remainingBytes),
                    color = NeonGreen,
                )
                QuotaMetric(
                    label = "TOTAL LIMIT",
                    value = RouterStatus.formatBytes(limitBytes),
                    color = TextSecondary,
                )
            }
        }
    }
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
