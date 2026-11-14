package com.lormusic.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lormusic.app.model.LocalStrings

@Composable
fun expressiveColors(): ColorScheme = MaterialTheme.colorScheme

@Composable
fun ExpressiveHeaderBackground(modifier: Modifier = Modifier) {
    val colors = expressiveColors()
    Canvas(modifier.background(colors.background)) {
        val w = size.width; val h = size.height
        val back = Path().apply {
            moveTo(w * .38f, 0f); lineTo(w, 0f); lineTo(w, h * .82f)
            cubicTo(w * .75f, h * .74f, w * .90f, h * .30f, w * .62f, h * .38f)
            cubicTo(w * .42f, h * .42f, w * .32f, h * .15f, w * .38f, 0f); close()
        }
        drawPath(back, colors.secondaryContainer.copy(alpha = .42f))
        val front = Path().apply {
            moveTo(w * .54f, 0f); lineTo(w, 0f); lineTo(w, h * .66f)
            cubicTo(w * .82f, h * .48f, w * .78f, h * .56f, w * .66f, h * .42f)
            cubicTo(w * .54f, h * .28f, w * .49f, h * .16f, w * .54f, 0f); close()
        }
        drawPath(front, colors.primaryContainer.copy(alpha = .62f))
    }
}

@Composable
fun ExpressiveScreenHeader(title: String, onBack: () -> Unit, onAction: () -> Unit,
    actionIcon: ImageVector = Icons.Filled.MoreVert, actionLabel: String = LocalStrings.current.actions,
    modifier: Modifier = Modifier, applyStatusInsets: Boolean = true) {
    val colors = expressiveColors()
    Box(modifier.fillMaxWidth()) {
        ExpressiveHeaderBackground(Modifier.matchParentSize())
        Column(Modifier.then(if (applyStatusInsets) Modifier.statusBarsPadding() else Modifier).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FilledTonalIconButton(onClick = onBack, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = colors.surfaceContainerHigh)) {
                    IosBackChevron(color = colors.onSurface)
                }
                FilledTonalIconButton(onClick = onAction, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer)) {
                    Icon(actionIcon, actionLabel)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(title, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold,
                color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** A focused field is never crossfaded over the title during a size transition. */
@Composable
fun ExpressiveSearchTitle(title: String, active: Boolean, query: String, onQuery: (String) -> Unit,
    onClose: () -> Unit, modifier: Modifier = Modifier, largeTitle: Boolean = true) {
    val colors = expressiveColors()
    val strings = LocalStrings.current
    val progress by animateFloatAsState(if (active) 1f else 0f, tween(280, easing = FastOutSlowInEasing), label = "headerSearchMorph")
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    Box(modifier.height(if (largeTitle) 52.dp else 44.dp), contentAlignment = Alignment.CenterStart) {
        if (active) {
            Box(Modifier.fillMaxWidth(progress.coerceAtLeast(.1f)).height(46.dp)
                .clip(RoundedCornerShape(28.dp)).background(colors.surfaceContainerHigh)
                .border(1.dp, colors.outlineVariant, RoundedCornerShape(28.dp)).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                if (progress > .9f) {
                    LaunchedEffect(Unit) { focus.requestFocus(); keyboard?.show() }
                    BasicTextField(value = query, onValueChange = onQuery, singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = colors.onSurface),
                        cursorBrush = SolidColor(colors.primary), decorationBox = { inner ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.width(48.dp))
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    if (query.isEmpty()) Text(strings.searchTitle, color = colors.onSurfaceVariant, maxLines = 1)
                                    inner()
                                }
                                IconButton(onClick = { if (query.isNotEmpty()) onQuery("") else { keyboard?.hide(); onClose() } }) {
                                    Icon(Icons.Filled.Close, strings.clear, tint = colors.onSurfaceVariant)
                                }
                            }
                        })
                }
            }
        } else if (progress < .25f) {
            Text(title, color = colors.onSurface, fontSize = if (largeTitle) 32.sp else 20.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun <T> ExpressiveNavigationBar(items: List<T>, selectedItem: T, onSelect: (T) -> Unit,
    label: (T) -> String, icon: (T) -> ImageVector, showSearch: Boolean,
    searchActive: Boolean, onSearch: () -> Unit, applyInsets: Boolean = true, modifier: Modifier = Modifier) {
    val colors = expressiveColors()
    Row(modifier.fillMaxWidth().then(if (applyInsets) Modifier.navigationBarsPadding() else Modifier)
        .padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.weight(1f).height(56.dp).clip(CircleShape).background(colors.surfaceContainer)
            .border(1.dp, colors.outlineVariant.copy(alpha = .55f), CircleShape).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items.forEach { item ->
                val active = selectedItem == item
                val weight by animateFloatAsState(if (active) 2f else 1f,
                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow), label = "tabWidth")
                val bg by animateColorAsState(if (active) colors.primaryContainer else colors.surfaceContainer, tween(240), label = "tabColor")
                Row(Modifier.weight(weight).fillMaxHeight().clip(CircleShape).background(bg)
                    .semantics { selected = active; role = Role.Tab }
                    .clickable { onSelect(item) }.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon(item), contentDescription = label(item), tint = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
                        modifier = Modifier.size(22.dp))
                    AnimatedVisibility(active, enter = expandHorizontally(tween(260)) + fadeIn(tween(160, 90)),
                        exit = shrinkHorizontally(tween(220)) + fadeOut(tween(100))) {
                        Text(label(item), Modifier.padding(start = 8.dp), color = colors.onPrimaryContainer,
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (showSearch) FilledTonalIconButton(onClick = onSearch, modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = if (searchActive) colors.primaryContainer else colors.surfaceContainer,
                contentColor = if (searchActive) colors.onPrimaryContainer else colors.onSurface)) {
            Icon(Icons.Filled.Search, LocalStrings.current.searchTitle, Modifier.size(24.dp))
        }
    }
}
