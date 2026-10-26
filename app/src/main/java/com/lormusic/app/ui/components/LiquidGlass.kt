package com.lormusic.app.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lormusic.app.ui.components.backdrop.Backdrop
import com.lormusic.app.ui.components.backdrop.backdrops.LayerBackdrop
import com.lormusic.app.ui.components.backdrop.backdrops.layerBackdrop
import com.lormusic.app.ui.components.backdrop.drawBackdrop
import com.lormusic.app.ui.components.backdrop.effects.blur
import com.lormusic.app.ui.components.backdrop.effects.colorControls
import com.lormusic.app.ui.components.backdrop.effects.lens
import com.lormusic.app.ui.components.backdrop.highlight.Highlight
import com.lormusic.app.ui.components.backdrop.shadow.Shadow
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

val LocalLiquidGlassEnabled = staticCompositionLocalOf { false }
val LocalPureMaterialYou = staticCompositionLocalOf { false }
val LocalAppBackdrop = staticCompositionLocalOf<Backdrop?> { null }
internal val LocalGlassExport = compositionLocalOf<LayerBackdrop?> { null }

fun isLiquidGlassSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

private const val SURFACE_OPACITY = 0.35f
private val BLUR_RADIUS = 8.dp
private val REFRACTION_HEIGHT = 24.dp
private val REFRACTION_AMOUNT = 48.dp

val GLASS_EDGE_WIDTH = 0.8.dp
val GLASS_EDGE_COLOR = Color.White.copy(alpha = 0.22f)

@Composable
fun glassContentColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color.Black else Color.White

@Composable
fun glassIndicatorColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color.White else Color.Black

@Composable
fun Modifier.appBackdropSource(): Modifier {
    val isGlassActive = LocalLiquidGlassEnabled.current && isLiquidGlassSupported()
    val backdrop = LocalAppBackdrop.current as? LayerBackdrop
    return if (isGlassActive && backdrop != null) {
        this.layerBackdrop(backdrop)
    } else {
        this
    }
}

@Composable
fun Modifier.liquidGlass(
    shape: CornerBasedShape,
    hazeState: HazeState? = null,
    enableBlur: Boolean = true
): Modifier {
    val backdrop = LocalAppBackdrop.current ?: return this
    val glassExport = LocalGlassExport.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isLight = surfaceColor.luminance() > 0.5f

    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            colorControls(
                brightness = if (isLight) 0.05f else -0.05f,
                contrast = 1.15f,
                saturation = 1.35f,
            )
            blur(BLUR_RADIUS.toPx())
            lens(
                refractionHeight = REFRACTION_HEIGHT.toPx(),
                refractionAmount = REFRACTION_AMOUNT.toPx(),
                chromaticAberration = false,
            )
        },
        highlight = { Highlight.Default },
        shadow = { Shadow.Default },
        exportedBackdrop = glassExport,
        onDrawSurface = { drawRect(surfaceColor.copy(alpha = SURFACE_OPACITY)) },
    )
}

@Composable
fun Modifier.liquidGlassBubble(
    shape: CornerBasedShape,
    hazeState: HazeState? = null,
    enableBlur: Boolean = true,
    isLiquidGlass: Boolean = false
): Modifier {
    val isPureMy = LocalPureMaterialYou.current
    val useGlass = !isPureMy && isLiquidGlass && isLiquidGlassSupported()
    val effectiveBlur = !isPureMy && enableBlur
    val baseSurface = MaterialTheme.colorScheme.surface
    val isLight = baseSurface.luminance() > 0.5f
    val backdrop = LocalAppBackdrop.current

    if (isPureMy) {
        return clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = shape
            )
    }

    if (useGlass && backdrop != null) {
        return this
            .clip(shape)
            .liquidGlass(shape = shape, hazeState = hazeState, enableBlur = effectiveBlur)
            .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
    }

    val blurModifier = if (hazeState != null && effectiveBlur) {
        Modifier.hazeEffect(state = hazeState) {
            blurEnabled = true
            backgroundColor = baseSurface
            blurRadius = 22.dp
            tints = listOf(HazeTint(baseSurface.copy(alpha = 0.65f)))
        }
    } else {
        Modifier
    }

    val bgModifier = if (effectiveBlur) {
        Modifier.background(baseSurface.copy(alpha = 0.60f), shape)
    } else {
        Modifier.background(baseSurface.copy(alpha = 0.94f), shape)
    }

    val borderModifier = Modifier.border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            if (isLight) {
                listOf(
                    Color.Black.copy(alpha = 0.08f),
                    Color.Black.copy(alpha = 0.02f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.06f)
                )
            }
        ),
        shape = shape
    )

    return clip(shape)
        .then(blurModifier)
        .then(bgModifier)
        .then(borderModifier)
}

@Composable
fun Modifier.lightweightLiquidGlass(
    shape: CornerBasedShape,
    fallbackColor: Color,
): Modifier {
    val useGlass = LocalLiquidGlassEnabled.current && isLiquidGlassSupported()
    val surfaceColor = MaterialTheme.colorScheme.surface
    return if (useGlass) {
        clip(shape)
            .background(surfaceColor.copy(alpha = SURFACE_OPACITY), shape)
            .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
    } else {
        clip(shape)
            .background(color = fallbackColor, shape = shape)
            .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
    }
}

@Composable
fun Modifier.liquidGlassSurface(
    shape: CornerBasedShape = RoundedCornerShape(24.dp),
    isLiquidGlass: Boolean = false,
    borderWidth: Dp = 1.2.dp,
    tintColor: Color = Color.Unspecified
): Modifier {
    val enabled = isLiquidGlass && isLiquidGlassSupported()
    if (!enabled) return this

    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val borderBrush = if (isLight) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.92f),
                Color.White.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.72f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.52f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.28f)
            )
        )
    }
    return this.border(width = borderWidth, brush = borderBrush, shape = shape)
}
