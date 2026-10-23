package com.lormusic.app.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lormusic.app.ui.components.backdrop.Backdrop
import com.lormusic.app.ui.components.backdrop.backdrops.LayerBackdrop
import com.lormusic.app.ui.components.backdrop.drawBackdrop
import com.lormusic.app.ui.components.backdrop.effects.blur
import com.lormusic.app.ui.components.backdrop.effects.colorControls
import com.lormusic.app.ui.components.backdrop.effects.lens
import com.lormusic.app.ui.components.backdrop.highlight.Highlight
import com.lormusic.app.ui.components.backdrop.highlight.HighlightElement
import com.lormusic.app.ui.components.backdrop.internal.ShapeProvider
import com.lormusic.app.ui.components.backdrop.shadow.Shadow
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

val LocalLiquidGlassEnabled = staticCompositionLocalOf { false }
val LocalPureMaterialYou = staticCompositionLocalOf { false }
val LocalAppBackdrop = staticCompositionLocalOf<Backdrop?> { null }
internal val LocalGlassExport = compositionLocalOf<LayerBackdrop?> { null }

fun isLiquidGlassSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

private const val VIBRANCY = 1f
private const val BLUR_RADIUS_DP = 8f
private const val LENS_HEIGHT = 0.5f
private const val LENS_AMOUNT = 0.5f
private const val LENS_MAX_DP = 48f
private const val SURFACE_OPACITY = 0.4f
private const val GLASS_RESOLUTION_SCALE = 0.33f

val GLASS_EDGE_WIDTH = 0.5.dp
val GLASS_EDGE_COLOR = Color.White.copy(alpha = 0.10f)

@Composable
fun glassContentColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color.Black else Color.White

@Composable
fun glassIndicatorColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color.White else Color.Black

@Composable
fun Modifier.liquidGlass(shape: CornerBasedShape): Modifier {
    if (!isLiquidGlassSupported()) return this
    val backdrop = LocalAppBackdrop.current ?: return this
    val exportedBackdrop = LocalGlassExport.current
    val density = LocalDensity.current
    val blurPx = with(density) { BLUR_RADIUS_DP.dp.toPx() } * GLASS_RESOLUTION_SCALE
    val lensHeightPx = with(density) { (LENS_HEIGHT * LENS_MAX_DP).dp.toPx() } * GLASS_RESOLUTION_SCALE
    val lensAmountPx = with(density) { (LENS_AMOUNT * LENS_MAX_DP).dp.toPx() } * GLASS_RESOLUTION_SCALE
    val surfaceTintColor = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFFAFAFA)
    } else {
        Color(0xFF121212)
    }

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            colorControls(saturation = 1f + 0.5f * VIBRANCY)
            blur(blurPx)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                lens(
                    refractionHeight = lensHeightPx,
                    refractionAmount = lensAmountPx,
                    depthEffect = true,
                    chromaticAberration = true,
                )
            }
        },
        highlight = { Highlight.Default },
        shadow = { Shadow.Default },
        onDrawSurface = {
            drawRect(color = surfaceTintColor.copy(alpha = SURFACE_OPACITY), size = size)
        },
        exportedBackdrop = exportedBackdrop,
        backdropScale = GLASS_RESOLUTION_SCALE,
    )
}

@Composable
fun Modifier.liquidGlassBubble(
    shape: CornerBasedShape,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    enableBlur: Boolean = true,
    isLiquidGlass: Boolean = false
): Modifier {
    val isPureMy = LocalPureMaterialYou.current
    val useGlass = !isPureMy && isLiquidGlass && isLiquidGlassSupported()
    val effectiveBlur = !isPureMy && enableBlur
    val baseSurface = MaterialTheme.colorScheme.surface
    val glassTint = if (baseSurface.luminance() > 0.5f) Color(0xFFFAFAFA) else Color(0xFF141414)
    val shapeProvider = ShapeProvider { shape }

    val blurModifier = if (hazeState != null && (useGlass || effectiveBlur)) {
        Modifier.hazeEffect(state = hazeState) {
            blurEnabled = true
            backgroundColor = baseSurface
            blurRadius = if (useGlass) 28.dp else 22.dp
            tints = listOf(dev.chrisbanes.haze.HazeTint(baseSurface.copy(alpha = if (useGlass) 0.35f else 0.65f)))
        }
    } else {
        Modifier
    }

    val bgModifier = if (isPureMy) {
        Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
    } else if (useGlass) {
        Modifier.background(glassTint.copy(alpha = 0.35f), shape)
    } else if (effectiveBlur) {
        Modifier.background(baseSurface.copy(alpha = 0.60f), shape)
    } else {
        Modifier.background(baseSurface.copy(alpha = 0.94f), shape)
    }

    val highlightModifier = if (useGlass) {
        HighlightElement(
            shapeProvider = shapeProvider,
            highlight = { Highlight.Default }
        )
    } else {
        Modifier
    }

    val isLight = baseSurface.luminance() > 0.5f
    val borderModifier = if (isPureMy) {
        Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            shape = shape
        )
    } else {
        Modifier.border(
            width = if (useGlass) 1.2.dp else 1.dp,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                if (isLight) {
                    listOf(
                        Color.Black.copy(alpha = if (useGlass) 0.12f else 0.08f),
                        Color.Black.copy(alpha = if (useGlass) 0.04f else 0.02f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = if (useGlass) 0.38f else 0.22f),
                        Color.White.copy(alpha = if (useGlass) 0.12f else 0.06f)
                    )
                }
            ),
            shape = shape
        )
    }

    return clip(shape)
        .then(blurModifier)
        .then(bgModifier)
        .then(highlightModifier)
        .then(borderModifier)
}

@Composable
fun Modifier.lightweightLiquidGlass(
    shape: CornerBasedShape,
    fallbackColor: Color,
): Modifier {
    val useGlass = LocalLiquidGlassEnabled.current && isLiquidGlassSupported()
    val glassTint = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFFAFAFA)
    } else {
        Color(0xFF121212)
    }
    val shapeProvider = ShapeProvider { shape }

    return clip(shape)
        .background(
            color = if (useGlass) glassTint.copy(alpha = SURFACE_OPACITY) else fallbackColor,
            shape = shape,
        )
        .then(
            if (useGlass) {
                HighlightElement(
                    shapeProvider = shapeProvider,
                    highlight = { Highlight.Default },
                )
            } else {
                Modifier
            },
        )
        .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
}

@Composable
fun Modifier.liquidGlassSurface(
    shape: CornerBasedShape = RoundedCornerShape(24.dp),
    isLiquidGlass: Boolean = false,
    borderWidth: Dp = 1.2.dp,
    tintColor: Color = Color.Unspecified
): Modifier {
    val enabled = isLiquidGlass && isLiquidGlassSupported() && LocalAppBackdrop.current != null
    return if (enabled) {
        this.liquidGlass(shape)
    } else {
        this
    }
}
