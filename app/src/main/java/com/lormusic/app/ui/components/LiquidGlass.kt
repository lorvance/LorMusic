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

val LocalLiquidGlassEnabled = staticCompositionLocalOf { false }
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
