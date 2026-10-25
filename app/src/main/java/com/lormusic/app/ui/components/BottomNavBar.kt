package com.lormusic.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

enum class LorTab(val route: String, val label: String, val filled: ImageVector, val outline: ImageVector) {
    LIBRARY("library", "Tracks", Icons.Filled.MusicNote, Icons.Outlined.MusicNote),
    PLAYLISTS("playlists", "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings);

    fun getDisplayName(strings: com.lormusic.app.model.AppStrings): String {
        return when (this) {
            LIBRARY -> strings.navLibrary
            PLAYLISTS -> strings.navPlaylists
            SETTINGS -> strings.navSettings
        }
    }
}

enum class YtMusicTab(val label: String, val filled: ImageVector, val outline: ImageVector) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    LIBRARIES("Libraries", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    MY_WAVE("My Wave", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
    SEARCH("", Icons.Filled.Search, Icons.Outlined.Search);

    fun getDisplayName(strings: com.lormusic.app.model.AppStrings): String {
        return when (this) {
            HOME -> strings.ytHome
            LIBRARIES -> strings.ytLibraries
            MY_WAVE -> strings.ytMyWave
            SEARCH -> strings.searchTitle
        }
    }
}

@Composable
fun LorBottomNavBar(
    selected: LorTab,
    onSelect: (LorTab) -> Unit,
    hazeState: HazeState,
    enableBlur: Boolean = true,
    useLiquidGlass: Boolean = false,
    showBottomSearchButton: Boolean = false,
    isLocalSearchActive: Boolean = false,
    showLabels: Boolean = false,
    onToggleLocalSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val strings = com.lormusic.app.model.LocalStrings.current
    val density = LocalDensity.current
    val navBarHeight = 58.dp
    val innerInset = 4.dp
    val capsuleShape = CircleShape
    val baseSurface = MaterialTheme.colorScheme.surface
    val isPureMy = LocalPureMaterialYou.current
    val tabs = LorTab.entries

    var innerWidthPx by remember { mutableIntStateOf(0) }
    var isDraggingNav by remember { mutableStateOf(false) }
    var dragFingerX by remember { mutableFloatStateOf(0f) }
    var hoveredIndex by remember { mutableIntStateOf(-1) }

    val selectedIndex = tabs.indexOf(selected).coerceAtLeast(0)
    val activeIndex = if (isDraggingNav && hoveredIndex in tabs.indices) hoveredIndex else selectedIndex

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(navBarHeight)
                .shadow(
                    elevation = 20.dp,
                    shape = capsuleShape,
                    ambientColor = Color.Black.copy(alpha = 0.6f),
                    spotColor = Color.Black.copy(alpha = 0.6f)
                )
                .clip(capsuleShape)
                .then(
                    if (isPureMy) {
                        Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainer, capsuleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), capsuleShape)
                    } else if (useLiquidGlass && isLiquidGlassSupported()) {
                        Modifier.liquidGlass(capsuleShape)
                    } else {
                        val baseMod = if (enableBlur) {
                            Modifier.hazeEffect(state = hazeState) {
                                blurEnabled = true
                                backgroundColor = baseSurface
                                blurRadius = 30.dp
                                tints = listOf(HazeTint(baseSurface.copy(alpha = 0.52f)))
                            }
                        } else {
                            Modifier.background(baseSurface.copy(alpha = 0.96f))
                        }
                        baseMod.border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            ),
                            shape = capsuleShape
                        )
                    }
                )
                .padding(innerInset)
                .onSizeChanged { innerWidthPx = it.width }
                .pointerInput(tabs, innerWidthPx) {
                    if (innerWidthPx <= 0) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val slotWidth = (innerWidthPx.toFloat() / tabs.size).coerceAtLeast(1f)
                        var currentIdx = (down.position.x / slotWidth).toInt().coerceIn(0, tabs.lastIndex)
                        dragFingerX = down.position.x.coerceIn(0f, innerWidthPx.toFloat())
                        hoveredIndex = currentIdx
                        var movedSignificantly = false
                        val startX = down.position.x

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                            if (change == null || !change.pressed) {
                                val finalIdx = hoveredIndex.coerceIn(0, tabs.lastIndex)
                                isDraggingNav = false
                                hoveredIndex = -1
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                onSelect(tabs[finalIdx])
                                break
                            }
                            val x = change.position.x.coerceIn(0f, innerWidthPx.toFloat())
                            dragFingerX = x
                            if (kotlin.math.abs(x - startX) > 8f) {
                                if (!movedSignificantly) {
                                    movedSignificantly = true
                                    isDraggingNav = true
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                change.consume()
                            } else if (!isDraggingNav && (change.uptimeMillis - down.uptimeMillis) > 160L) {
                                isDraggingNav = true
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                            val newIdx = (x / slotWidth).toInt().coerceIn(0, tabs.lastIndex)
                            if (newIdx != currentIdx) {
                                currentIdx = newIdx
                                hoveredIndex = newIdx
                                isDraggingNav = true
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                        }
                    }
                }
        ) {

            if (innerWidthPx > 0) {
                val slotWidthPx = innerWidthPx.toFloat() / tabs.size
                val targetLeftPx = if (isDraggingNav) {
                    (dragFingerX - slotWidthPx * 0.5f).coerceIn(0f, innerWidthPx - slotWidthPx)
                } else {
                    activeIndex * slotWidthPx
                }
                val pillWidthDp = with(density) { slotWidthPx.toDp() }
                val targetOffsetX = with(density) { targetLeftPx.toDp() }
                val animatedOffsetX by animateDpAsState(
                    targetValue = targetOffsetX,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = if (isDraggingNav) Spring.StiffnessHigh else Spring.StiffnessMediumLow
                    ),
                    label = "slidingPillOffset"
                )
                val pillScale by animateFloatAsState(
                    targetValue = if (isDraggingNav) 1.03f else 1.0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "slidingPillScale"
                )

                Box(
                    modifier = Modifier
                        .offset(x = animatedOffsetX)
                        .width(pillWidthDp)
                        .fillMaxHeight()
                        .scale(pillScale)
                        .clip(CircleShape)
                        .background(
                            if (isDraggingNav) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            }
                        )
                        .then(
                            if (isDraggingNav) {
                                Modifier.border(
                                    width = 1.2.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                    shape = CircleShape
                                )
                            } else Modifier
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { idx, tab ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        LorTabItem(
                            label = tab.getDisplayName(strings),
                            filledIcon = tab.filled,
                            outlineIcon = tab.outline,
                            isSelected = idx == activeIndex,
                            isHoveredByGesture = isDraggingNav && idx == activeIndex,
                            isYtBadge = false,
                            compact = showBottomSearchButton,
                            showLabels = showLabels
                        )
                    }
                }
            }
        }

        if (showBottomSearchButton) {
            val searchBg by animateColorAsState(
                targetValue = if (isLocalSearchActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "localSearchBtnBg"
            )
            Box(
                modifier = Modifier
                    .size(navBarHeight)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.6f),
                        spotColor = Color.Black.copy(alpha = 0.6f)
                    )
                    .clip(CircleShape)
                    .then(
                        if (isPureMy) {
                            Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), CircleShape)
                        } else if (useLiquidGlass && isLiquidGlassSupported()) {
                            Modifier.liquidGlass(CircleShape)
                        } else {
                            val baseMod = if (enableBlur) {
                                Modifier.hazeEffect(state = hazeState) {
                                    blurEnabled = true
                                    backgroundColor = baseSurface
                                    blurRadius = 30.dp
                                    tints = listOf(HazeTint(baseSurface.copy(alpha = 0.54f)))
                                }
                            } else {
                                Modifier.background(baseSurface.copy(alpha = 0.96f))
                            }
                            baseMod.border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.24f),
                                        Color.White.copy(alpha = 0.06f)
                                    )
                                ),
                                shape = CircleShape
                            )
                        }
                    )
                    .background(searchBg.copy(alpha = if (isLocalSearchActive) 0.88f else 0f))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.24f),
                                Color.White.copy(alpha = 0.06f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onToggleLocalSearch()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = strings.searchInLibrary,
                    tint = if (isLocalSearchActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
fun YtMusicBottomNavBar(
    selected: YtMusicTab,
    onSelect: (YtMusicTab) -> Unit,
    hazeState: HazeState,
    enableBlur: Boolean = true,
    useLiquidGlass: Boolean = false,
    showBottomSearchButton: Boolean = true,
    showLabels: Boolean = false,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val strings = com.lormusic.app.model.LocalStrings.current
    val density = LocalDensity.current
    val navBarHeight = 58.dp
    val innerInset = 4.dp
    val capsuleShape = CircleShape
    val baseSurface = MaterialTheme.colorScheme.surface
    val isPureMy = LocalPureMaterialYou.current
    val mainTabs = remember {
        listOf(
            YtMusicTab.HOME,
            YtMusicTab.LIBRARIES,
            YtMusicTab.MY_WAVE
        )
    }

    var innerWidthPx by remember { mutableIntStateOf(0) }
    var isDraggingNav by remember { mutableStateOf(false) }
    var dragFingerX by remember { mutableFloatStateOf(0f) }
    var hoveredIndex by remember { mutableIntStateOf(-1) }

    val selectedIndex = mainTabs.indexOf(selected)
    val activeIndex = if (isDraggingNav && hoveredIndex in mainTabs.indices) hoveredIndex else selectedIndex

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(navBarHeight)
                .shadow(
                    elevation = 20.dp,
                    shape = capsuleShape,
                    ambientColor = Color.Black.copy(alpha = 0.6f),
                    spotColor = Color.Black.copy(alpha = 0.6f)
                )
                .clip(capsuleShape)
                .then(
                    if (isPureMy) {
                        Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainer, capsuleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), capsuleShape)
                    } else if (useLiquidGlass && isLiquidGlassSupported()) {
                        Modifier.liquidGlass(capsuleShape)
                    } else {
                        val baseMod = if (enableBlur) {
                            Modifier.hazeEffect(state = hazeState) {
                                blurEnabled = true
                                backgroundColor = baseSurface
                                blurRadius = 30.dp
                                tints = listOf(HazeTint(baseSurface.copy(alpha = 0.52f)))
                            }
                        } else {
                            Modifier.background(baseSurface.copy(alpha = 0.96f))
                        }
                        baseMod.border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            ),
                            shape = capsuleShape
                        )
                    }
                )
                .padding(innerInset)
                .onSizeChanged { innerWidthPx = it.width }
                .pointerInput(mainTabs, innerWidthPx) {
                    if (innerWidthPx <= 0) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val slotWidth = (innerWidthPx.toFloat() / mainTabs.size).coerceAtLeast(1f)
                        var currentIdx = (down.position.x / slotWidth).toInt().coerceIn(0, mainTabs.lastIndex)
                        dragFingerX = down.position.x.coerceIn(0f, innerWidthPx.toFloat())
                        hoveredIndex = currentIdx
                        var movedSignificantly = false
                        val startX = down.position.x

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                            if (change == null || !change.pressed) {
                                val finalIdx = hoveredIndex.coerceIn(0, mainTabs.lastIndex)
                                isDraggingNav = false
                                hoveredIndex = -1
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                onSelect(mainTabs[finalIdx])
                                break
                            }
                            val x = change.position.x.coerceIn(0f, innerWidthPx.toFloat())
                            dragFingerX = x
                            if (kotlin.math.abs(x - startX) > 8f) {
                                if (!movedSignificantly) {
                                    movedSignificantly = true
                                    isDraggingNav = true
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                change.consume()
                            } else if (!isDraggingNav && (change.uptimeMillis - down.uptimeMillis) > 160L) {
                                isDraggingNav = true
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                            val newIdx = (x / slotWidth).toInt().coerceIn(0, mainTabs.lastIndex)
                            if (newIdx != currentIdx) {
                                currentIdx = newIdx
                                hoveredIndex = newIdx
                                isDraggingNav = true
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                        }
                    }
                }
        ) {
            if (innerWidthPx > 0 && activeIndex in mainTabs.indices) {
                val slotWidthPx = innerWidthPx.toFloat() / mainTabs.size
                val targetLeftPx = if (isDraggingNav) {
                    (dragFingerX - slotWidthPx * 0.5f).coerceIn(0f, innerWidthPx - slotWidthPx)
                } else {
                    activeIndex * slotWidthPx
                }
                val pillWidthDp = with(density) { slotWidthPx.toDp() }
                val targetOffsetX = with(density) { targetLeftPx.toDp() }
                val animatedOffsetX by animateDpAsState(
                    targetValue = targetOffsetX,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = if (isDraggingNav) Spring.StiffnessHigh else Spring.StiffnessMediumLow
                    ),
                    label = "ytSlidingPillOffset"
                )
                val pillScale by animateFloatAsState(
                    targetValue = if (isDraggingNav) 1.03f else 1.0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "ytSlidingPillScale"
                )

                Box(
                    modifier = Modifier
                        .offset(x = animatedOffsetX)
                        .width(pillWidthDp)
                        .fillMaxHeight()
                        .scale(pillScale)
                        .clip(CircleShape)
                        .background(
                            if (isDraggingNav) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            }
                        )
                        .then(
                            if (isDraggingNav) {
                                Modifier.border(
                                    width = 1.2.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                    shape = CircleShape
                                )
                            } else Modifier
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                mainTabs.forEachIndexed { idx, tab ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        LorTabItem(
                            label = tab.getDisplayName(strings),
                            filledIcon = tab.filled,
                            outlineIcon = tab.outline,
                            isSelected = idx == activeIndex,
                            isHoveredByGesture = isDraggingNav && idx == activeIndex,
                            isYtBadge = false,
                            compact = true,
                            showLabels = showLabels
                        )
                    }
                }
            }
        }

        if (showBottomSearchButton) {
            val searchSelected = selected == YtMusicTab.SEARCH
            val searchBg by animateColorAsState(
                targetValue = if (searchSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "searchBtnBg"
            )
            Box(
                modifier = Modifier
                    .size(navBarHeight)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.6f),
                        spotColor = Color.Black.copy(alpha = 0.6f)
                    )
                    .clip(CircleShape)
                    .then(
                        if (isPureMy) {
                            Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), CircleShape)
                        } else if (useLiquidGlass && isLiquidGlassSupported()) {
                            Modifier.liquidGlass(CircleShape)
                        } else {
                            val baseMod = if (enableBlur) {
                                Modifier.hazeEffect(state = hazeState) {
                                    blurEnabled = true
                                    backgroundColor = baseSurface
                                    blurRadius = 30.dp
                                    tints = listOf(HazeTint(baseSurface.copy(alpha = 0.54f)))
                                }
                            } else {
                                Modifier.background(baseSurface.copy(alpha = 0.96f))
                            }
                            baseMod.border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.24f),
                                        Color.White.copy(alpha = 0.06f)
                                    )
                                ),
                                shape = CircleShape
                            )
                        }
                    )
                    .background(searchBg.copy(alpha = if (searchSelected) 0.88f else 0f))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.24f),
                                Color.White.copy(alpha = 0.06f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onSelect(YtMusicTab.SEARCH)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = strings.searchInYtMusic,
                    tint = if (searchSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun LorTabItem(
    label: String,
    filledIcon: ImageVector,
    outlineIcon: ImageVector,
    isSelected: Boolean,
    isHoveredByGesture: Boolean = false,
    isYtBadge: Boolean = false,
    compact: Boolean = false,
    showLabels: Boolean = false
) {
    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onSurface
            isYtBadge -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f)
        },
        label = "contentColor"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            isHoveredByGesture -> 1.10f
            isSelected -> 1.03f
            else -> 1.0f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .scale(scale)
            .padding(horizontal = 2.dp)
    ) {
        Icon(
            imageVector = if (isSelected) filledIcon else outlineIcon,
            contentDescription = label,
            tint = if (isSelected || isYtBadge) MaterialTheme.colorScheme.primary else contentColor,
            modifier = Modifier.size(if (showLabels) 22.dp else 26.dp)
        )
        if (showLabels) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = TextStyle(
                    fontSize = if (compact) 10.5.sp else 11.sp,
                    fontWeight = if (isSelected || isYtBadge) FontWeight.Bold else FontWeight.Medium,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            )
        }
    }
}
