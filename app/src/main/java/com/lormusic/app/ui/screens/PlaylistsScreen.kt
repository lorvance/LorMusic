package com.lormusic.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lormusic.app.model.LocalStrings
import com.lormusic.app.model.Playlist
import com.lormusic.app.model.Song
import com.lormusic.app.ui.components.SongArtwork
import com.lormusic.app.ui.components.SongRow

@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    allSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    onPlayQueue: (List<Song>, Int, Boolean) -> Unit,
    onCreatePlaylist: (String, List<Song>) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onAddSongToPlaylist: (String, Song) -> Unit,
    onRemoveSongFromPlaylist: (String, String) -> Unit,
    onToggleFavorite: (Song) -> Unit
) {
    val strings = LocalStrings.current
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val activePlaylist = playlists.find { it.id == selectedPlaylistId }

    AnimatedContent(
        targetState = activePlaylist,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally(tween(260)) { it / 4 } + fadeIn(tween(220)))
                    .togetherWith(slideOutHorizontally(tween(220)) { -it / 4 } + fadeOut(tween(180)))
            } else {
                (slideInHorizontally(tween(260)) { -it / 4 } + fadeIn(tween(220)))
                    .togetherWith(slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(180)))
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        label = "playlistNav"
    ) { detailPlaylist ->
        if (detailPlaylist != null) {
            PlaylistDetailContent(
                playlist = detailPlaylist,
                allSongs = allSongs,
                currentSong = currentSong,
                isPlaying = isPlaying,
                favoriteIds = favoriteIds,
                onBack = { selectedPlaylistId = null },
                onPlayAll = { shuffle ->
                    if (detailPlaylist.songs.isNotEmpty()) {
                        onPlayQueue(detailPlaylist.songs, 0, shuffle)
                    }
                },
                onPlaySongAt = { idx ->
                    onPlayQueue(detailPlaylist.songs, idx, false)
                },
                onAddSong = { song ->
                    onAddSongToPlaylist(detailPlaylist.id, song)
                },
                onRemoveSong = { songId ->
                    onRemoveSongFromPlaylist(detailPlaylist.id, songId)
                },
                onDeletePlaylist = {
                    onDeletePlaylist(detailPlaylist.id)
                    selectedPlaylistId = null
                },
                onToggleFavorite = onToggleFavorite
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.navPlaylists,
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.newPlaylist)
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 180.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { selectedPlaylistId = playlist.id },
                            onQuickPlay = {
                                if (playlist.songs.isNotEmpty()) {
                                    onPlayQueue(playlist.songs, 0, false)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        val selectedSongIds = remember { mutableStateListOf<String>() }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(strings.newPlaylistDialogTitle) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(strings.playlistNameLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "${strings.addTracksButton} (${selectedSongIds.size}):",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        itemsIndexed(allSongs, key = { _, s -> s.id }) { _, s ->
                            val checked = selectedSongIds.contains(s.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) selectedSongIds.remove(s.id)
                                        else selectedSongIds.add(s.id)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        if (checked) selectedSongIds.remove(s.id)
                                        else selectedSongIds.add(s.id)
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        s.artist,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        val initial = allSongs.filter { selectedSongIds.contains(it.id) }
                        onCreatePlaylist(name, initial)
                        showCreateDialog = false
                    }
                ) {
                    Text(strings.create)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onQuickPlay: () -> Unit
) {
    val strings = LocalStrings.current
    val firstSong = playlist.songs.firstOrNull()
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            SongArtwork(
                seed = playlist.coverColorSeed,
                albumArtUri = firstSong?.albumArtUri,
                songUri = firstSong?.uri,
                cornerRadius = 24.dp,
                modifier = Modifier.fillMaxSize()
            )
            if (playlist.isSystem) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = strings.autoPlaylistBadge,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
            if (playlist.songs.isNotEmpty()) {
                FilledIconButton(
                    onClick = onQuickPlay,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(40.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = strings.playPlaylist)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = "${playlist.songs.size} ${strings.tracksCount}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun PlaylistDetailContent(
    playlist: Playlist,
    allSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    onBack: () -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onPlaySongAt: (Int) -> Unit,
    onAddSong: (Song) -> Unit,
    onRemoveSong: (String) -> Unit,
    onDeletePlaylist: () -> Unit,
    onToggleFavorite: (Song) -> Unit
) {
    BackHandler(onBack = onBack)

    val strings = LocalStrings.current
    var showAddTracksDialog by remember { mutableStateOf(false) }
    val firstSong = playlist.songs.firstOrNull()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
            }
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (!playlist.isSystem) {
                IconButton(onClick = { showAddTracksDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = strings.addTracksButton)
                }
                IconButton(onClick = onDeletePlaylist) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = strings.deletePlaylist)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongArtwork(
                seed = playlist.coverColorSeed,
                albumArtUri = firstSong?.albumArtUri,
                songUri = firstSong?.uri,
                cornerRadius = 22.dp,
                modifier = Modifier.size(92.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(playlist.name, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${playlist.songs.size} ${strings.tracksCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = playlist.songs.isNotEmpty(),
                        onClick = { onPlayAll(false) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(strings.playAction)
                    }
                    FilledTonalButton(
                        enabled = playlist.songs.isNotEmpty(),
                        onClick = { onPlayAll(true) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (playlist.songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (playlist.isSystem) strings.emptyFavoritesHint
                        else strings.emptyPlaylistHint,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (!playlist.isSystem) {
                        Spacer(Modifier.height(12.dp))
                        FilledTonalButton(onClick = { showAddTracksDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.addTracksButton)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 180.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(playlist.songs, key = { idx, s -> "${s.id}_$idx" }) { idx, song ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            SongRow(
                                song = song,
                                isCurrentSong = currentSong?.id == song.id,
                                isPlaying = isPlaying,
                                isFavorite = favoriteIds.contains(song.id),
                                onClick = { onPlaySongAt(idx) },
                                onToggleFavorite = { onToggleFavorite(song) }
                            )
                        }
                        if (!playlist.isSystem) {
                            IconButton(onClick = { onRemoveSong(song.id) }) {
                                Icon(
                                    imageVector = Icons.Filled.RemoveCircleOutline,
                                    contentDescription = strings.removeFromPlaylist,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTracksDialog) {
        val existingIds = remember(playlist.songs) { playlist.songs.map { it.id }.toSet() }
        val availableSongs = remember(allSongs, existingIds) {
            allSongs.filterNot { existingIds.contains(it.id) }
        }
        AlertDialog(
            onDismissRequest = { showAddTracksDialog = false },
            title = { Text("${strings.addToPlaylist}: «${playlist.name}»") },
            text = {
                if (availableSongs.isEmpty()) {
                    Text(strings.allTracksAlreadyInPlaylist)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        itemsIndexed(availableSongs, key = { _, s -> s.id }) { _, song ->
                            ListItem(
                                headlineContent = { Text(song.title) },
                                supportingContent = { Text(song.artist) },
                                trailingContent = {
                                    IconButton(onClick = { onAddSong(song) }) {
                                        Icon(Icons.Filled.AddCircleOutline, contentDescription = strings.addTracksButton)
                                    }
                                },
                                modifier = Modifier.clickable { onAddSong(song) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddTracksDialog = false }) {
                    Text(strings.done)
                }
            }
        )
    }
}
