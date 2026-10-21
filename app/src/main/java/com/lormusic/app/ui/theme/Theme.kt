package com.lormusic.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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
    content: @Composable () -> Unit
) {
    val colorScheme = when {
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
