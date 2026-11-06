package dev.zlt.companion.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zlt.companion.data.SpeedSample
import dev.zlt.companion.ui.theme.DarkSurface
import dev.zlt.companion.ui.theme.DarkSurfaceBorder
import dev.zlt.companion.ui.theme.NeonCyan
import dev.zlt.companion.ui.theme.NeonMagenta
import dev.zlt.companion.ui.theme.TextMuted
import dev.zlt.companion.ui.theme.TextPrimary
import dev.zlt.companion.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun LiveSpeedChart(
    samples: List<SpeedSample>,
    currentRxRate: Long,
    currentTxRate: Long,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .then(
                Modifier.padding(0.dp)
            ),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header with title and live speed badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "LIVE THROUGHPUT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextSecondary,
                    )
                    Text(
                        text = "Real-time Network Activity",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Download rate badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NeonCyan,
                            modifier = Modifier.size(8.dp),
                        ) {}
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "↓ ${formatSpeed(currentRxRate)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = NeonCyan,
                            )
                            Text(
                                text = "DOWNLOAD",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = TextMuted,
                            )
                        }
                    }

                    // Upload rate badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NeonMagenta,
                            modifier = Modifier.size(8.dp),
                        ) {}
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "↑ ${formatSpeed(currentTxRate)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = NeonMagenta,
                            )
                            Text(
                                text = "UPLOAD",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = TextMuted,
                            )
                        }
                    }
                }
            }

            // Real-time Canvas Waveform Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val width = size.width
                    val height = size.height

                    // Grid lines
                    val gridColor = Color(0xFF1E1E1E)
                    for (i in 1..3) {
                        val y = height * (i / 4f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f,
                        )
                    }

                    if (samples.size < 2) return@Canvas

                    // Determine max scale (at least 500 KB/s for good baseline visualization)
                    val maxRx = samples.maxOfOrNull { it.rxBytesPerSec } ?: 0L
                    val maxTx = samples.maxOfOrNull { it.txBytesPerSec } ?: 0L
                    val maxVal = maxOf(maxRx, maxTx, 500 * 1024L).toFloat()

                    val stepX = width / (samples.size - 1).coerceAtLeast(1)

                    // Path for RX (Download)
                    val rxPath = Path()
                    val rxFillPath = Path()

                    // Path for TX (Upload)
                    val txPath = Path()
                    val txFillPath = Path()

                    samples.forEachIndexed { index, sample ->
                        val x = index * stepX
                        val yRx = height - ((sample.rxBytesPerSec / maxVal) * (height - 10f))
                        val yTx = height - ((sample.txBytesPerSec / maxVal) * (height - 10f))

                        if (index == 0) {
                            rxPath.moveTo(x, yRx)
                            rxFillPath.moveTo(x, height)
                            rxFillPath.lineTo(x, yRx)

                            txPath.moveTo(x, yTx)
                            txFillPath.moveTo(x, height)
                            txFillPath.lineTo(x, yTx)
                        } else {
                            rxPath.lineTo(x, yRx)
                            rxFillPath.lineTo(x, yRx)

                            txPath.lineTo(x, yTx)
                            txFillPath.lineTo(x, yTx)
                        }
                    }

                    rxFillPath.lineTo(width, height)
                    rxFillPath.close()

                    txFillPath.lineTo(width, height)
                    txFillPath.close()

                    // Draw translucent fills
                    drawPath(
                        path = rxFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.25f), Color.Transparent),
                        ),
                    )
                    drawPath(
                        path = txFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(NeonMagenta.copy(alpha = 0.20f), Color.Transparent),
                        ),
                    )

                    // Draw stroke curves
                    drawPath(
                        path = rxPath,
                        color = NeonCyan,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawPath(
                        path = txPath,
                        color = NeonMagenta,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
        }
    }
}

fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return "0 B/s"
    val k = 1024.0
    val kb = bytesPerSec / k
    val mb = kb / k
    return when {
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB/s", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB/s", kb)
        else -> "$bytesPerSec B/s"
    }
}
