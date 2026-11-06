package dev.zlt.companion.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Pure AMOLED Black palette
val AmoledBlack = Color(0xFF000000)
val DarkSurface = Color(0xFF0A0A0A)
val DarkSurfaceElevated = Color(0xFF141414)
val DarkSurfaceBorder = Color(0xFF242424)

// Vibrant high-contrast accents
val NeonCyan = Color(0xFF00E5FF)       // Download / RX
val NeonMagenta = Color(0xFFD500F9)    // Upload / TX
val NeonGreen = Color(0xFF00E676)      // Excellent / Connected
val NeonYellow = Color(0xFFFFD600)     // Fair / Moderate
val NeonRed = Color(0xFFFF1744)        // Poor / Disconnected

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA0A0A0)
val TextMuted = Color(0xFF666666)

val AmoledColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00363D),
    onPrimaryContainer = NeonCyan,
    secondary = NeonMagenta,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF350040),
    onSecondaryContainer = NeonMagenta,
    tertiary = NeonGreen,
    onTertiary = Color.Black,
    background = AmoledBlack,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceBorder,
    outlineVariant = Color(0xFF1F1F1F),
    error = NeonRed,
    onError = Color.Black,
    errorContainer = Color(0xFF45000B),
    onErrorContainer = NeonRed,
)
