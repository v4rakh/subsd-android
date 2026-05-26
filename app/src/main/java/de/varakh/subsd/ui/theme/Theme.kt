package de.varakh.subsd.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Purple80 = Color(0xFFBB86FC)
private val PurpleGrey80 = Color(0xFF9E7BB5)
private val Surface = Color(0xFF121212)
private val SurfaceVariant = Color(0xFF1E1E1E)
private val Background = Color(0xFF0A0A0A)

val SubsdColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF4A148C),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = PurpleGrey80,
    tertiary = Color(0xFF03DAC6),
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF49454F),
    error = Color(0xFFCF6679)
)

@Composable
fun SubsdTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SubsdColorScheme,
        content = content
    )
}
