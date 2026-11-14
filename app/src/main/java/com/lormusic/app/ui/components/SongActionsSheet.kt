package com.lormusic.app.ui.components

import android.view.HapticFeedbackConstants
import com.lormusic.app.model.betaText
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.*

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lormusic.app.data.SoundCloudRepository
import com.lormusic.app.data.YouTubeMusicRepository
import com.lormusic.app.model.LocalStrings
import com.lormusic.app.model.Playlist
import com.lormusic.app.model.Song

@Composable
fun SongRow(
    song: Song,
    isCurrentSong: Boolean = false,
    isPlaying: Boolean = false,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null
) {
    val view = LocalView.current
    val strings = LocalStrings.current
    val containerColor = if (isCurrentSong) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.0f)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onClick()
            },
        color = containerColor,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongArtwork(
                seed = song.coverColorSeed,
                albumArtUri = song.albumArtUri,
                songUri = song.uri,
                cornerRadius = 14.dp,
                showEqualizerOverlay = isCurrentSong,
                isPlaying = isCurrentSong && isPlaying,
                modifier = Modifier.size(50.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 2000,
                        repeatDelayMillis = 2500,
                        velocity = 35.dp
                    )
                )
                Text(
                    text = "${song.artist} • ${song.album} • ${formatDurationShort(song.durationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onToggleFavorite != null) {
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onToggleFavorite()
                    }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = strings.favorites,
                        tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
            if (onMoreClick != null) {
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = strings.more,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionsBottomSheet(
    song: Song,
    isFavorite: Boolean,
    isHidden: Boolean = false,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onCreateNewPlaylistWithSong: (String) -> Unit,
    onCreateUnifiedPlaylistWithSong: ((String) -> Unit)? = null,
    onOpenTagEditor: (() -> Unit)? = null,
    onDownloadSong: (() -> Unit)? = null,
    onToggleHidden: (() -> Unit)? = null,
    onDeleteFromDevice: (() -> Unit)? = null,
    onOpenArtistProfile: ((String) -> Unit)? = null,
    onOpenEqualizer: (() -> Unit)? = null,
    onOpenSpeedDialog: (() -> Unit)? = null
) {
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showTrackDetails by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    val strings = LocalStrings.current
    val context = LocalContext.current
    val ytShareUrl = remember(song) { YouTubeMusicRepository.getShareUrl(song) }
    val scShareUrl = remember(song) { SoundCloudRepository.getShareUrl(song) }
    val shareUrl = ytShareUrl ?: scShareUrl

    val scheme = MaterialTheme.colorScheme
    val darkSheet = scheme.surface.luminance() < .5f
    val sheetColor = lerp(scheme.surface, scheme.primary, if (darkSheet) .025f else .015f)
    val cardColor = lerp(scheme.surfaceContainerLow, scheme.primary, if (darkSheet) .10f else .06f)
    val neutralIcon = scheme.onSurface
    val cardOutline = scheme.primary.copy(alpha = .16f)

    ModalBottomSheet(
        containerColor = sheetColor,
        contentColor = scheme.onSurface,
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongArtwork(seed = song.coverColorSeed, albumArtUri = song.albumArtUri,
                    songUri = song.uri, cornerRadius = 18.dp, modifier = Modifier.size(72.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                    Text("${song.artist} • ${song.album}", style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (!showPlaylistPicker && !showTrackDetails && !showDeleteConfirm) {
                    Spacer(Modifier.width(12.dp))
                    FilledIconButton(onClick = { onPlayNow(); onDismiss() }, modifier = Modifier.size(52.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = scheme.primary,
                            contentColor = scheme.onPrimary)) {
                        Icon(Icons.Filled.PlayArrow, strings.playNow, Modifier.size(30.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            when {
                showDeleteConfirm -> {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(
                            text = strings.deleteSongDialogTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "«${song.title}»: ${strings.deleteFromDeviceSubtitle}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDeleteConfirm = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(strings.cancel)
                            }
                            Button(
                                onClick = {
                                    onDeleteFromDevice?.invoke()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.delete)
                            }
                        }
                    }
                }

                showTrackDetails -> {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text(strings.fileDetailsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("${strings.trackTitleField}: ${song.title}", style = MaterialTheme.typography.bodyMedium)
                        Text("${strings.artistField}: ${song.artist}", style = MaterialTheme.typography.bodyMedium)
                        Text("${strings.albumField}: ${song.album}", style = MaterialTheme.typography.bodyMedium)
                        if (song.genre.isNotBlank()) {
                            Text("${strings.genreField}: ${song.genre}", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (song.year.isNotBlank()) {
                            Text("${strings.yearField}: ${song.year}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("${strings.libraryTitle}: ${if (isHidden) "${strings.hiddenFolderName} (${song.folderName})" else song.folderName}", style = MaterialTheme.typography.bodyMedium)
                        Text("${strings.sortOptionDuration}: ${formatDurationShort(song.durationMs)}", style = MaterialTheme.typography.bodyMedium)
                        if (!song.filePath.isNullOrBlank()) {
                            Text(
                                "Path: ${song.filePath}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        FilledTonalButton(onClick = { showTrackDetails = false }) {
                            Text(strings.back)
                        }
                    }
                }

                !showPlaylistPicker -> {
                    val isInUnifiedFav = playlists.find { it.id == "sys_unified_favorites" }
                        ?.songs?.any { it.id == song.id } == true
                    @Composable
                    fun QuickActions(twoColumns: Boolean) {
                        val actions: List<Triple<ImageVector, String, () -> Unit>> = listOf(
                            Triple(Icons.Outlined.QueuePlayNext, strings.betaText("songQueue"), { onPlayNext(); onDismiss() }),
                            Triple(Icons.AutoMirrored.Filled.PlaylistAdd, strings.betaText("songPlaylist"), { showPlaylistPicker = true }),
                            Triple(if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                strings.betaText(if (isFavorite) "songUnlike" else "songLike"), { onToggleFavorite(); onDismiss() }),
                            Triple(Icons.Outlined.AutoAwesome, strings.betaText(if (isInUnifiedFav) "songUnifiedRemove" else "songUnified"),
                                { onAddToPlaylist("sys_unified_favorites"); onDismiss() })
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            actions.chunked(if (twoColumns) 2 else 4).forEach { row ->
                                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    row.forEach { (icon, label, action) ->
                                        Surface(onClick = action, shape = RoundedCornerShape(24.dp), color = cardColor,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, cardOutline),
                                            modifier = Modifier.weight(1f).fillMaxHeight()) {
                                            Column(Modifier.heightIn(min = 92.dp).padding(horizontal = 5.dp, vertical = 14.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center) {
                                                Icon(icon, null, Modifier.size(28.dp), tint = neutralIcon)
                                                Spacer(Modifier.height(10.dp))
                                                Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val largeText = LocalDensity.current.fontScale > 1.2f
                        BoxWithConstraints { QuickActions(twoColumns = maxWidth < 310.dp || largeText) }
                        if ((onOpenArtistProfile != null && song.artist.isNotBlank()) || onOpenTagEditor != null) {
                            SongActionCard(cardColor, cardOutline) {
                                if (onOpenArtistProfile != null && song.artist.isNotBlank()) {
                                    SongActionRow(strings.artistProfile, song.artist, Icons.Outlined.Person, neutralIcon,
                                        chevron = true, onClick = { onDismiss(); onOpenArtistProfile(song.artist) })
                                }
                                if (onOpenTagEditor != null) {
                                    if (onOpenArtistProfile != null && song.artist.isNotBlank()) SongActionDivider(cardOutline)
                                    SongActionRow(strings.editTagsAndCover, strings.editTagsSubtitle, Icons.Outlined.Sell, neutralIcon,
                                        chevron = true, onClick = { onDismiss(); onOpenTagEditor() })
                                }
                            }
                        }
                        SongActionCard(cardColor, cardOutline) {
                            if (onToggleHidden != null) {
                                SongActionRow(if (isHidden) strings.unhideFromStream else strings.hideFromStream,
                                    if (isHidden) strings.unhideSubtitle else strings.hideSubtitle,
                                    if (isHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, neutralIcon,
                                    onClick = { onToggleHidden(); onDismiss() })
                                SongActionDivider(cardOutline)
                            }
                            SongActionRow(strings.trackDetailsAction, icon = Icons.Outlined.Info, tint = neutralIcon,
                                chevron = true, onClick = { showTrackDetails = true })
                            if (onOpenEqualizer != null) {
                                SongActionDivider(cardOutline)
                                SongActionRow(strings.equalizerTitle, icon = Icons.Outlined.GraphicEq, tint = neutralIcon,
                                    onClick = { onDismiss(); onOpenEqualizer() })
                            }
                            if (onOpenSpeedDialog != null) {
                                SongActionDivider(cardOutline)
                                SongActionRow(strings.playbackSpeedTitle, icon = Icons.Outlined.Speed, tint = neutralIcon,
                                    onClick = { onDismiss(); onOpenSpeedDialog() })
                            }
                            if (onDownloadSong != null && (song.isRemoteYt || song.isRemoteSc || song.uri.startsWith("http") || song.uri.startsWith("yt://") || song.uri.startsWith("sc://"))) {
                                SongActionDivider(cardOutline)
                                SongActionRow(strings.downloadToPhone, strings.downloadSubtitle, Icons.Outlined.Download, neutralIcon,
                                    onClick = { onDownloadSong(); onDismiss() })
                            }
                            if (shareUrl != null) {
                                SongActionDivider(cardOutline)
                                SongActionRow(strings.shareSong, icon = Icons.Outlined.Share, tint = neutralIcon,
                                    onClick = {
                                        if (scShareUrl != null) SoundCloudRepository.shareSongLink(context, song)
                                        else YouTubeMusicRepository.shareSongLink(context, song)
                                        onDismiss()
                                    })
                            }
                        }
                        if (onDeleteFromDevice != null && !song.isRemoteYt && !song.isRemoteSc && !song.uri.startsWith("http") && !song.uri.startsWith("yt://") && !song.uri.startsWith("sc://")) {
                            SongActionCard(scheme.error.copy(alpha = .10f), scheme.error.copy(alpha = .16f)) {
                                SongActionRow(strings.deleteFromDevice, strings.deleteFromDeviceSubtitle, Icons.Outlined.Delete,
                                    scheme.error, onClick = { showDeleteConfirm = true })
                            }
                        }
                    }
                }

                else -> {
                    Text(
                        text = strings.selectPlaylist,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                    var createAsUnified by remember { mutableStateOf(song.isRemoteYt || song.isRemoteSc) }
                    val availablePlaylists = remember(playlists) {
                        val unifiedFav = playlists.find { it.id == "sys_unified_favorites" }
                        val others = playlists.filter { !it.isSystem }
                        if (unifiedFav != null) listOf(unifiedFav) + others else others
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(availablePlaylists) { pl ->
                            val isUniFav = pl.id == "sys_unified_favorites"
                            ListItem(
                                headlineContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(if (isUniFav) strings.unifiedFavorites else pl.name)
                                        if (pl.isUnified) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                            ) {
                                                Text(
                                                    text = strings.unifiedPlaylist,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                },
                                supportingContent = { Text("${pl.songs.size} ${strings.tracksCount}") },
                                leadingContent = {
                                    if (isUniFav) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = neutralIcon,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        SongArtwork(
                                            seed = pl.coverColorSeed,
                                            cornerRadius = 10.dp,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.clickable {
                                    onAddToPlaylist(pl.id)
                                    onDismiss()
                                }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = createAsUnified,
                            onCheckedChange = { createAsUnified = it }
                        )
                        Text(
                            text = strings.createUnifiedPlaylist,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable { createAsUnified = !createAsUnified }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            placeholder = { Text(if (createAsUnified) strings.unifiedPlaylist else strings.newPlaylistPlaceholder) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(
                            enabled = newPlaylistName.isNotBlank(),
                            onClick = {
                                if (createAsUnified && onCreateUnifiedPlaylistWithSong != null) {
                                    onCreateUnifiedPlaylistWithSong(newPlaylistName.trim())
                                } else {
                                    onCreateNewPlaylistWithSong(newPlaylistName.trim())
                                }
                                onDismiss()
                            }
                        ) {
                            Text(strings.create)
                        }
                    }
                }
            }
        }
    }
}

fun formatDurationShort(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0L)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

@Composable
private fun SongActionCard(color: Color, outline: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = color,
        border = androidx.compose.foundation.BorderStroke(1.dp, outline), modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
private fun SongActionDivider(color: Color) {
    HorizontalDivider(Modifier.padding(start = 64.dp, end = 18.dp), color = color)
}

@Composable
private fun SongActionRow(title: String, subtitle: String? = null, icon: ImageVector,
    tint: Color, chevron: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 64.dp)
        .padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(26.dp), tint = tint)
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                color = if (icon == Icons.Outlined.Delete) tint else LocalContentColor.current)
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (chevron) {
            Spacer(Modifier.width(10.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(24.dp), tint = tint.copy(alpha = .65f))
        }
    }
}
