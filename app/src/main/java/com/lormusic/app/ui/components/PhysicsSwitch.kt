package com.lormusic.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** The thumb follows the hand; the spring only takes over after release. */
@Composable
fun PhysicsSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier, enabled: Boolean = true,
    thumbContent: (@Composable () -> Unit)? = null,
    colors: SwitchColors = SwitchDefaults.colors()) {
    val callback by rememberUpdatedState(onCheckedChange)
    val currentChecked by rememberUpdatedState(checked)
    val travel = with(LocalDensity.current) { 20.dp.toPx() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var dragging by remember { mutableStateOf(false) }
    var finger by remember { mutableFloatStateOf(if (checked) 1f else 0f) }
    val settled by animateFloatAsState(if (dragging) finger else if (checked) 1f else 0f,
        if (dragging) snap() else spring(dampingRatio = .78f, stiffness = 700f), label = "switchSettle")
    val stretch by animateFloatAsState(if (dragging) 1.16f else 1f,
        spring(dampingRatio = .7f, stiffness = 900f), label = "switchStretch")
    val position = if (dragging) finger else settled
    val track by animateColorAsState(if (position >= .5f) colors.checkedTrackColor else colors.uncheckedTrackColor, label = "switchTrack")
    Box(modifier.minimumInteractiveComponentSize()
        .then(if (onCheckedChange != null) Modifier.toggleable(checked, enabled, Role.Switch) { callback?.invoke(it) }
            .draggable(rememberDraggableState { delta ->
                finger = (finger + delta / travel).coerceIn(0f, 1f)
            }, Orientation.Horizontal, enabled = enabled, reverseDirection = rtl,
                onDragStarted = { finger = if (currentChecked) 1f else 0f; dragging = true },
                onDragStopped = { velocity ->
                    val next = if (kotlin.math.abs(velocity) > 350f) velocity > 0f else finger >= .5f
                    callback?.invoke(next)
                    dragging = false
                }) else Modifier)
        .size(52.dp, 32.dp).graphicsLayer { alpha = if (enabled) 1f else .38f }
        .clip(CircleShape).background(track), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.offset(x = 4.dp + 20.dp * position)
            .size(24.dp).graphicsLayer { scaleX = stretch; scaleY = 1f / stretch }
            .background(if (position >= .5f) colors.checkedThumbColor else colors.uncheckedThumbColor, CircleShape),
            contentAlignment = Alignment.Center) { thumbContent?.invoke() }
    }
}
