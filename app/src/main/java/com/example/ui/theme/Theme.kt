package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("SYSTEM", "Ikuti Sistem"),
    LIGHT("LIGHT", "Merah Putih (Terang)"),
    DARK("DARK", "Mode Gelap")
}

private val LightColorScheme = lightColorScheme(
    primary = BrandRed,
    onPrimary = Color.White,
    primaryContainer = BrandRedContainerLight,
    onPrimaryContainer = OnBrandRedContainerLight,
    secondary = CharcoalBrand,
    onSecondary = Color.White,
    secondaryContainer = CharcoalContainerLight,
    onSecondaryContainer = CharcoalBrand,
    tertiary = BrandRedDark,
    onTertiary = Color.White,
    tertiaryContainer = BrandRedContainerLight,
    onTertiaryContainer = OnBrandRedContainerLight,
    background = SurfaceLight,
    onBackground = Color(0xFF111827),
    surface = CardLight,
    onSurface = Color(0xFF111827),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF4B5563),
    outline = OutlineLight,
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerRedContainer,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandRed,
    onPrimary = Color.White,
    primaryContainer = BrandRedContainerDark,
    onPrimaryContainer = OnBrandRedContainerDark,
    secondary = BrandRedBright,
    onSecondary = Color(0xFF2B0307),
    secondaryContainer = CharcoalContainerDark,
    onSecondaryContainer = Color(0xFFF3F4F6),
    tertiary = BrandRedBright,
    onTertiary = Color(0xFF2B0307),
    tertiaryContainer = BrandRedContainerDark,
    onTertiaryContainer = OnBrandRedContainerDark,
    background = SurfaceDark,
    onBackground = Color(0xFFF3F4F6),
    surface = CardDark,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFA1A8B8),
    outline = OutlineDark,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF5F1418),
    onErrorContainer = DangerRedContainer
)

val FarnadShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun FarnadCellularTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FarnadTypography,
        shapes = FarnadShapes,
        content = content
    )
}
