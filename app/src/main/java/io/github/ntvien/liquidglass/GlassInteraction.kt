/*
 * Press / drag effect for glass (the glass swells, tilts toward the finger and glints at the touch point).
 * Ported from Kyant0/AndroidLiquidGlass (catalog: InteractiveHighlight.kt, DragGestureInspector.kt, LiquidButton.kt),
 * Copyright Kyant, Apache License 2.0. https://github.com/Kyant0/AndroidLiquidGlass
 * Modified for liquid-glass: spring parameters exposed, wrapped into a reusable GlassInteraction.
 */
package io.github.ntvien.liquidglass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import io.github.ntvien.liquidglass.internal.backdrop.RuntimeShader
import io.github.ntvien.liquidglass.internal.backdrop.asComposeShader
import io.github.ntvien.liquidglass.internal.backdrop.isRuntimeShaderSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.tanh

/**
 * Press state of a glass element: it swells when pressed, tilts toward the finger while dragged and glints at the
 * touch point. It never consumes touches, so `clickable` on the same element still receives the tap.
 *
 * Create it with [rememberGlassInteraction] and pass it to [liquidGlass]. [LiquidSurface] does this for you when it
 * has an `onClick`. All values are read in the draw/layer phase: pressing does not recompose.
 */
@Stable
public class GlassInteraction internal constructor(
    private val scope: CoroutineScope,
    dampingRatio: Float,
    stiffness: Float,
) {
    private val pressSpec = spring(dampingRatio, stiffness, 0.001f)
    private val press = Animatable(0f, 0.001f)
    private var position by mutableStateOf(Offset.Zero)
    private var start = Offset.Zero

    /** 0 = released, 1 = fully pressed. */
    public val pressProgress: Float get() = press.value

    /** Finger offset from where the press started. */
    public val dragOffset: Offset get() = position - start

    private val shader = if (isRuntimeShaderSupported()) {
        RuntimeShader(
            """
layout(color) uniform half4 color;
uniform float radius;
uniform float2 position;
half4 main(float2 coord) {
    float dist = distance(coord, position);
    float intensity = 1.0 - smoothstep(radius * 0.5, radius, dist);
    return color * intensity;
}"""
        )
    } else {
        null
    }

    /** Bounded stretch with a spring return, including gestures that end during a scroll. */
    internal fun stretchLayer(shape: Shape): (GraphicsLayerScope.() -> Unit)? {
        if (shape !is RoundedCornerShape) return null
        return stretch@{
            if (!size.isSpecified ||
                !isCircularGlassBounds(
                    size.width, size.height,
                    shape.topStart.toPx(size, this), shape.topEnd.toPx(size, this),
                    shape.bottomEnd.toPx(size, this), shape.bottomStart.toPx(size, this),
                )
            ) return@stretch
            val progress = press.value.coerceIn(0f, 1f)
            val drag = dragOffset / size.minDimension
            val x = tanh(drag.x * 2f)
            val y = tanh(drag.y * 2f)
            val shift = 7.dp.toPx() * progress
            val swell = 6.dp.toPx() * progress
            val pull = 8.dp.toPx() * progress
            translationX = shift * x
            translationY = shift * y
            scaleX = 1f + (swell + pull * abs(x)) / size.width
            scaleY = 1f + (swell + pull * abs(y)) / size.height
        }
    }

    /** Glint at the touch point. */
    internal val highlightModifier: Modifier = Modifier.drawWithContent {
        val progress = press.value.coerceIn(0f, 1f)
        if (progress > 0f && size.minDimension > 0f) {
            if (shader != null) {
                drawRect(Color.White.copy(alpha = 0.025f * progress), blendMode = BlendMode.Plus)
                val p = position
                shader.setColorUniform("color", Color.White.copy(alpha = 0.095f * progress))
                shader.setFloatUniform("radius", size.minDimension * 1.5f)
                shader.setFloatUniform("position", p.x.fastCoerceIn(0f, size.width), p.y.fastCoerceIn(0f, size.height))
                drawRect(ShaderBrush(shader.asComposeShader()), blendMode = BlendMode.Plus)
            } else {
                drawRect(Color.White.copy(alpha = 0.09f * progress), blendMode = BlendMode.Plus)
            }
        }
        drawContent()
    }

    internal val gestureModifier: Modifier = Modifier.pointerInput(this) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            start = down.position
            position = start
            scope.launch { press.animateTo(1f, pressSpec) }
            try {
                // Wait for the same down event to finish before checking for consumed moves.
                awaitPointerEvent(PointerEventPass.Final)
                trackDrag(down.id) { change -> position = change.position }
            } finally {
                // Release on cancellation as well as finger-up, without re-centering the lens abruptly.
                scope.launch { press.animateTo(0f, pressSpec) }
            }
        }
    }
}

/**
 * Remembers a [GlassInteraction] for a pressable glass element.
 *
 * @param dampingRatio spring damping of the press; lower bounces more.
 * @param stiffness spring stiffness of the press.
 */
@Composable
public fun rememberGlassInteraction(
    dampingRatio: Float = 0.5f,
    stiffness: Float = 300f,
): GlassInteraction {
    val scope = rememberCoroutineScope()
    return remember(scope, dampingRatio, stiffness) {
        GlassInteraction(scope, dampingRatio, stiffness)
    }
}

/** Observe final events without consuming clicks or competing with a scrollable parent. */
private suspend fun AwaitPointerEventScope.trackDrag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Final)
        val change = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (change.changedToUpIgnoreConsumed()) {
            val other = event.changes.fastFirstOrNull { it.pressed } ?: return change
            pointer = other.id
            continue
        }
        if (change.isConsumed) return null
        if (change.previousPosition != change.position) onDrag(change)
    }
}
