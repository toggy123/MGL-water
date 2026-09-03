package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = WarningGold,
    background = SurfaceDark,
    surface = SurfaceCardDark,
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = GeometricBorder,
    outlineVariant = Color(0xFF44474E)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryNavy,
    onPrimary = Color.White,
    primaryContainer = PrimaryGeometricContainer,
    onPrimaryContainer = OnPrimaryGeometricContainer,
    secondary = SecondaryBlue,
    onSecondary = Color.White,
    secondaryContainer = GeometricPill,
    onSecondaryContainer = TextPrimary,
    tertiary = WarningGold,
    background = BackgroundLight,
    surface = SurfaceCard,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = CanvasNeutral,
    onSurfaceVariant = TextSecondary,
    outline = GeometricBorder,
    outlineVariant = Color(0xFFC4C6D0).copy(alpha = 0.5f)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our consistent engineering theme
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
