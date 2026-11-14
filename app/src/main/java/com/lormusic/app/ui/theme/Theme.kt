package com.lormusic.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.lormusic.app.model.AppAccentColor

@Composable
fun LorMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    accentColor: AppAccentColor = AppAccentColor.PIXEL_BLUE,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> remember(accentColor, darkTheme) {
            buildAccentColorScheme(accentColor, darkTheme)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LorTypography,
        content = content
    )
}
