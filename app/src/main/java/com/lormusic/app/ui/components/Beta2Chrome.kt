package com.lormusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lormusic.app.model.LocalStrings
import dev.chrisbanes.haze.HazeState

val LocalMaterialArrows = staticCompositionLocalOf { false }

@Composable
fun LegacyScreenHeader(title: String, onBack: () -> Unit, onAction: () -> Unit,
    hazeState: HazeState?, enableBlur: Boolean, useLiquidGlass: Boolean,
    actionIcon: ImageVector = Icons.Filled.MoreVert, actionLabel: String = LocalStrings.current.actions,
    applyStatusInsets: Boolean = true) {
    Row(Modifier.fillMaxWidth().then(if (applyStatusInsets) Modifier.statusBarsPadding() else Modifier)
        .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).clip(CircleShape)
            .liquidGlassBubble(CircleShape, hazeState, enableBlur, useLiquidGlass).clickable(onClick = onBack),
            contentAlignment = Alignment.Center) { IosBackChevron() }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(Modifier.height(44.dp).widthIn(max = 250.dp).clip(CircleShape)
                .liquidGlassBubble(CircleShape, hazeState, enableBlur, useLiquidGlass).padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(Modifier.size(44.dp).clip(CircleShape)
            .liquidGlassBubble(CircleShape, hazeState, enableBlur, useLiquidGlass).clickable(onClick = onAction),
            contentAlignment = Alignment.Center) { Icon(actionIcon, actionLabel) }
    }
}

/** Compact iOS-style prompt: neutral sheet, separated equal-width text actions. */
@Composable
fun IosPromptDialog(title: String, confirmLabel: String, cancelLabel: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit, confirmEnabled: Boolean = true,
    content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth()) {
            Column {
                Column(Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState()).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(title, fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center)
                    content()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(0.dp)) {
                        Text(cancelLabel, fontSize = 16.sp)
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                    TextButton(onClick = onConfirm, enabled = confirmEnabled, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(0.dp)) {
                        Text(confirmLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
