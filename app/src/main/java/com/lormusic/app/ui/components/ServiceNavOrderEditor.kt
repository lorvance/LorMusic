package com.lormusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.*
import com.lormusic.app.model.LocalStrings
import com.lormusic.app.model.betaText

@Composable
fun <T> ServiceNavOrderEditor(
    title: String,
    order: List<T>,
    defaults: List<T>,
    label: (T) -> String,
    icon: (T) -> ImageVector,
    onChange: (List<T>) -> Unit,
) {
    val current by rememberUpdatedState(order)
    val update by rememberUpdatedState(onChange)
    var width by remember { mutableIntStateOf(0) }
    var moving by remember { mutableStateOf<T?>(null) }
    var finger by remember { mutableFloatStateOf(0f) }
    val strings = LocalStrings.current
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            if (order != defaults)
                TextButton({ onChange(defaults) }) { Text(strings.betaText("resetOrder")) }
        }
        Text(
            strings.betaText("navTabOrderDesc"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth()
                .height(94.dp)
                .onSizeChanged { width = it.width }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { pos ->
                            if (width > 0) {
                                val i =
                                    (pos.x / (width.toFloat() / current.size))
                                        .toInt()
                                        .coerceIn(current.indices)
                                moving = current[i]
                                finger = pos.x
                            }
                        },
                        onDragCancel = { moving = null },
                        onDragEnd = { moving = null },
                    ) { change, amount ->
                        change.consume()
                        finger = (finger + amount.x).coerceIn(0f, width.toFloat())
                        val item = moving
                        if (item != null && width > 0) {
                            val from = current.indexOf(item)
                            val to =
                                (finger / (width.toFloat() / current.size))
                                    .toInt()
                                    .coerceIn(current.indices)
                            if (from >= 0 && from != to) {
                                val list = current.toMutableList()
                                list.removeAt(from)
                                list.add(to, item)
                                update(list)
                            }
                        }
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            order.forEachIndexed { i, item ->
                Column(
                    Modifier.weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            if (moving == item) {
                                translationX = finger - (i + .5f) * width / order.size
                                scaleX = 1.06f
                                scaleY = 1.06f
                            }
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (moving == item) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("${i+1}", style = MaterialTheme.typography.labelSmall)
                    Icon(icon(item), label(item), tint = MaterialTheme.colorScheme.primary)
                    Text(label(item), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}
