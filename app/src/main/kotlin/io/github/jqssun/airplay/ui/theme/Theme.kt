package io.github.jqssun.airplay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.jqssun.airplay.ui.isTv

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DA3FF),
    onPrimary = Color(0xFF071C33),
    primaryContainer = Color(0xFF173653),
    onPrimaryContainer = Color(0xFFD3E7FF),
    inversePrimary = Color(0xFF0B63CE),
    secondary = Color(0xFF8FA3BF),
    onSecondary = Color(0xFF132335),
    secondaryContainer = Color(0xFF263548),
    onSecondaryContainer = Color(0xFFDCE6F3),
    tertiary = Color(0xFF3DDC84),
    onTertiary = Color(0xFF052D19),
    tertiaryContainer = Color(0xFF133D2A),
    onTertiaryContainer = Color(0xFFB2F3D0),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE9EEF5),
    surface = Color(0xFF141A22),
    onSurface = Color(0xFFE9EEF5),
    surfaceVariant = Color(0xFF222B36),
    onSurfaceVariant = Color(0xFFAAB8CA),
    surfaceTint = Color(0xFF4DA3FF),
    surfaceDim = Color(0xFF0D1117),
    surfaceBright = Color(0xFF303B49),
    surfaceContainerLowest = Color(0xFF0A0E13),
    surfaceContainerLow = Color(0xFF141A22),
    surfaceContainer = Color(0xFF1B232D),
    surfaceContainerHigh = Color(0xFF222B36),
    surfaceContainerHighest = Color(0xFF2A3543),
    inverseSurface = Color(0xFFE9EEF5),
    inverseOnSurface = Color(0xFF192330),
    outline = Color(0xFF69788C),
    outlineVariant = Color(0xFF2E3946),
    error = Color(0xFFFF5A5F),
    onError = Color(0xFF39090D),
    errorContainer = Color(0xFF542126),
    onErrorContainer = Color(0xFFFFDADB),
    scrim = Color.Black,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B63CE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E8FF),
    onPrimaryContainer = Color(0xFF10365C),
    inversePrimary = Color(0xFF4DA3FF),
    secondary = Color(0xFF506580),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E8F3),
    onSecondaryContainer = Color(0xFF25384D),
    tertiary = Color(0xFF177743),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC7F1D8),
    onTertiaryContainer = Color(0xFF103D26),
    background = Color(0xFFF4F6FA),
    onBackground = Color(0xFF192330),
    surface = Color.White,
    onSurface = Color(0xFF192330),
    surfaceVariant = Color(0xFFE5EBF3),
    onSurfaceVariant = Color(0xFF526175),
    surfaceTint = Color(0xFF0B63CE),
    surfaceDim = Color(0xFFDCE3EC),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F3F8),
    surfaceContainer = Color(0xFFEAF0F6),
    surfaceContainerHigh = Color(0xFFE3EAF3),
    surfaceContainerHighest = Color(0xFFDAE3EE),
    inverseSurface = Color(0xFF222B36),
    inverseOnSurface = Color(0xFFF0F3F8),
    outline = Color(0xFF748298),
    outlineVariant = Color(0xFFCED8E5),
    error = Color(0xFFBA2634),
    onError = Color.White,
    errorContainer = Color(0xFFFFDADB),
    onErrorContainer = Color(0xFF510E18),
    scrim = Color.Black,
)

private val AirPlayShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

private val PhoneTypography = Typography()
private fun TextStyle.forTv() = copy(fontSize = fontSize * 1.15f, lineHeight = lineHeight * 1.15f)
private val TvTypography = PhoneTypography.run {
    copy(
        displayLarge = displayLarge.forTv(), displayMedium = displayMedium.forTv(),
        displaySmall = displaySmall.forTv(), headlineLarge = headlineLarge.forTv(),
        headlineMedium = headlineMedium.forTv(), headlineSmall = headlineSmall.forTv(),
        titleLarge = titleLarge.forTv(), titleMedium = titleMedium.forTv(), titleSmall = titleSmall.forTv(),
        bodyLarge = bodyLarge.forTv(), bodyMedium = bodyMedium.forTv(), bodySmall = bodySmall.forTv(),
        labelLarge = labelLarge.forTv(), labelMedium = labelMedium.forTv(), labelSmall = labelSmall.forTv(),
    )
}

@Composable
fun AirPlayTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = themeMode.useDarkColors(isSystemInDarkTheme())
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = if (isTv()) TvTypography else PhoneTypography,
        shapes = AirPlayShapes,
        content = content,
    )
}
