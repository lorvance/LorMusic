package com.lormusic.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/** The reveal pivots around the button that owns the popup, on either side. */
@Composable
fun AnchoredBubbleMenu(expanded: Boolean, onDismissRequest: () -> Unit,
    shape: Shape = RoundedCornerShape(24.dp), content: @Composable ColumnScope.() -> Unit) {
    val visible = remember { MutableTransitionState(false) }
    visible.targetState = expanded
    val pivot = remember { mutableStateOf(TransformOrigin(.5f, 0f)) }
    val gap = with(LocalDensity.current) { 8.dp.roundToPx() }
    val provider = remember(gap) {
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize,
                layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                val rightAligned = anchorBounds.center.x > windowSize.width / 2
                val candidateX = if (rightAligned) anchorBounds.right - popupContentSize.width else anchorBounds.left
                val x = candidateX.coerceIn(gap, (windowSize.width - popupContentSize.width - gap).coerceAtLeast(gap))
                val below = anchorBounds.bottom + gap
                val y = (if (below + popupContentSize.height <= windowSize.height - gap) below
                    else anchorBounds.top - popupContentSize.height - gap)
                    .coerceIn(gap, (windowSize.height - popupContentSize.height - gap).coerceAtLeast(gap))
                pivot.value = TransformOrigin(
                    ((anchorBounds.center.x - x).toFloat() / popupContentSize.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                    ((anchorBounds.center.y - y).toFloat() / popupContentSize.height.coerceAtLeast(1)).coerceIn(0f, 1f))
                return IntOffset(x, y)
            }
        }
    }
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * .72f
    if (visible.currentState || visible.targetState) {
        Popup(popupPositionProvider = provider, onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)) {
            AnimatedVisibility(visibleState = visible,
                enter = fadeIn(tween(120)) + scaleIn(spring(dampingRatio = .86f, stiffness = 580f),
                    initialScale = .14f, transformOrigin = pivot.value),
                exit = fadeOut(tween(110)) + scaleOut(tween(150), targetScale = .72f, transformOrigin = pivot.value)) {
                Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp, shadowElevation = 14.dp,
                    modifier = Modifier.widthIn(min = 200.dp, max = 300.dp)) {
                    Column(Modifier.width(IntrinsicSize.Max).heightIn(max = maxHeight)
                        .verticalScroll(rememberScrollState()).padding(vertical = 6.dp), content = content)
                }
            }
        }
    }
}
