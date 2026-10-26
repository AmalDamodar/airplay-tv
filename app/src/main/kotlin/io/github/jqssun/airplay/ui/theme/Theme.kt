package io.github.jqssun.airplay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jqssun.airplay.R
import io.github.jqssun.airplay.ui.DarkGlass
import io.github.jqssun.airplay.ui.LightGlass
import io.github.jqssun.airplay.ui.LocalGlass
import io.github.jqssun.airplay.ui.isTv

// Black, white and shades of them. The only colour is a soft red for errors, so a failure stays visible.
private val DarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color(0xFF111114),
    primaryContainer = Color(0x38FFFFFF),
    onPrimaryContainer = Color.White,
    inversePrimary = Color(0xFF111114),
    secondary = Color(0xB3FFFFFF),
    onSecondary = Color(0xFF111114),
    secondaryContainer = Color(0x38FFFFFF),
    onSecondaryContainer = Color.White,
    tertiary = Color.White,
    onTertiary = Color(0xFF111114),
    tertiaryContainer = Color(0x38FFFFFF),
    onTertiaryContainer = Color.White,
    background = Color.Transparent,
    onBackground = Color.White,
    surface = Color(0x24FFFFFF),
    onSurface = Color.White,
    surfaceVariant = Color(0x1FFFFFFF),
    onSurfaceVariant = Color(0xB3FFFFFF),
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFF1C1C20),
    surfaceBright = Color(0xFF3A3A40),
    surfaceContainerLowest = Color(0x14FFFFFF),
    surfaceContainerLow = Color(0x1AFFFFFF),
    surfaceContainer = Color(0x24FFFFFF),
    surfaceContainerHigh = Color(0xFF2C2C31),
    surfaceContainerHighest = Color(0xFF38383E),
    inverseSurface = Color.White,
    inverseOnSurface = Color(0xFF111114),
    outline = Color(0x66FFFFFF),
    outlineVariant = Color(0x29FFFFFF),
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF3A0B0B),
    errorContainer = Color(0x33FF8A8A),
    onErrorContainer = Color(0xFFFFD9D9),
    scrim = Color.Black,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF111114),
    onPrimary = Color.White,
    primaryContainer = Color(0xB3FFFFFF),
    onPrimaryContainer = Color(0xFF111114),
    inversePrimary = Color.White,
    secondary = Color(0x99111114),
    onSecondary = Color.White,
    secondaryContainer = Color(0xB3FFFFFF),
    onSecondaryContainer = Color(0xFF111114),
    tertiary = Color(0xFF111114),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xB3FFFFFF),
    onTertiaryContainer = Color(0xFF111114),
    background = Color.Transparent,
    onBackground = Color(0xFF111114),
    surface = Color(0x8CFFFFFF),
    onSurface = Color(0xFF111114),
    surfaceVariant = Color(0x66FFFFFF),
    onSurfaceVariant = Color(0x99111114),
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFFE4E4E8),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color(0x66FFFFFF),
    surfaceContainerLow = Color(0x73FFFFFF),
    surfaceContainer = Color(0x8CFFFFFF),
    surfaceContainerHigh = Color(0xFFF2F2F5),
    surfaceContainerHighest = Color(0xFFE9E9ED),
    inverseSurface = Color(0xFF111114),
    inverseOnSurface = Color.White,
    outline = Color(0x66111114),
    outlineVariant = Color(0x29111114),
    error = Color(0xFFC4332F),
    onError = Color.White,
    errorContainer = Color(0x33C4332F),
    onErrorContainer = Color(0xFF5A100E),
    scrim = Color.Black,
)

private val AirPlayShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// Outfit (SIL Open Font License) is a variable font: one file, any weight.
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun outfit(weight: FontWeight) = Font(
    R.font.outfit, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val OutfitFamily = FontFamily(
    outfit(FontWeight.Normal), outfit(FontWeight.Medium), outfit(FontWeight.SemiBold), outfit(FontWeight.Bold),
)

private fun TextStyle.styled(tv: Boolean, weight: FontWeight? = null): TextStyle {
    val scale = if (tv) 1.05f else 1f
    return copy(
        fontFamily = OutfitFamily,
        fontSize = fontSize * scale,
        lineHeight = lineHeight * scale,
        fontWeight = weight ?: fontWeight,
        letterSpacing = if (fontSize.value >= 28f) (-0.4).sp else letterSpacing,
    )
}

private fun typography(tv: Boolean): Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.styled(tv, FontWeight.SemiBold), displayMedium = displayMedium.styled(tv, FontWeight.SemiBold),
        displaySmall = displaySmall.styled(tv, FontWeight.SemiBold), headlineLarge = headlineLarge.styled(tv, FontWeight.SemiBold),
        headlineMedium = headlineMedium.styled(tv, FontWeight.SemiBold), headlineSmall = headlineSmall.styled(tv, FontWeight.SemiBold),
        titleLarge = titleLarge.styled(tv, FontWeight.SemiBold), titleMedium = titleMedium.styled(tv, FontWeight.Medium),
        titleSmall = titleSmall.styled(tv, FontWeight.Medium),
        bodyLarge = bodyLarge.styled(tv), bodyMedium = bodyMedium.styled(tv), bodySmall = bodySmall.styled(tv),
        labelLarge = labelLarge.styled(tv, FontWeight.Medium), labelMedium = labelMedium.styled(tv, FontWeight.Medium),
        labelSmall = labelSmall.styled(tv),
    )
}

@Composable
fun AirPlayTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = themeMode.useDarkColors(isSystemInDarkTheme())
    val tv = isTv()
    CompositionLocalProvider(LocalGlass provides if (darkTheme) DarkGlass else LightGlass) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = typography(tv),
            shapes = AirPlayShapes,
            content = content,
        )
    }
}
