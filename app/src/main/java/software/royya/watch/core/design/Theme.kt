package software.royya.watch.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = RoyyaPrimary,
    onPrimary = RoyyaSurface,
    primaryContainer = RoyyaSoftGreen,
    onPrimaryContainer = RoyyaPrimaryDark,
    secondary = RoyyaPrimaryDark,
    onSecondary = RoyyaSurface,
    background = RoyyaBackground,
    onBackground = RoyyaTextPrimary,
    surface = RoyyaSurface,
    onSurface = RoyyaTextPrimary,
    error = RoyyaCritical,
    onError = RoyyaSurface,
    outline = RoyyaBorder
)

@Composable
fun RoyyaWatchTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
