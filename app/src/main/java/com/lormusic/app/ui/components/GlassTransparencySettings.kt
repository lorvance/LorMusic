package com.lormusic.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lormusic.app.model.LocalStrings
import com.lormusic.app.model.betaText
import kotlin.math.roundToInt

@Composable
fun GlassTransparencySettings(percent: Int, onPercentChange: (Int) -> Unit,
    tabOrder: List<LorTab>, showLabels: Boolean, showSearchButton: Boolean = true) {
    val strings = LocalStrings.current
    val scheme = MaterialTheme.colorScheme
    val title = strings.betaText("glassTransparency")
    var showPreview by rememberSaveable { mutableStateOf(true) }
    Surface(shape = RoundedCornerShape(24.dp), color = scheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold)
                Surface(color = scheme.primary.copy(alpha = .20f), shape = RoundedCornerShape(6.dp)) {
                    Text("BETA", Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall, color = scheme.primary,
                        fontWeight = FontWeight.Bold)
                }
                Text("$percent%", style = MaterialTheme.typography.titleMedium, color = scheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            Text(strings.betaText("glassTransparencyDesc"), style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant)
            Slider(value = percent.toFloat(), onValueChange = { onPercentChange(it.roundToInt()) },
                valueRange = 0f..100f, steps = 99,
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = title
                    stateDescription = "$percent%"
                })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("0%", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                TextButton(onClick = { onPercentChange(65) }, enabled = percent != 65) {
                    Text(strings.betaText("glassDefault"))
                }
                Text("100%", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(strings.betaText("preview"), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { showPreview = !showPreview }) {
                    Icon(if (showPreview) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                    Spacer(Modifier.width(4.dp))
                    Text(strings.betaText(if (showPreview) "hide" else "show"))
                }
            }
            AnimatedVisibility(showPreview) {
                // Always demonstrate the glass itself, even if the active app
                // navigation currently uses Material You or ordinary blur.
                CompositionLocalProvider(LocalPureMaterialYou provides false,
                    LocalLiquidGlassTransparency provides percent) {
                    NavigationBarPreview(tabOrder = tabOrder, backgroundColor = scheme.surfaceContainerLow,
                        enableBlur = true, useLiquidGlass = true, showLabels = showLabels,
                        showSearchButton = showSearchButton)
                }
            }
        }
    }
}
