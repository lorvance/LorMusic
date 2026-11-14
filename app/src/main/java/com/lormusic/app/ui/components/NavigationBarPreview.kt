package com.lormusic.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.lormusic.app.ui.components.backdrop.backdrops.layerBackdrop
import com.lormusic.app.ui.components.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun buildExpressiveFlowerPath(
    cx: Float,
    cy: Float,
    baseRadius: Float,
    lobes: Int,
    waveDepth: Float,
    phaseRad: Float = 0f
): Path = Path().apply {
    val steps = 120
    for (i in 0..steps) {
        val t = (i.toFloat() / steps) * 2f * PI.toFloat()
        val r = baseRadius * (1f + waveDepth * sin(lobes * t + phaseRad))
        val x = cx + r * cos(t)
        val y = cy + r * sin(t)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@Composable
fun NavigationBarPreview(
    useExpressive: Boolean = false,
    tabOrder: List<LorTab> = DEFAULT_LOR_TAB_ORDER,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    enableBlur: Boolean,
    useLiquidGlass: Boolean,
    showLabels: Boolean,
    showSearchButton: Boolean
) {
    val haze = remember { HazeState() }
    val scheme = MaterialTheme.colorScheme
    val bg = backgroundColor
    val drawBg: ContentDrawScope.() -> Unit = remember(bg) {
        {
            drawRect(bg)
            drawContent()
        }
    }
    val backdrop = rememberLayerBackdrop(onDraw = drawBg)
    var tab by remember { mutableStateOf(LorTab.LIBRARY) }
    var searching by remember { mutableStateOf(false) }

    val shapeColor1 = remember(scheme.primary) {
        if (scheme.primary.luminance() > 0.82f) Color(0xFF6489D8) else scheme.primary
    }
    val shapeColor2 = remember(scheme.tertiary, shapeColor1) {
        val raw = if (scheme.tertiary.luminance() > 0.82f) Color(0xFFA18BD4) else scheme.tertiary
        lerp(raw, Color(0xFFC47FA3), 0.35f)
    }
    val shapeColor3 = remember(scheme.secondary, shapeColor1) {
        val raw = if (scheme.secondary.luminance() > 0.82f) Color(0xFF5D9EAA) else scheme.secondary
        lerp(raw, Color(0xFF719B83), 0.35f)
    }

    CompositionLocalProvider(
        LocalAppBackdrop provides backdrop,
        LocalPreviewBackdrop provides backdrop,
        LocalLiquidGlassEnabled provides (useLiquidGlass && isLiquidGlassSupported()),
        LocalGlassExport provides null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .hazeSource(haze)
                    .layerBackdrop(backdrop)
            ) {
                drawRect(bg)
                val w = size.width
                val h = size.height

                val cookieLeft = buildExpressiveFlowerPath(
                    cx = w * 0.21f,
                    cy = h * 0.24f,
                    baseRadius = 38.dp.toPx(),
                    lobes = 12,
                    waveDepth = 0.11f,
                    phaseRad = 0.3f
                )
                drawPath(cookieLeft, shapeColor1.copy(alpha = 0.88f))

                val cloverCenter = buildExpressiveFlowerPath(
                    cx = w * 0.52f,
                    cy = h * 0.78f,
                    baseRadius = 42.dp.toPx(),
                    lobes = 8,
                    waveDepth = 0.15f,
                    phaseRad = 0.8f
                )
                drawPath(cloverCenter, shapeColor2.copy(alpha = 0.84f))

                val starRight = buildExpressiveFlowerPath(
                    cx = w * 0.82f,
                    cy = h * 0.26f,
                    baseRadius = 35.dp.toPx(),
                    lobes = 6,
                    waveDepth = 0.16f,
                    phaseRad = 1.2f
                )
                drawPath(starRight, shapeColor3.copy(alpha = 0.84f))
            }

            LorBottomNavBar(
                useExpressive = useExpressive,
                tabOrder = tabOrder,
                selected = tab,
                onSelect = { tab = it },
                hazeState = haze,
                enableBlur = enableBlur,
                useLiquidGlass = useLiquidGlass,
                showLabels = showLabels,
                showBottomSearchButton = showSearchButton,
                isLocalSearchActive = searching,
                onToggleLocalSearch = { searching = !searching },
                applyNavigationInsets = false
            )
        }
    }
}
