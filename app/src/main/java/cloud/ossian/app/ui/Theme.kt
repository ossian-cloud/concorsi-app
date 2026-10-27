package cloud.ossian.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/* M3 scheme built around the brand teal #0F5E6E (light primary); tertiary is the brand amber, used for
 * "soon" deadlines. No dynamic color: the brand stays. Every text/background pair is >= 4.5:1 (WCAG),
 * checked when the values were chosen: e.g. onSurfaceVariant on surfaceContainerHighest 7.2:1 in both themes. */

private val Light = lightColorScheme(
    primary = Color(0xFF0F5E6E), onPrimary = Color.White,
    primaryContainer = Color(0xFFB6EBFA), onPrimaryContainer = Color(0xFF001F27),
    inversePrimary = Color(0xFF87D1E4),
    secondary = Color(0xFF4A6268), onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE7EE), onSecondaryContainer = Color(0xFF051F24),
    tertiary = Color(0xFF785A00), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDF9E), onTertiaryContainer = Color(0xFF261A00),
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5FAFC), onBackground = Color(0xFF171C1E),
    surface = Color(0xFFF5FAFC), onSurface = Color(0xFF171C1E),
    surfaceVariant = Color(0xFFDBE4E7), onSurfaceVariant = Color(0xFF3F484B),
    surfaceTint = Color(0xFF0F5E6E),
    inverseSurface = Color(0xFF2C3133), inverseOnSurface = Color(0xFFECF1F4),
    outline = Color(0xFF6F797B), outlineVariant = Color(0xFFBFC8CB), scrim = Color.Black,
    surfaceBright = Color(0xFFF5FAFC), surfaceDim = Color(0xFFD5DBDD),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFEFF4F7),
    surfaceContainer = Color(0xFFE9EFF1), surfaceContainerHigh = Color(0xFFE3E9EB),
    surfaceContainerHighest = Color(0xFFDEE3E6),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF87D1E4), onPrimary = Color(0xFF003640),
    primaryContainer = Color(0xFF004E5C), onPrimaryContainer = Color(0xFFB6EBFA),
    inversePrimary = Color(0xFF0F5E6E),
    secondary = Color(0xFFB1CBD2), onSecondary = Color(0xFF1C3439),
    secondaryContainer = Color(0xFF334A50), onSecondaryContainer = Color(0xFFCDE7EE),
    tertiary = Color(0xFFEFC048), onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5B4300), onTertiaryContainer = Color(0xFFFFDF9E),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1416), onBackground = Color(0xFFDEE3E6),
    surface = Color(0xFF0F1416), onSurface = Color(0xFFDEE3E6),
    surfaceVariant = Color(0xFF3F484B), onSurfaceVariant = Color(0xFFBFC8CB),
    surfaceTint = Color(0xFF87D1E4),
    inverseSurface = Color(0xFFDEE3E6), inverseOnSurface = Color(0xFF2C3133),
    outline = Color(0xFF899295), outlineVariant = Color(0xFF3F484B), scrim = Color.Black,
    surfaceBright = Color(0xFF353A3C), surfaceDim = Color(0xFF0F1416),
    surfaceContainerLowest = Color(0xFF0A0F11), surfaceContainerLow = Color(0xFF171C1E),
    surfaceContainer = Color(0xFF1B2022), surfaceContainerHigh = Color(0xFF252B2D),
    surfaceContainerHighest = Color(0xFF303638),
)

@Composable
fun OssianTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
