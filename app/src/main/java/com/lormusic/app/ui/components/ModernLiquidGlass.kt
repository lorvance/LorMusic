package com.lormusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import com.kyant.backdrop.catalog.components.LiquidSlider
import io.github.ntvien.liquidglass.*
import io.github.ntvien.liquidglass.component.LiquidSurface

val LocalModernBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Composable
fun ModernGlassProviders(sourceKey: Any? = null, content: @Composable () -> Unit) {
    val c = MaterialTheme.colorScheme
    val drawBackground: androidx.compose.ui.graphics.drawscope.ContentDrawScope.() -> Unit = remember(c.background) {
        { drawRect(c.background); drawContent() }
    }
    val backdrop = key(sourceKey) { rememberLayerBackdrop(onDraw = drawBackground) }
    val colors =
        (if (c.background.luminance() > .5f) GlassColors.light() else GlassColors.dark()).copy(
            accent = c.primary,
            accentSoft = c.primary.copy(alpha = .2f),
            onAccent = c.onPrimary,
        )
    val pageBackdrop = remember(backdrop) { NtBackdropAdapter(backdrop) }
    val backdrops = remember(pageBackdrop) { GlassBackdrops(pageBackdrop, pageBackdrop, samplesContent = true) }
    val top = remember { GlassSceneTop() }
    LiquidGlassTheme(colors = colors, look = GlassLook.Rich, overlaySamplesContent = true) {
        CompositionLocalProvider(
            LocalGlassQuality provides (if (isLiquidGlassSupported()) GlassQuality.Full else GlassQuality.BlurOnly),
            LocalModernBackdrop provides backdrop,
            LocalGlassBackdrops provides backdrops,
            LocalGlassSceneTop provides top,
        ) {
            Box(Modifier.fillMaxSize()) {
                content()
                top.entries.forEach { (entryKey, entry) -> key(entryKey) { entry() } }
            }
        }
    }
}

/** Both shader engines read the same live capture, including its ownership/recording state. */
internal class NtBackdropAdapter(private val source: Backdrop) : io.github.ntvien.liquidglass.internal.backdrop.Backdrop {
    override val isBeingRecorded get() = source.isBeingRecorded
    override val isCoordinatesDependent get() = source.isCoordinatesDependent
    override fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBackdrop(
        density: androidx.compose.ui.unit.Density,
        coordinates: androidx.compose.ui.layout.LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) { with(source) { drawBackdrop(density, coordinates, layerBlock) } }
}

/** Embedded controls refract their own live track, rather than recursively sampling themselves in the page. */
@Composable
internal fun rememberControlBackdrop(): Backdrop {
    val color = MaterialTheme.colorScheme.surface
    return com.kyant.backdrop.backdrops.rememberCanvasBackdrop { drawRect(color) }
}

@Composable
fun <T> ModernGlassNavigation(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    icon: (T) -> ImageVector,
    showSearch: Boolean,
    searchActive: Boolean,
    onSearch: () -> Unit,
    showLabels: Boolean,
    applyInsets: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val backdrop = LocalModernBackdrop.current ?: return
    val latest by rememberUpdatedState(onSelect)
    var lastIndex by remember(items) { mutableIntStateOf(items.indexOf(selected).coerceAtLeast(0)) }
    val selectedIndex = items.indexOf(selected)
    LaunchedEffect(selectedIndex) { if (selectedIndex >= 0) lastIndex = selectedIndex }
    val index = if (selectedIndex >= 0) selectedIndex else lastIndex
    Row(
        modifier
            .fillMaxWidth()
            .then(if (applyInsets) Modifier.navigationBarsPadding() else Modifier)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LiquidBottomTabs(
            selectedTabIndex = { index },
            onTabSelected = { latest(items[it.coerceIn(items.indices)]) },
            backdrop = backdrop,
            tabsCount = items.size,
            modifier = Modifier.weight(1f).height(64.dp),
        ) {
            items.forEach { item ->
                LiquidBottomTab(
                    onClick = { latest(item) },
                    modifier =
                        Modifier.semantics {
                            this.selected = item == selected
                            contentDescription = label(item)
                        },
                ) {
                    Icon(
                        icon(item),
                        label(item),
                        Modifier.size(25.dp),
                        tint =
                            if (item == selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (showLabels)
                        Text(label(item), fontSize = 10.sp, maxLines = 1)
                }
            }
        }
        if (showSearch)
            LiquidSurface(
                modifier =
                    Modifier.size(64.dp).semantics {
                        contentDescription = "Search"
                        this.selected = searchActive
                    },
                style = headerGlassStyle().copy(surface = MaterialTheme.colorScheme.surface.let { it.copy(alpha = if (it.luminance() > .5f) .42f else .09f) }),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                onClick = onSearch,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Search,
                        null,
                        tint =
                            if (searchActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
    }
}

@Composable
fun AdaptiveGlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
) {
    val backdrop = rememberControlBackdrop()
    val safeRange = com.lormusic.app.data.GlassControlValues.range(valueRange)
    val boundedValue = com.lormusic.app.data.GlassControlValues.value(value, safeRange, steps)
    val current by rememberUpdatedState(boundedValue)
    val change by rememberUpdatedState(onValueChange)
    val finish by rememberUpdatedState(onValueChangeFinished)
    if (
        LocalLiquidGlassEnabled.current && isLiquidGlassSupported() && enabled && valueRange.endInclusive > valueRange.start && backdrop != null
    ) {
        LiquidSlider(
            value = { current },
            onValueChange = {
                change(com.lormusic.app.data.GlassControlValues.value(it, safeRange, steps))
            },
            valueRange = safeRange,
            visibilityThreshold =
                ((safeRange.endInclusive - safeRange.start) / 10000f).coerceAtLeast(.001f),
            backdrop = backdrop,
            modifier =
                modifier.heightIn(min = 48.dp).semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(boundedValue, safeRange, steps)
                    setProgress {
                        change(com.lormusic.app.data.GlassControlValues.value(it, safeRange, steps))
                        finish?.invoke()
                        true
                    }
                },
            onValueChangeFinished = { finish?.invoke() },
        )
    } else
        androidx.compose.material3.Slider(
            boundedValue,
            onValueChange,
            modifier,
            enabled && valueRange.endInclusive > valueRange.start,
            safeRange,
            steps.coerceAtLeast(0),
            onValueChangeFinished,
            colors,
        )
}
