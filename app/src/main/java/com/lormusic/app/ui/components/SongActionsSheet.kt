package com.lormusic.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
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
                    overflow = TextOverflow.Ellipsis
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
    onOpenArtistProfile: ((String) -> Unit)? = null
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongArtwork(
                    seed = song.coverColorSeed,
                    albumArtUri = song.albumArtUri,
                    songUri = song.uri,
                    cornerRadius = 14.dp,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${song.artist} • ${song.album}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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
                    ListItem(
                        headlineContent = { Text(strings.playNow) },
                        leadingContent = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                        modifier = Modifier.clickable {
                            onPlayNow()
                            onDismiss()
                        }
                    )
                    ListItem(
                        headlineContent = { Text(strings.playNextInQueue) },
                        leadingContent = { Icon(Icons.Filled.QueuePlayNext, contentDescription = null) },
                        modifier = Modifier.clickable {
                            onPlayNext()
                            onDismiss()
                        }
                    )
                    if (onOpenArtistProfile != null && song.artist.isNotBlank()) {
                        ListItem(
                            headlineContent = { Text("${strings.artistProfile}: ${song.artist}") },
                            leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                            modifier = Modifier.clickable {
                                onDismiss()
                                onOpenArtistProfile(song.artist)
                            }
                        )
                    }
                    if (onOpenTagEditor != null) {
                        ListItem(
                            headlineContent = { Text(strings.editTagsAndCover) },
                            supportingContent = { Text(strings.editTagsSubtitle) },
                            leadingContent = {
                                Icon(
                                    Icons.Filled.EditNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable {
                                onDismiss()
                                onOpenTagEditor()
                            }
                        )
                    }
                    if (onDownloadSong != null && (song.isRemoteYt || song.isRemoteSc || song.id.startsWith("sc_") || song.uri.startsWith("http") || song.uri.startsWith("yt://") || song.uri.startsWith("sc://"))) {
                        ListItem(
                            headlineContent = { Text(strings.downloadToPhone) },
                            supportingContent = { Text(strings.downloadSubtitle) },
                            leadingContent = {
                                Icon(
                                    Icons.Filled.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable {
                                onDownloadSong()
                                onDismiss()
                            }
                        )
                    }
                    if (shareUrl != null) {
                        ListItem(
                            headlineContent = { Text(strings.shareSong) },
                            supportingContent = { Text("${strings.share}: $shareUrl") },
                            leadingContent = {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable {
                                if (scShareUrl != null) {
                                    SoundCloudRepository.shareSongLink(context, song)
                                } else {
                                    YouTubeMusicRepository.shareSongLink(context, song)
                                }
                                onDismiss()
                            }
                        )
                    }
                    ListItem(
                        headlineContent = {
                            Text(if (isFavorite) strings.removeFromFavorites else strings.addToFavorites)
                        },
                        leadingContent = {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) MaterialTheme.colorScheme.secondary else LocalContentColor.current
                            )
                        },
                        modifier = Modifier.clickable {
                            onToggleFavorite()
                            onDismiss()
                        }
                    )
                    val unifiedPl = playlists.find { it.id == "sys_unified_favorites" }
                    val isInUnifiedFav = unifiedPl?.songs?.any { it.id == song.id } == true
                    ListItem(
                        headlineContent = {
                            Text(if (isInUnifiedFav) strings.scRemoveFromUnifiedFavorites else strings.scAddToUnifiedFavorites)
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = if (isInUnifiedFav) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        },
                        modifier = Modifier.clickable {
                            onAddToPlaylist("sys_unified_favorites")
                            onDismiss()
                        }
                    )
                    ListItem(
                        headlineContent = { Text(strings.addToPlaylistAction) },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showPlaylistPicker = true
                        }
                    )
                    if (onToggleHidden != null) {
                        ListItem(
                            headlineContent = {
                                Text(if (isHidden) strings.unhideFromStream else strings.hideFromStream)
                            },
                            supportingContent = {
                                Text(
                                    if (isHidden) strings.unhideSubtitle
                                    else strings.hideSubtitle
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = if (isHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable {
                                onToggleHidden()
                                onDismiss()
                            }
                        )
                    }
                    ListItem(
                        headlineContent = { Text(strings.trackDetailsAction) },
                        leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showTrackDetails = true
                        }
                    )
                    if (onDeleteFromDevice != null && !song.isRemoteYt && !song.uri.startsWith("http") && !song.uri.startsWith("yt://")) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = strings.deleteFromDevice,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            supportingContent = {
                                Text(strings.deleteFromDeviceSubtitle)
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.DeleteForever,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            modifier = Modifier.clickable {
                                showDeleteConfirm = true
                            }
                        )
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
                                                    tint = MaterialTheme.colorScheme.primary,
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
