package com.lormusic.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lormusic.app.R
import com.lormusic.app.model.AppIconOption
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

@Composable
fun WavyPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val view = LocalView.current

    val waveMorph by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "waveMorph"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "wavyButtonRotation")
    val continuousAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shapeAngle"
    )

    val iconMorph by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "iconMorph"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.02f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(pressScale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val minDim = this.size.minDimension

            val baseRadius = minDim * 0.445f
            val gentleWaveAmp = minDim * 0.0195f * waveMorph

            rotate(degrees = continuousAngle * waveMorph, pivot = Offset(cx, cy)) {
                val wavyPath = buildSmoothCookiePath(
                    cx = cx,
                    cy = cy,
                    baseRadius = baseRadius,
                    amplitude = gentleWaveAmp,
                    lobes = 12
                )
                drawPath(path = wavyPath, color = containerColor)
            }

            if (iconMorph < 0.5f) {
                val alpha = 1f - (iconMorph * 2f)
                val triSize = minDim * 0.27f
                val playPath = Path().apply {
                    moveTo(cx - triSize * 0.35f, cy - triSize * 0.52f)
                    lineTo(cx + triSize * 0.62f, cy)
                    lineTo(cx - triSize * 0.35f, cy + triSize * 0.52f)
                    close()
                }
                drawPath(path = playPath, color = contentColor.copy(alpha = alpha))
            } else {
                val alpha = (iconMorph - 0.5f) * 2f
                val barWidth = minDim * 0.082f
                val barHeight = minDim * 0.28f
                val gap = minDim * 0.062f
                val corner = CornerRadius(minDim * 0.014f, minDim * 0.014f)

                drawRoundRect(
                    color = contentColor.copy(alpha = alpha),
                    topLeft = Offset(cx - gap / 2f - barWidth, cy - barHeight / 2f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = corner
                )

                drawRoundRect(
                    color = contentColor.copy(alpha = alpha),
                    topLeft = Offset(cx + gap / 2f, cy - barHeight / 2f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = corner
                )
            }
        }
    }
}

@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color? = null
) {
    val shapeCount = 7
    val infiniteTransition = rememberInfiniteTransition(label = "expressiveLoader")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = shapeCount.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = shapeCount * 680, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "morphPhase"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val minDim = this.size.minDimension

            if (containerColor != null) {
                drawCircle(
                    color = containerColor,
                    radius = minDim * 0.48f,
                    center = Offset(cx, cy)
                )
            }

            val stepIndex = floor(phase).toInt()
            val rawFraction = (phase - stepIndex).coerceIn(0f, 1f)

            val morphFraction = FastOutSlowInEasing.transform(rawFraction)

            val currentShapeIdx = stepIndex % shapeCount
            val nextShapeIdx = (stepIndex + 1) % shapeCount

            val rotationDegrees = (stepIndex * 135f) + (morphFraction * 135f)
            val pulseScale = 1f + 0.06f * sin(rawFraction * PI.toFloat())
            val baseR = minDim * (if (containerColor != null) 0.30f else 0.38f) * pulseScale

            rotate(degrees = rotationDegrees, pivot = Offset(cx, cy)) {
                val path = Path()
                val steps = 160
                for (i in 0..steps) {
                    val theta = (i.toFloat() / steps.toFloat()) * (2f * PI.toFloat())
                    val r1 = evalExpressiveShapeRadius(currentShapeIdx, theta)
                    val r2 = evalExpressiveShapeRadius(nextShapeIdx, theta)
                    val r = baseR * (r1 + (r2 - r1) * morphFraction)
                    val x = cx + r * cos(theta)
                    val y = cy + r * sin(theta)
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                path.close()
                drawPath(path = path, color = color)
            }
        }
    }
}

private fun evalExpressiveShapeRadius(shapeIndex: Int, theta: Float): Float {
    return when (shapeIndex % 7) {
        0 -> {

            1.0f + 0.14f * cos(8f * theta) - 0.02f * cos(16f * theta)
        }
        1 -> {

            1.0f + 0.18f * cos(4f * theta) - 0.03f * cos(8f * theta)
        }
        2 -> {

            1.0f + 0.11f * cos(5f * theta) - 0.02f * cos(10f * theta)
        }
        3 -> {

            1.0f + 0.10f * cos(7f * theta) - 0.015f * cos(14f * theta)
        }
        4 -> {

            1.0f + 0.22f * cos(2f * (theta - 0.6f))
        }
        5 -> {

            1.0f + 0.085f * cos(9f * theta) - 0.012f * cos(18f * theta)
        }
        else -> {

            1.0f + 0.22f * cos(4f * (theta + 0.785f)) - 0.04f * cos(8f * (theta + 0.785f))
        }
    }
}

