package com.lormusic.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.lormusic.app.model.AppAccentColor

val LorPrimaryDark = Color(0xFF6489D8)
val LorOnPrimaryDark = Color(0xFF0A1428)
val LorPrimaryContainerDark = Color(0xFF283B66)
val LorOnPrimaryContainerDark = Color(0xFFD6E3FF)

val LorSecondaryDark = Color(0xFF8AA8E6)
val LorOnSecondaryDark = Color(0xFF0E1B36)
val LorSecondaryContainerDark = Color(0xFF213052)
val LorOnSecondaryContainerDark = Color(0xFFD6E3FF)

val LorTertiaryDark = Color(0xFFA5BFEF)
val LorOnTertiaryDark = Color(0xFF10203E)
val LorTertiaryContainerDark = Color(0xFF2B3F6B)
val LorOnTertiaryContainerDark = Color(0xFFE2ECFF)

val LorBackgroundDark = Color(0xFF0C0E13)
val LorOnBackgroundDark = Color(0xFFEAEFF8)
val LorSurfaceDark = Color(0xFF11141B)
val LorOnSurfaceDark = Color(0xFFEAEFF8)
val LorSurfaceVariantDark = Color(0xFF1D2331)
val LorOnSurfaceVariantDark = Color(0xFFC1CADB)
val LorOutlineDark = Color(0xFF424D66)

val LorPrimaryLight = Color(0xFF4B72C4)
val LorOnPrimaryLight = Color(0xFFFFFFFF)
val LorPrimaryContainerLight = Color(0xFFD8E4FC)
val LorOnPrimaryContainerLight = Color(0xFF0F2147)

val LorSecondaryLight = Color(0xFF5879BE)
val LorOnSecondaryLight = Color(0xFFFFFFFF)
val LorSecondaryContainerLight = Color(0xFFE1EAFC)
val LorOnSecondaryContainerLight = Color(0xFF14264C)

val LorTertiaryLight = Color(0xFF6489D8)
val LorOnTertiaryLight = Color(0xFFFFFFFF)
val LorTertiaryContainerLight = Color(0xFFD5E2FC)
val LorOnTertiaryContainerLight = Color(0xFF102248)

val LorBackgroundLight = Color(0xFFF7F9FC)
val LorOnBackgroundLight = Color(0xFF161920)
val LorSurfaceLight = Color(0xFFFFFFFF)
val LorOnSurfaceLight = Color(0xFF161920)
val LorSurfaceVariantLight = Color(0xFFE9EEF7)
val LorOnSurfaceVariantLight = Color(0xFF414754)
val LorOutlineLight = Color(0xFFBEC6D6)

val AppAccentColor.seedColor: Color
    get() = Color(primaryHex)

val AppAccentColor.lightToneColor: Color
    get() = lightHex?.let { Color(it) } ?: lerp(seedColor, Color.White, 0.38f)

val AppAccentColor.deepToneColor: Color
    get() = deepHex?.let { Color(it) } ?: lerp(Color(0xFF10131A), seedColor, 0.48f)

fun buildAccentColorScheme(accent: AppAccentColor, darkTheme: Boolean): ColorScheme {
    val seed = accent.seedColor
    return if (darkTheme) {
        darkColorScheme(
            primary = seed,
            onPrimary = lerp(Color(0xFF07090E), seed, 0.20f),
            primaryContainer = accent.deepHex?.let { Color(it) } ?: lerp(Color(0xFF10131A), seed, 0.44f),
            onPrimaryContainer = lerp(seed, Color.White, 0.62f),
            inversePrimary = lerp(seed, Color(0xFF10131A), 0.35f),
            secondary = lerp(seed, Color.White, 0.18f),
            onSecondary = lerp(Color(0xFF080B10), seed, 0.22f),
            secondaryContainer = lerp(Color(0xFF12151D), seed, 0.32f),
            onSecondaryContainer = lerp(seed, Color.White, 0.54f),
            tertiary = accent.lightHex?.let { Color(it) } ?: lerp(seed, Color.White, 0.30f),
            onTertiary = lerp(Color(0xFF090C12), seed, 0.24f),
            tertiaryContainer = lerp(Color(0xFF141820), seed, 0.38f),
            onTertiaryContainer = lerp(seed, Color.White, 0.66f),
            background = lerp(Color(0xFF0B0C10), seed, 0.04f),
            onBackground = lerp(Color(0xFFECEEF4), seed, 0.08f),
            surface = lerp(Color(0xFF0E1015), seed, 0.05f),
            onSurface = lerp(Color(0xFFECEEF4), seed, 0.08f),
            surfaceVariant = lerp(Color(0xFF1B1E26), seed, 0.16f),
            onSurfaceVariant = lerp(Color(0xFFC1C5D0), seed, 0.22f),
            surfaceTint = seed,
            outline = lerp(Color(0xFF666B78), seed, 0.30f),
            outlineVariant = lerp(Color(0xFF2B2F3A), seed, 0.20f),
            surfaceContainerLowest = lerp(Color(0xFF08090C), seed, 0.03f),
            surfaceContainerLow = lerp(Color(0xFF12141A), seed, 0.07f),
            surfaceContainer = lerp(Color(0xFF161920), seed, 0.10f),
            surfaceContainerHigh = lerp(Color(0xFF1C2028), seed, 0.13f),
            surfaceContainerHighest = lerp(Color(0xFF242832), seed, 0.17f)
        )
    } else {
        lightColorScheme(
            primary = lerp(seed, Color(0xFF121620), 0.14f),
            onPrimary = Color.White,
            primaryContainer = accent.lightHex?.let { Color(it) } ?: lerp(Color.White, seed, 0.28f),
            onPrimaryContainer = lerp(Color(0xFF0C1018), seed, 0.36f),
            inversePrimary = seed,
            secondary = lerp(seed, Color(0xFF222732), 0.22f),
            onSecondary = Color.White,
            secondaryContainer = lerp(Color.White, seed, 0.20f),
            onSecondaryContainer = lerp(Color(0xFF10141E), seed, 0.34f),
            tertiary = seed,
            onTertiary = Color.White,
            tertiaryContainer = lerp(Color.White, seed, 0.34f),
            onTertiaryContainer = lerp(Color(0xFF0C1018), seed, 0.38f),
            background = lerp(Color(0xFFF7F8FC), seed, 0.04f),
            onBackground = lerp(Color(0xFF15171E), seed, 0.08f),
            surface = Color.White,
            onSurface = lerp(Color(0xFF15171E), seed, 0.08f),
            surfaceVariant = lerp(Color(0xFFE7EAF2), seed, 0.16f),
            onSurfaceVariant = lerp(Color(0xFF3F434F), seed, 0.22f),
            surfaceTint = seed,
            outline = lerp(Color(0xFF707582), seed, 0.26f),
            outlineVariant = lerp(Color(0xFFC5C9D4), seed, 0.20f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = lerp(Color(0xFFF2F4F8), seed, 0.06f),
            surfaceContainer = lerp(Color(0xFFECEEF4), seed, 0.09f),
            surfaceContainerHigh = lerp(Color(0xFFE5E8F0), seed, 0.12f),
            surfaceContainerHighest = lerp(Color(0xFFDDE1EB), seed, 0.15f)
        )
    }
}
