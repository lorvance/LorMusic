package com.lormusic.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WavyProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeWidth: Dp = 4.dp,
    amplitude: Dp = 3.5.dp,
    waveLength: Dp = 22.dp
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val amplitudePx = with(density) { amplitude.toPx() }
    val waveLengthPx = with(density) { waveLength.toPx() }

    val infiniteTransition = rememberInfiniteTransition(label = "wavyProgressPhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val width = size.width
        val centerY = size.height / 2f
        val clampedProgress = progress.coerceIn(0f, 1f)
        val activeWidth = width * clampedProgress

        if (activeWidth < width) {
            drawLine(
                color = trackColor,
                start = androidx.compose.ui.geometry.Offset(activeWidth, centerY),
                end = androidx.compose.ui.geometry.Offset(width, centerY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
        }

        if (activeWidth > 0f) {
            val path = Path()
            path.moveTo(0f, centerY + sin(phase) * amplitudePx)

            var x = 0f
            val step = 3f
            while (x <= activeWidth) {
                val y = centerY + sin((x / waveLengthPx) * (2f * PI).toFloat() - phase) * amplitudePx
                path.lineTo(x, y)
                x += step
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }
    }
}
