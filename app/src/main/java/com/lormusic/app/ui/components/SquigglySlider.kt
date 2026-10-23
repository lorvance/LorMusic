package com.lormusic.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

@Composable
fun SquigglySlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    isPlaying: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = activeColor.copy(alpha = 0.28f),
    thumbColor: Color = activeColor
) {
    val view = LocalView.current
    val density = LocalDensity.current

    val waveLengthPx = with(density) { 27.dp.toPx() }
    val lineAmplitudePx = with(density) { 3.2.dp.toPx() }
    val phaseSpeedPxPerSec = with(density) { 14.dp.toPx() }
    val strokeWidthPx = with(density) { 3.6.dp.toPx() }
    val thumbWidthPx = with(density) { 6.dp.toPx() }
    val thumbHeightPx = with(density) { 22.dp.toPx() }

    var isDragging by remember { mutableStateOf(false) }

    val shouldAnimateWave = isPlaying && !isDragging
    val heightFraction by animateFloatAsState(
        targetValue = if (shouldAnimateWave) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (shouldAnimateWave) 650 else 450,
            easing = FastOutSlowInEasing
        ),
        label = "squigglyHeightFraction"
    )

    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.28f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "thumbScale"
    )

    var phaseOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(shouldAnimateWave, heightFraction > 0.01f) {
        if (shouldAnimateWave || heightFraction > 0.01f) {
            var lastTimeNanos = withFrameNanos { it }
            while (true) {
                withFrameNanos { nowNanos ->
                    val dtSec = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
                    lastTimeNanos = nowNanos
                    phaseOffset = (phaseOffset + dtSec * phaseSpeedPxPerSec) % waveLengthPx
                }
            }
        }
    }

    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1f)
    val progress = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    val wavePath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(valueRange) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onValueChange(newValue)
                    onValueChangeFinished()
                }
            }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    },
                    onDragEnd = {
                        isDragging = false
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onValueChangeFinished()
                    },
                    onDragCancel = {
                        isDragging = false
                        onValueChangeFinished()
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    }
                )
            }
    ) {
        val horizontalPad = thumbWidthPx
        val totalWidth = (size.width - horizontalPad * 2f).coerceAtLeast(1f)
        val centerY = size.height / 2f
        val totalProgressPx = totalWidth * progress

        val transitionPeriods = 1.5f
        val waveStart = -phaseOffset - waveLengthPx / 2f
        val waveEnd = totalWidth

        fun lerpInvSat(a: Float, b: Float, v: Float): Float {
            return if (a != b) ((v - a) / (b - a)).coerceIn(0f, 1f) else 0f
        }

        fun computeAmplitude(x: Float, sign: Float): Float {
            val length = transitionPeriods * waveLengthPx
            val coeff = lerpInvSat(totalProgressPx + length / 2f, totalProgressPx - length / 2f, x)
            return sign * heightFraction * lineAmplitudePx * coeff
        }

        wavePath.reset()
        var currentX = waveStart
        var waveSign = 1f
        var currentAmp = computeAmplitude(currentX, waveSign)
        wavePath.moveTo(horizontalPad + currentX, centerY + currentAmp)

        val dist = waveLengthPx / 2f
        while (currentX < waveEnd) {
            waveSign = -waveSign
            val nextX = currentX + dist
            val midX = currentX + dist / 2f
            val nextAmp = computeAmplitude(nextX, waveSign)
            wavePath.cubicTo(
                horizontalPad + midX,
                centerY + currentAmp,
                horizontalPad + midX,
                centerY + nextAmp,
                horizontalPad + nextX,
                centerY + nextAmp
            )
            currentAmp = nextAmp
            currentX = nextX
        }

        clipRect(
            left = horizontalPad,
            top = 0f,
            right = horizontalPad + totalProgressPx,
            bottom = size.height
        ) {
            drawPath(
                path = wavePath,
                color = activeColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        clipRect(
            left = horizontalPad + totalProgressPx,
            top = 0f,
            right = horizontalPad + totalWidth,
            bottom = size.height
        ) {
            drawPath(
                path = wavePath,
                color = inactiveColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        val startAmp = cos(abs(waveStart) / waveLengthPx * (PI.toFloat() * 2f))
        drawCircle(
            color = activeColor,
            radius = strokeWidthPx / 2f,
            center = Offset(horizontalPad, centerY + startAmp * lineAmplitudePx * heightFraction)
        )

        val curThumbW = thumbWidthPx * thumbScale
        val curThumbH = thumbHeightPx * thumbScale
        val thumbX = horizontalPad + totalProgressPx - curThumbW / 2f
        val thumbY = centerY - curThumbH / 2f
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(thumbX, thumbY),
            size = Size(curThumbW, curThumbH),
            cornerRadius = CornerRadius(curThumbW / 2f, curThumbW / 2f)
        )
    }
}

@Composable
fun WavyLinearProgressIndicator(
    progress: Float?,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = activeColor.copy(alpha = 0.24f)
) {
    val density = LocalDensity.current
    val waveLengthPx = with(density) { 24.dp.toPx() }
    val lineAmplitudePx = with(density) { 2.8.dp.toPx() }
    val phaseSpeedPxPerSec = with(density) { 18.dp.toPx() }
    val strokeWidthPx = with(density) { 3.2.dp.toPx() }

    var phaseOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastTimeNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { nowNanos ->
                val dtSec = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
                lastTimeNanos = nowNanos
                phaseOffset = (phaseOffset + dtSec * phaseSpeedPxPerSec) % waveLengthPx
            }
        }
    }

    val wavePath = remember { Path() }
    val effectiveProgress = (progress ?: 0.5f).coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val totalWidth = size.width.coerceAtLeast(1f)
        val centerY = size.height / 2f
        val filledWidthPx = if (progress == null) totalWidth else totalWidth * effectiveProgress

        wavePath.reset()
        val waveStart = -phaseOffset - waveLengthPx / 2f
        val waveEnd = totalWidth

        var currentX = waveStart
        var waveSign = 1f
        var currentAmp = waveSign * lineAmplitudePx
        wavePath.moveTo(currentX, centerY + currentAmp)

        val dist = waveLengthPx / 2f
        while (currentX < waveEnd) {
            waveSign = -waveSign
            val nextX = currentX + dist
            val midX = currentX + dist / 2f
            val nextAmp = waveSign * lineAmplitudePx
            wavePath.cubicTo(
                midX, centerY + currentAmp,
                midX, centerY + nextAmp,
                nextX, centerY + nextAmp
            )
            currentAmp = nextAmp
            currentX = nextX
        }

        if (progress != null && progress > 0f) {
            clipRect(left = 0f, top = 0f, right = filledWidthPx, bottom = size.height) {
                drawPath(
                    path = wavePath,
                    color = activeColor,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
            if (filledWidthPx < totalWidth) {
                clipRect(left = filledWidthPx, top = 0f, right = totalWidth, bottom = size.height) {
                    drawPath(
                        path = wavePath,
                        color = inactiveColor,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }
            }
        } else {
            drawPath(
                path = wavePath,
                color = activeColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }
    }
}
