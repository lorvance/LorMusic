package com.lormusic.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.lormusic.app.model.LocalStrings
import com.lormusic.app.ui.components.backdrop.drawPlainBackdrop
import com.lormusic.app.ui.components.backdrop.effects.blur
import dev.chrisbanes.haze.*

/** One blur surface and one moving pill, including the attached search slot. */
@Composable
fun <T> JoinedBlurNavigationBar(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    icon: (T) -> ImageVector,
    hazeState: HazeState,
    enableBlur: Boolean,
    showSearch: Boolean,
    searchActive: Boolean,
    onSearch: () -> Unit,
    showLabels: Boolean,
    applyInsets: Boolean,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = MaterialTheme.colorScheme
    val count = items.size + if (showSearch) 1 else 0
    val index = if (showSearch && searchActive) items.size else items.indexOf(selected)
    var width by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var finger by remember { mutableFloatStateOf(0f) }
    val latestSelect by rememberUpdatedState(onSelect)
    val latestSearch by rememberUpdatedState(onSearch)
    fun select(i: Int) {
        if (i in items.indices) latestSelect(items[i]) else if (showSearch) latestSearch()
    }
    val target =
        if (dragging && width > 0)
            (finger / (width.toFloat() / count) - .5f).coerceIn(0f, (count - 1).toFloat())
        else index.coerceAtLeast(0).toFloat()
    val springPosition by
        animateFloatAsState(
            target,
            spring(dampingRatio = .8f, stiffness = 480f),
            label = "joinedNavPosition",
        )
    val stretch by
        animateFloatAsState(if (dragging) 1.08f else 1f, spring(), label = "joinedNavStretch")
    val backdrop = LocalPreviewBackdrop.current
    val surface =
        if (LocalPureMaterialYou.current) Modifier.background(colors.surfaceContainer)
        else if (enableBlur && backdrop != null)
            Modifier.drawPlainBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = { blur(42.dp.toPx()) },
                onDrawSurface = { drawRect(colors.surface.copy(alpha = .54f)) },
            )
        else if (enableBlur)
            Modifier.hazeEffect(state = hazeState) {
                blurEnabled = true
                backgroundColor = colors.surface
                blurRadius = 46.dp
                tints = listOf(HazeTint(colors.surface.copy(alpha = .54f)))
            }
        else Modifier.background(colors.surface.copy(alpha = .96f))
    Box(
        modifier
            .fillMaxWidth()
            .then(if (applyInsets) Modifier.navigationBarsPadding() else Modifier)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Box(
            Modifier.fillMaxWidth()
                .height(58.dp)
                .clip(CircleShape)
                .then(surface)
                .border(1.dp, colors.outlineVariant.copy(alpha = .4f), CircleShape)
                .padding(4.dp)
        ) {
            Box(
                Modifier.fillMaxSize()
                    .onSizeChanged { width = it.width }
                    .pointerInput(items, count, width) {
                        if (width <= 0) return@pointerInput
                        detectDragGestures(
                            onDragStart = {
                                finger = it.x
                                dragging = true
                            },
                            onDragCancel = { dragging = false },
                            onDragEnd = {
                                val i =
                                    (finger / (width.toFloat() / count))
                                        .toInt()
                                        .coerceIn(0, count - 1)
                                dragging = false
                                select(i)
                            },
                        ) { change, amount ->
                            change.consume()
                            finger = (finger + amount.x).coerceIn(0f, width.toFloat())
                        }
                    }
            ) {
                if (index >= 0 || dragging)
                    Box(
                        Modifier.fillMaxHeight()
                            .fillMaxWidth(1f / count)
                            .graphicsLayer {
                                translationX = (width.toFloat() / count) * springPosition
                                scaleX = stretch
                            }
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = .24f))
                    )
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    repeat(count) { i ->
                        val active =
                            if (dragging)
                                (finger / (width.toFloat() / count).coerceAtLeast(1f))
                                    .toInt()
                                    .coerceIn(0, count - 1) == i
                            else index == i
                        Column(
                            Modifier.weight(1f).fillMaxHeight().clip(CircleShape).clickable {
                                select(i)
                            },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val text = if (i < items.size) label(items[i]) else strings.searchTitle
                            Icon(
                                if (i < items.size) icon(items[i]) else Icons.Default.Search,
                                text,
                                Modifier.size(if (showLabels) 22.dp else 26.dp),
                                tint =
                                    if (active) colors.primary
                                    else colors.onSurface.copy(alpha = .7f),
                            )
                            if (showLabels)
                                Text(
                                    text,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (active) colors.primary else colors.onSurfaceVariant,
                                )
                        }
                    }
                }
            }
        }
    }
}
