package com.lormusic.app.ui.theme

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

private val HalloweenColors = darkColorScheme(
    primary = Color(0xFFFF7518),
    onPrimary = Color(0xFF1E0700),
    primaryContainer = Color(0xFF561A00),
    onPrimaryContainer = Color(0xFFFFDBCD),
    secondary = Color(0xFFFF9E40),
    onSecondary = Color(0xFF2C1300),
    secondaryContainer = Color(0xFF593000),
    onSecondaryContainer = Color(0xFFFFDCBE),
    tertiary = Color(0xFFC77DFF),
    onTertiary = Color(0xFF2E004F),
    tertiaryContainer = Color(0xFF4C007E),
    onTertiaryContainer = Color(0xFFF2DAFF),
    background = Color(0xFF0F0B0E),
    onBackground = Color(0xFFEDE0DD),
    surface = Color(0xFF181116),
    onSurface = Color(0xFFEDE0DD),
    surfaceVariant = Color(0xFF2B1D25),
    onSurfaceVariant = Color(0xFFDCC2BA),
    outline = Color(0xFF8F6350),
    outlineVariant = Color(0xFF4A342B)
)

private val LorDarkColors = darkColorScheme(
    primary = LorPrimaryDark,
    onPrimary = LorOnPrimaryDark,
    primaryContainer = LorPrimaryContainerDark,
    onPrimaryContainer = LorOnPrimaryContainerDark,
    secondary = LorSecondaryDark,
    onSecondary = LorOnSecondaryDark,
    secondaryContainer = LorSecondaryContainerDark,
    onSecondaryContainer = LorOnSecondaryContainerDark,
    tertiary = LorTertiaryDark,
    onTertiary = LorOnTertiaryDark,
    tertiaryContainer = LorTertiaryContainerDark,
    onTertiaryContainer = LorOnTertiaryContainerDark,
    background = LorBackgroundDark,
    onBackground = LorOnBackgroundDark,
    surface = LorSurfaceDark,
    onSurface = LorOnSurfaceDark,
    surfaceVariant = LorSurfaceVariantDark,
    onSurfaceVariant = LorOnSurfaceVariantDark,
    outline = LorOutlineDark
)

private val LorLightColors = lightColorScheme(
    primary = LorPrimaryLight,
    onPrimary = LorOnPrimaryLight,
    primaryContainer = LorPrimaryContainerLight,
    onPrimaryContainer = LorOnPrimaryContainerLight,
    secondary = LorSecondaryLight,
    onSecondary = LorOnSecondaryLight,
    secondaryContainer = LorSecondaryContainerLight,
    onSecondaryContainer = LorOnSecondaryContainerLight,
    tertiary = LorTertiaryLight,
    onTertiary = LorOnTertiaryLight,
    tertiaryContainer = LorTertiaryContainerLight,
    onTertiaryContainer = LorOnTertiaryContainerLight,
    background = LorBackgroundLight,
    onBackground = LorOnBackgroundLight,
    surface = LorSurfaceLight,
    onSurface = LorOnSurfaceLight,
    surfaceVariant = LorSurfaceVariantLight,
    onSurfaceVariant = LorOnSurfaceVariantLight,
    outline = LorOutlineLight
)

@Composable
fun LorMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    isHalloweenMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isHalloweenMode -> HalloweenColors
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LorDarkColors
        else -> LorLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LorTypography,
        content = content
    )
}
