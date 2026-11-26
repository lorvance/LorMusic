package com.lormusic.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.lormusic.app.model.AppIconOption
import com.lormusic.app.model.AppStrings
import kotlin.math.abs

@Composable
internal fun AppIconCarousel(strings: AppStrings, selected: AppIconOption, apply: (AppIconOption) -> Unit) {
    val icons = listOf(AppIconOption.PULSE, AppIconOption.REGULAR, AppIconOption.OLD, AppIconOption.MINIMAL)
    val pager = rememberPagerState(initialPage = icons.indexOf(selected).coerceAtLeast(0)) { icons.size }
    val preview = icons[pager.currentPage]
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp), color = colors.surfaceContainerLow) {
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 60.dp)) {
                    Text(strings.appIconSectionTitle, style = MaterialTheme.typography.titleMedium)
                    Text(strings.appIconSectionDesc, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Spacer(Modifier.height(20.dp))
                BoxWithConstraints {
                    val iconSize = minOf(maxWidth * .52f, 156.dp)
                    HorizontalPager(pager, contentPadding = PaddingValues(horizontal = (maxWidth - iconSize) / 2),
                        pageSpacing = 12.dp, modifier = Modifier.fillMaxWidth()) { page ->
                        val distance = abs(pager.currentPage - page + pager.currentPageOffsetFraction).coerceIn(0f, 1f)
                        Surface(shape = RoundedCornerShape(24.dp), border = if (page == pager.currentPage) BorderStroke(2.dp, colors.primary.copy(alpha = .65f)) else null,
                            modifier = Modifier.graphicsLayer {
                                alpha = 1f - distance * .55f
                                scaleX = 1f - distance * .22f
                                scaleY = scaleX
                            }) {
                            AppIconVectorPreview(icons[page], size = iconSize, cornerRadius = 24.dp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(preview.getDisplayName(strings), style = MaterialTheme.typography.titleMedium)
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    icons.indices.forEach { page ->
                        Box(Modifier.size(7.dp).background(if (page == pager.currentPage) colors.primary else colors.outlineVariant, CircleShape))
                    }
                }
            }
            if (preview != selected && !pager.isScrollInProgress) {
                FilledIconButton(onClick = { apply(preview) }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Icon(Icons.Filled.Check, "Применить иконку")
                }
            }
        }
    }
}
