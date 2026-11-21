package io.github.ntvien.liquidglass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.ntvien.liquidglass.internal.backdrop.drawBackdrop
import io.github.ntvien.liquidglass.internal.backdrop.effects.blur
import io.github.ntvien.liquidglass.internal.backdrop.effects.lens
import io.github.ntvien.liquidglass.internal.backdrop.effects.VibrancySaturation
import io.github.ntvien.liquidglass.internal.backdrop.effects.vibrancy
import io.github.ntvien.liquidglass.internal.backdrop.highlight.Highlight
import io.github.ntvien.liquidglass.internal.backdrop.shadow.Shadow

/**
 * Draws liquid glass behind this element. The single entry point to the glass engine.
 *
 * Picks how to draw from the device [GlassQuality] and [GlassStyle.kind]:
 * - Liquid + [GlassQuality.Full]: lens refraction (+ chromatic aberration) + highlight + shadow; blur only when the
 *   glass samples scrolling content.
 * - Liquid + [GlassQuality.BlurOnly]: highlight + shadow (+ blur when sampling content). No AGSL on Android 12.
 * - [GlassKind.Frost] or [GlassQuality.Off]: translucent fill + sheen + stroke, never samples the background.
 *
 * Must be inside a [LiquidGlassScene] to sample anything; outside it the element falls back to frost.
 *
 * Performance budget per screen: at most 6 real (Liquid) glass elements, the tab bar included. Use
 * [GlassDefaults.frost] for repeated items.
 *
 * @param style a preset from [GlassDefaults], or a copy of one.
 * @param shape a [androidx.compose.foundation.shape.RoundedCornerShape]; path-based shapes are slow.
 * @param interaction pass [rememberGlassInteraction] on pressable elements for the press/tilt/glint effect.
 */
@Composable
public fun Modifier.liquidGlass(
    style: GlassStyle = GlassDefaults.card(),
    shape: Shape = GlassDefaults.CardShape,
    interaction: GlassInteraction? = null,
): Modifier {
    val strength=(100-com.lormusic.app.ui.components.LocalLiquidGlassTransparency.current).coerceIn(0,100)/100f
    val quality = LocalGlassQuality.current
    val backdrops = LocalGlassBackdrops.current
    val layer = LocalGlassLayer.current
    val shadowsEnabled = LocalGlassShadows.current
    val colors = LocalGlassColors.current
    val stretch = interaction?.stretchLayer(shape)
    val trackTouch = interaction?.gestureModifier ?: Modifier
    val highlightColor = colors.glassHighlight
    val strokeColor = colors.glassStroke

    val useLiquid = style.kind == GlassKind.Liquid && quality != GlassQuality.Off && backdrops != null
    if (!useLiquid) {
        return this
            .then(trackTouch)
            .then(if (stretch != null) Modifier.graphicsLayer(stretch) else Modifier)
            .then(
                if (style.shadowRadius > 0.dp && shadowsEnabled) {
                    Modifier.shadow(style.shadowRadius / 3, shape, clip = false, ambientColor = style.shadowColor, spotColor = style.shadowColor)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(style.fallbackSurface)
            .frostSheen(highlightColor.copy(alpha = highlightColor.alpha * style.fallbackSheen))
            .border(style.highlightWidth.coerceAtLeast(1.dp), strokeColor, shape)
            .then(interaction?.highlightModifier ?: Modifier)
    }

    val backdrop = if (layer == GlassLayer.Overlay) backdrops.overlay else backdrops.background
    val allowLens = quality == GlassQuality.Full
    // The background is a smooth gradient: blurring it changes nothing but costs GPU time every frame while
    // scrolling. Blur only when the glass samples scrolling content.
    val samplesContent = backdrops.samplesContent || backdrop !== backdrops.background
    val blurRadius = if (samplesContent) style.blur * (.2f+strength*1.8f) else 0.dp
    val useLens = allowLens && style.lensHeight > 0.dp && style.lensAmount > 0.dp
    return this
        // Measure finger movement in the control's coordinates before it stretches.
        .then(trackTouch)
        // Glass gets its own layer: when something else on screen changes, idle glass keeps its recorded drawing.
        // drawBackdrop provides the layer itself when it has a stretch transform.
        .then(if (stretch == null) Modifier.graphicsLayer() else Modifier)
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                // With a lens the saturation is applied by the lens shader: a separate color filter pass costs a full
                // render pass of the backdrop on every frame the backdrop changes.
                if (style.vibrancy && !useLens) vibrancy()
                val optics = GlassOptics.forSize(
                    size.minDimension,
                    blurRadius.toPx(),
                    style.lensHeight.toPx(),
                    style.lensAmount.toPx(),
                    style.chromaticAberration,
                )
                if (optics.blur > 0f) blur(optics.blur)
                if (useLens) {
                    lens(
                        refractionHeight = optics.height,
                        refractionAmount = optics.amount,
                        depthEffect = style.depthEffect,
                        chromaticAberration = style.chromaticAberration,
                        saturation = if (style.vibrancy) VibrancySaturation else 1f,
                        capturePadding = optics.padding,
                    )
                }
            },
            highlight = { Highlight(width = style.highlightWidth, alpha = style.highlightAlpha) },
            shadow = if (style.shadowRadius > 0.dp && shadowsEnabled) {
                { Shadow(radius = style.shadowRadius, color = style.shadowColor) }
            } else {
                null
            },
            layerBlock = stretch,
            onDrawSurface = {
                if (style.surface.alpha > 0f) drawRect(style.surface)
            },
        )
        .then(if (interaction != null) Modifier.clip(shape).then(interaction.highlightModifier) else Modifier)
}

/** Diagonal sheen from the top-left corner, faking glass light when real glass is not drawn. Cached per size. */
private fun Modifier.frostSheen(highlight: Color): Modifier = drawWithCache {
    val brush = Brush.radialGradient(
        colors = listOf(highlight, Color.Transparent),
        center = Offset(size.width * 0.12f, 0f),
        radius = size.maxDimension * 0.7f,
    )
    onDrawBehind { drawRect(brush) }
}
