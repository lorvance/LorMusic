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
import com.kyant.backdrop.backdrops.layerBackdrop as modernLayerBackdrop
import io.github.ntvien.liquidglass.liquidGlass as modernLiquidGlass
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

val LocalLiquidGlassTransparency = compositionLocalOf { com.lormusic.app.data.GlassStrength.DEFAULT_TRANSPARENCY }

internal fun glassBlurDp(percent: Int): Float = 80f * (1f - percent.coerceIn(0, 100) / 100f)
internal fun glassTintAlpha(percent: Int): Float = .02f + .70f * (1f - percent.coerceIn(0, 100) / 100f)

val LocalLiquidGlassEnabled = staticCompositionLocalOf { false }
val LocalPureMaterialYou = staticCompositionLocalOf { false }
val LocalAppBackdrop = staticCompositionLocalOf<Backdrop?> { null }
val LocalPreviewBackdrop = staticCompositionLocalOf<Backdrop?> { null }
internal val LocalGlassExport = compositionLocalOf<LayerBackdrop?> { null }

fun isLiquidGlassSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

private val REFRACTION_HEIGHT = 36.dp
private val REFRACTION_AMOUNT = 88.dp

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
        this.layerBackdrop(backdrop).then(LocalModernBackdrop.current?.let { Modifier.modernLayerBackdrop(it as com.kyant.backdrop.backdrops.LayerBackdrop) } ?: Modifier)
    } else {
        this
    }
}

@Composable
fun Modifier.liquidGlass(
    shape: CornerBasedShape,
    hazeState: HazeState? = null,
    enableBlur: Boolean = true,
    exportedBackdrop: LayerBackdrop? = null
): Modifier {
    return this.modernLiquidGlass(
        style = headerGlassStyle(enableBlur),
        shape = shape,
        interaction = io.github.ntvien.liquidglass.rememberGlassInteraction(),
    )
}

/** Shared optical treatment for the title and action bubbles in screen headers. */
@Composable
fun headerGlassStyle(enableBlur: Boolean = true): io.github.ntvien.liquidglass.GlassStyle {
    val surface = MaterialTheme.colorScheme.surface
    val power = 1f - LocalLiquidGlassTransparency.current.coerceIn(0, 100) / 100f
    return io.github.ntvien.liquidglass.GlassDefaults.card().copy(
        blur = if (enableBlur) 36.dp else 0.dp,
        lensHeight = 24.dp,
        lensAmount = 64.dp,
        chromaticAberration = false,
        vibrancy = false,
        surface = surface.copy(alpha = .04f + .12f * power),
        fallbackSurface = surface.copy(alpha = .86f),
        shadowRadius = 0.dp,
        highlightWidth = .65.dp,
        highlightAlpha = .65f,
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
            .liquidGlass(shape = shape, hazeState = hazeState, enableBlur = effectiveBlur)
    }

    val blurModifier = if (hazeState != null && effectiveBlur) {
        Modifier.hazeEffect(state = hazeState) {
            blurEnabled = true
            backgroundColor = baseSurface
            blurRadius = 52.dp
            tints = listOf(HazeTint(baseSurface.copy(alpha = 0.45f)))
        }
    } else {
        Modifier
    }

    val bgModifier = when {
        hazeState != null && effectiveBlur -> Modifier.background(baseSurface.copy(alpha = 0.25f), shape)
        effectiveBlur -> Modifier.background(baseSurface.copy(alpha = 0.60f), shape)
        else -> Modifier.background(baseSurface.copy(alpha = 0.94f), shape)
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
                    Color.White.copy(alpha = 0.24f),
                    Color.White.copy(alpha = 0.07f)
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
    val surfaceOpacity = glassTintAlpha(LocalLiquidGlassTransparency.current)
    return if (useGlass) {
        clip(shape)
            .background(surfaceColor.copy(alpha = surfaceOpacity), shape)
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