@Composable
fun AppIconVectorPreview(
    option: AppIconOption,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    cornerRadius: Dp = 18.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        when (option) {
            AppIconOption.REGULAR -> {
                Image(
                    painter = painterResource(id = R.drawable.icon_preview_regular),
                    contentDescription = option.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppIconOption.MINIMAL -> {
                Image(
                    painter = painterResource(id = R.drawable.icon_preview_minimal),
                    contentDescription = option.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppIconOption.OLD -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    val cx = w / 2f
                    val cy = h / 2f
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0E0B16),
                                Color(0xFF2D1B69),
                                Color(0xFF6750FF)
                            )
                        )
                    )
                    val teal = Color(0xFF00D5B8)
                    val pink = Color(0xFFFF4D8D)
                    drawCircle(
                        color = teal,
                        radius = w * 0.085f,
                        center = Offset(cx - w * 0.06f, cy + h * 0.10f)
                    )
                    drawLine(
                        color = teal,
                        start = Offset(cx + w * 0.01f, cy + h * 0.10f),
                        end = Offset(cx + w * 0.01f, cy - h * 0.16f),
                        strokeWidth = w * 0.05f,
                        cap = StrokeCap.Round
                    )
                    drawRoundRect(
                        color = teal,
                        topLeft = Offset(cx + w * 0.01f, cy - h * 0.18f),
                        size = Size(w * 0.15f, h * 0.07f),
                        cornerRadius = CornerRadius(w * 0.02f)
                    )
                    drawCircle(
                        color = pink,
                        radius = w * 0.055f,
                        center = Offset(cx + w * 0.12f, cy + h * 0.05f)
                    )
                }
            }
        }
    }
}

@Composable
fun MaterialYouLaunchSplash(
    iconOption: AppIconOption
) {
    var visible by remember { mutableStateOf(true) }
    val scaleAnim = remember { Animatable(0.45f) }
    val glowAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        glowAlpha.animateTo(1f, tween(380))
    }
    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
        delay(1450)
        visible = false
    }

    val primary = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.background

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(tween(420)) + scaleOut(targetScale = 1.14f, animationSpec = tween(420))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(280.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.34f * glowAlpha.value),
                            Color.Transparent
                        )
                    ),
                    radius = size.minDimension / 2f
                )
            }

            Box(
                modifier = Modifier.scale(scaleAnim.value),
                contentAlignment = Alignment.Center
            ) {
                AnimatedSplashIcon(
                    option = iconOption,
                    size = 126.dp,
                    cornerRadius = 36.dp
                )
            }
        }
    }
}

