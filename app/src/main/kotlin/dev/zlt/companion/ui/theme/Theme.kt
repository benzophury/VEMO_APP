package dev.zlt.companion.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Signal quality colours, shared by the meter, the readings and the tile. */
object QualityColors {
    val Good = Color(0xFF00E676)
    val GoodDark = Color(0xFF00E676)
    val Fair = Color(0xFFFFD600)
    val FairDark = Color(0xFFFFD600)
    val Poor = Color(0xFFFF1744)
    val PoorDark = Color(0xFFFF1744)
}

private val LightScheme = lightColorScheme(
    primary = Color(0xFF25638F),
    secondary = Color(0xFF50606E),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF97CBFA),
    secondary = Color(0xFFB7C9D8),
)

@Composable
fun ZltTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = AmoledColorScheme, content = content)
}
