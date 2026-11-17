package com.lormusic.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lormusic.app.R
import com.lormusic.app.model.LocalStrings

val LocalManagedServiceHeader = staticCompositionLocalOf { false }

@Composable
fun Modifier.serviceStatusBarsPadding(): Modifier =
    if (LocalManagedServiceHeader.current) this else this.statusBarsPadding()

@Composable
fun ServiceScreenShell(
    enabled: Boolean,
    name: String,
    tab: String,
    avatar: String?,
    onBack: () -> Unit,
    onAccount: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (enabled)
            ServiceExpressiveHeader(
                name,
                tab,
                avatar,
                onBack,
                onAccount,
                Modifier.statusBarsPadding(),
            )
        CompositionLocalProvider(LocalManagedServiceHeader provides enabled) {
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        }
    }
}

@Composable
fun ServiceExpressiveHeader(
    name: String,
    tab: String,
    avatar: String?,
    onBack: () -> Unit,
    onAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.colorScheme
    val strings = LocalStrings.current
    Box(modifier.fillMaxWidth().height(96.dp).background(c.background)) {
        Canvas(Modifier.matchParentSize()) {
            val top =
                Path().apply {
                    moveTo(size.width * .52f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * .62f)
                    cubicTo(
                        size.width * .74f,
                        size.height * .35f,
                        size.width * .62f,
                        size.height * .65f,
                        size.width * .52f,
                        0f,
                    )
                    close()
                }
            val bottom =
                Path().apply {
                    moveTo(0f, size.height)
                    cubicTo(
                        size.width * .15f,
                        size.height * .27f,
                        size.width * .29f,
                        size.height * .88f,
                        size.width * .39f,
                        size.height,
                    )
                    close()
                }
            drawPath(top, c.surfaceContainerHigh.copy(alpha = .78f))
            drawPath(bottom, c.surfaceContainerHigh.copy(alpha = .6f))
        }
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalIconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors =
                    IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = c.surfaceContainerHighest,
                        contentColor = c.onSurface,
                    ),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.back)
            }
            if (name == "SoundCloud")
                Icon(
                    painterResource(R.drawable.ic_soundcloud),
                    null,
                    Modifier.size(28.dp),
                    tint = c.onSurface,
                )
            else Icon(Icons.Default.PlayCircle, null, Modifier.size(28.dp), tint = c.onSurface)
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    tab,
                    style = MaterialTheme.typography.labelMedium,
                    color = c.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledTonalIconButton(
                onClick = onAccount,
                modifier =
                    Modifier.size(48.dp).semantics {
                        contentDescription =
                            if (name == "SoundCloud") strings.scProfileTitle
                            else strings.selectGoogleAccount
                    },
                shape = CircleShape,
                colors =
                    IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = c.surfaceContainerHighest,
                        contentColor = c.onSurface,
                    ),
            ) {
                if (!avatar.isNullOrBlank())
                    SongArtwork(
                        seed = avatar.hashCode(),
                        albumArtUri = avatar,
                        cornerRadius = 24.dp,
                        modifier = Modifier.size(40.dp),
                    )
                else Icon(Icons.Default.Person, null)
            }
        }
    }
}
