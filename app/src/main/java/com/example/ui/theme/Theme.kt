package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val RomelLightColorScheme = lightColorScheme(
    primary = RomelNavy,
    onPrimary = CleanWhite,
    primaryContainer = LightBlueSurface,
    onPrimaryContainer = RomelNavy,
    secondary = RomelRed,
    onSecondary = CleanWhite,
    secondaryContainer = RomelRedSoft,
    onSecondaryContainer = RomelRedDark,
    tertiary = StatusEmerald,
    onTertiary = CleanWhite,
    tertiaryContainer = StatusEmeraldSoft,
    onTertiaryContainer = Color(0xFF14532D),
    background = OffWhiteBg,
    onBackground = RomelText,
    surface = CleanWhite,
    onSurface = RomelText,
    surfaceVariant = LightBlueSurface,
    onSurfaceVariant = SlateMuted,
    outline = SlateBorder,
    outlineVariant = Color(0xFFE2E8F3),
    error = StatusRose,
    onError = CleanWhite,
    errorContainer = StatusRoseSoft,
    onErrorContainer = Color(0xFF991B1B)
)

private val RomelDarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = RomelNavyDark,
    primaryContainer = RomelNavyLight,
    onPrimaryContainer = CleanWhite,
    secondary = RomelRed,
    onSecondary = CleanWhite,
    secondaryContainer = Color(0xFF5C1119),
    onSecondaryContainer = Color(0xFFFECACA),
    tertiary = Color(0xFF34D399),
    onTertiary = RomelNavyDark,
    background = DarkBg,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = Color(0xFF28426B),
    error = Color(0xFFF87171),
    onError = RomelNavyDark
)

val RomelShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RomelTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) RomelDarkColorScheme else RomelLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = RomelShapes,
        content = content
    )
}
