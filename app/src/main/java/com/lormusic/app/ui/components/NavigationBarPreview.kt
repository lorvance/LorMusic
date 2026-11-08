package com.lormusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lormusic.app.ui.components.backdrop.backdrops.layerBackdrop
import com.lormusic.app.ui.components.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/** Its glass only samples the sibling background, never the settings screen containing it. */
@Composable
fun NavigationBarPreview(
    enableBlur: Boolean,
    useLiquidGlass: Boolean,
    showLabels: Boolean,
    showSearchButton: Boolean
) {
    val haze = remember { HazeState() }
    val backdrop = rememberLayerBackdrop()
    var tab by remember { mutableStateOf(LorTab.LIBRARY) }
    var searching by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalAppBackdrop provides backdrop, LocalGlassExport provides null) {
        Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
            // Leave room around the real component so its shadow and circular search button aren't clipped.
            Box(Modifier.matchParentSize().hazeSource(haze).layerBackdrop(backdrop)
                .background(MaterialTheme.colorScheme.background)) {
                Box(Modifier.align(Alignment.CenterStart).padding(start = 22.dp).size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape))
            }
            LorBottomNavBar(
                selected = tab, onSelect = { tab = it }, hazeState = haze,
                enableBlur = enableBlur, useLiquidGlass = useLiquidGlass,
                showLabels = showLabels, showBottomSearchButton = showSearchButton,
                isLocalSearchActive = searching, onToggleLocalSearch = { searching = !searching },
                applyNavigationInsets = false
            )
        }
    }
}
