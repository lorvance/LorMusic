package com.lormusic.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lormusic.app.model.LocalStrings
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

val LocalMaterialArrows = staticCompositionLocalOf { false }

@Composable
fun LegacyScreenHeader(title: String, onBack: () -> Unit, onAction: () -> Unit,
    hazeState: HazeState?, enableBlur: Boolean, useLiquidGlass: Boolean,
    actionIcon: ImageVector = Icons.Filled.MoreVert, actionLabel: String = LocalStrings.current.actions,
    applyStatusInsets: Boolean = true) {
    val topScrimColor = MaterialTheme.colorScheme.background
    val pureMy = LocalPureMaterialYou.current
    Box(Modifier.fillMaxWidth()) {
        if (enableBlur && !pureMy && hazeState != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .hazeEffect(state = hazeState) {
                        blurEnabled = true
                        backgroundColor = topScrimColor
                        blurRadius = 42.dp
                        tints = listOf(HazeTint(topScrimColor.copy(alpha = 0.65f)))
                    }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                topScrimColor.copy(alpha = 0.80f),
                                topScrimColor.copy(alpha = 0.42f),
                                Color.Transparent
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                topScrimColor.copy(alpha = 0.96f),
                                topScrimColor.copy(alpha = 0.78f),
                                topScrimColor.copy(alpha = 0.42f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (applyStatusInsets) Modifier.statusBarsPadding() else Modifier)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .liquidGlassBubble(CircleShape, hazeState, enableBlur, useLiquidGlass)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                IosBackChevron(color = MaterialTheme.colorScheme.onSurface)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .height(44.dp)
                        .widthIn(max = 260.dp)
                        .animateContentSize(animationSpec = tween(220))
                        .clip(RoundedCornerShape(22.dp))
                        .liquidGlassBubble(RoundedCornerShape(22.dp), hazeState, enableBlur, useLiquidGlass)
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = title,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)).togetherWith(fadeOut(animationSpec = tween(150)))
                        },
                        label = "legacyHeaderTitleAnim"
                    ) { titleText ->
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Box(
                Modifier
                    .size(44.dp)
                    .liquidGlassBubble(CircleShape, hazeState, enableBlur, useLiquidGlass)
                    .clickable(onClick = onAction),
                contentAlignment = Alignment.Center
            ) {
                Icon(actionIcon, actionLabel, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
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