@Composable
fun AnimatedSplashIcon(
    option: AppIconOption,
    modifier: Modifier = Modifier,
    size: Dp = 126.dp,
    cornerRadius: Dp = 36.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        when (option) {
            AppIconOption.REGULAR -> {
                val transition = rememberInfiniteTransition(label = "regularEqualizer")
                val h1 by transition.animateFloat(
                    initialValue = 0.22f,
                    targetValue = 0.70f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(420, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar1"
                )
                val h2 by transition.animateFloat(
                    initialValue = 0.78f,
                    targetValue = 0.28f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(360, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar2"
                )
                val h3 by transition.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 0.85f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(480, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar3"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    val cx = w / 2f
                    val cy = h / 2f

                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF2E1948), Color(0xFF1B0E2E))
                        )
                    )

                    val cookiePath = buildSmoothCookiePath(
                        cx = cx,
                        cy = cy,
                        baseRadius = w * 0.38f,
                        amplitude = w * 0.045f,
                        lobes = 12
                    )
                    drawPath(
                        path = cookiePath,
                        color = Color(0xFFCDB4FF)
                    )

                    val barColor = Color(0xFF1C1124)
                    val barWidth = w * 0.065f
                    val maxBarHeight = h * 0.36f
                    val cornerR = CornerRadius(barWidth / 2f)

                    val b1H = maxBarHeight * h1
                    val b1Left = cx - w * 0.13f - barWidth / 2f
                    val b1Top = cy - b1H / 2f
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(b1Left, b1Top),
                        size = Size(barWidth, b1H),
                        cornerRadius = cornerR
                    )

                    val b2H = maxBarHeight * h2
                    val b2Left = cx - barWidth / 2f
                    val b2Top = cy - b2H / 2f
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(b2Left, b2Top),
                        size = Size(barWidth, b2H),
                        cornerRadius = cornerR
                    )

                    val b3H = maxBarHeight * h3
                    val b3Left = cx + w * 0.13f - barWidth / 2f
                    val b3Top = cy - b3H / 2f
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(b3Left, b3Top),
                        size = Size(barWidth, b3H),
                        cornerRadius = cornerR
                    )
                }
            }

            AppIconOption.OLD -> {
                val flyAnim = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    flyAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 850,
                            easing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1f)
                        )
                    )
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    val cx = w / 2f
                    val cy = h / 2f

                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0E0B16),
                                Color(0xFF2D1B69),
                                Color(0xFF6750FF)
                            )
                        )
                    )

                    val teal = Color(0xFF00D5B8)
                    val pink = Color(0xFFFF4D8D)

                    drawCircle(
                        color = teal,
                        radius = w * 0.085f,
                        center = Offset(cx - w * 0.06f, cy + h * 0.10f)
                    )
                    drawLine(
                        color = teal,
                        start = Offset(cx + w * 0.01f, cy + h * 0.10f),
                        end = Offset(cx + w * 0.01f, cy - h * 0.16f),
                        strokeWidth = w * 0.05f,
                        cap = StrokeCap.Round
                    )
                    drawRoundRect(
                        color = teal,
                        topLeft = Offset(cx + w * 0.01f, cy - h * 0.18f),
                        size = Size(w * 0.15f, h * 0.07f),
                        cornerRadius = CornerRadius(w * 0.02f)
                    )

                    val flyFraction = flyAnim.value
                    val startX = cx + w * 0.90f
                    val startY = cy - h * 0.90f
                    val targetX = cx + w * 0.12f
                    val targetY = cy + h * 0.05f

                    val currentX = startX + (targetX - startX) * flyFraction
                    val currentY = startY + (targetY - startY) * flyFraction
                    val ballRadius = (w * 0.055f) * (0.3f + 0.7f * flyFraction.coerceIn(0f, 1f))

                    drawCircle(
                        color = pink,
                        radius = ballRadius,
                        center = Offset(currentX, currentY)
                    )
                }
            }

            AppIconOption.MINIMAL -> {
                val transition = rememberInfiniteTransition(label = "minimalSmoothHop")
                val hopOffset by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = -12f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(750, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "noteHop"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    val cx = w / 2f
                    val cy = h / 2f

                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF2E1948), Color(0xFF1B0E2E))
                        )
                    )

                    val cookiePath = buildSmoothCookiePath(
                        cx = cx,
                        cy = cy,
                        baseRadius = w * 0.38f,
                        amplitude = w * 0.045f,
                        lobes = 12
                    )
                    drawPath(
                        path = cookiePath,
                        color = Color(0xFFCDB4FF)
                    )

                    val noteColor = Color(0xFF1C1124)
                    val noteYShift = (hopOffset / 108f) * h

                    val noteScale = w / 108f
                    val p = Path().apply {
                        moveTo(49.0f * noteScale, (43.0f * noteScale) + noteYShift)
                        lineTo(63.5f * noteScale, (39.8f * noteScale) + noteYShift)
                        lineTo(63.5f * noteScale, (58.2f * noteScale) + noteYShift)
                        addOval(
                            androidx.compose.ui.geometry.Rect(
                                left = (53.9f * noteScale),
                                top = ((53.4f * noteScale) + noteYShift),
                                right = (63.5f * noteScale),
                                bottom = ((63.0f * noteScale) + noteYShift)
                            )
                        )
                        moveTo(51.8f * noteScale, (47.2f * noteScale) + noteYShift)
                        lineTo(51.8f * noteScale, (60.5f * noteScale) + noteYShift)
                        addOval(
                            androidx.compose.ui.geometry.Rect(
                                left = (42.2f * noteScale),
                                top = ((55.7f * noteScale) + noteYShift),
                                right = (51.8f * noteScale),
                                bottom = ((65.3f * noteScale) + noteYShift)
                            )
                        )
                    }
                    drawPath(
                        path = p,
                        color = noteColor
                    )
                }
            }
        }
    }
}

private fun DrawScope.buildSmoothCookiePath(
    cx: Float,
    cy: Float,
    baseRadius: Float,
    amplitude: Float,
    lobes: Int,
    steps: Int = 240
): Path {
    val path = Path()
    for (i in 0..steps) {
        val theta = (i.toFloat() / steps.toFloat()) * (2f * PI.toFloat())

        val wave = 0.88f * cos(lobes * theta) - 0.12f * cos(2f * lobes * theta)
        val r = baseRadius + amplitude * wave
        val x = cx + r * cos(theta)
        val y = cy + r * sin(theta)
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    return path
}
