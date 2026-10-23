package com.lormusic.app.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lormusic.app.model.LocalStrings
import com.lormusic.app.model.Playlist
import com.lormusic.app.model.Song
import com.lormusic.app.ui.components.SongArtwork
import com.lormusic.app.ui.components.SongRow
import com.lormusic.app.ui.components.liquidGlassBubble
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

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
    onToggleFavorite: (Song) -> Unit,
    hazeState: HazeState? = null,
    enableBlur: Boolean = true,
    useLiquidGlass: Boolean = false
) {
    val strings = LocalStrings.current
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    @Composable
    fun bubbleModifier(shape: CornerBasedShape): Modifier {
        return Modifier.liquidGlassBubble(
            shape = shape,
            hazeState = hazeState,
            enableBlur = enableBlur,
            isLiquidGlass = useLiquidGlass
        )
    }

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
                onToggleFavorite = onToggleFavorite,
                bubbleModifier = { bubbleModifier(it) }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                var headerHeightDp by remember { mutableStateOf(68.dp) }
                val density = LocalDensity.current
                val view = LocalView.current

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = headerHeightDp + 12.dp, bottom = 185.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(2) }, key = "stats_row") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = strings.statsTracks,
                                value = allSongs.size.toString(),
                                modifier = Modifier.weight(1f),
                                bubbleModifier = { bubbleModifier(it) }
                            )
                            StatCard(
                                title = strings.statsFavorites,
                                value = favoriteIds.size.toString(),
                                modifier = Modifier.weight(1f),
                                bubbleModifier = { bubbleModifier(it) }
                            )
                            StatCard(
                                title = strings.statsPlaylists,
                                value = playlists.size.toString(),
                                modifier = Modifier.weight(1f),
                                bubbleModifier = { bubbleModifier(it) }
                            )
                        }
                    }

                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { selectedPlaylistId = playlist.id },
                            onQuickPlay = {
                                if (playlist.songs.isNotEmpty()) {
                                    onPlayQueue(playlist.songs, 0, false)
                                }
                            },
                            bubbleModifier = { bubbleModifier(it) }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            headerHeightDp = with(density) { coords.size.height.toDp() }
                        }
                ) {
                    if (enableBlur && hazeState != null) {
                        val topScrimColor = MaterialTheme.colorScheme.background
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .hazeEffect(state = hazeState) {
                                    blurEnabled = true
                                    backgroundColor = topScrimColor
                                    blurRadius = 30.dp
                                    tints = listOf(HazeTint(topScrimColor.copy(alpha = 0.65f)))
                                }
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            topScrimColor.copy(alpha = 0.75f),
                                            topScrimColor.copy(alpha = 0.35f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.size(44.dp))

                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .then(bubbleModifier(RoundedCornerShape(22.dp)))
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.navPlaylists,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .then(bubbleModifier(RoundedCornerShape(22.dp)))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    showCreateDialog = true
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = strings.newPlaylist,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = strings.newPlaylist,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
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
    onQuickPlay: () -> Unit,
    bubbleModifier: @Composable (CornerBasedShape) -> Modifier = { Modifier }
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
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .then(bubbleModifier(RoundedCornerShape(14.dp)))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = strings.autoPlaylistBadge,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            if (playlist.songs.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .then(bubbleModifier(CircleShape))
                        .clickable(onClick = onQuickPlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = strings.playPlaylist,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
    onToggleFavorite: (Song) -> Unit,
    bubbleModifier: @Composable (CornerBasedShape) -> Modifier = { Modifier }
) {
    BackHandler(onBack = onBack)

    val strings = LocalStrings.current
    var showAddTracksDialog by remember { mutableStateOf(false) }
    val firstSong = playlist.songs.firstOrNull()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .then(bubbleModifier(CircleShape))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.back,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .then(bubbleModifier(RoundedCornerShape(22.dp)))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (!playlist.isSystem) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .then(bubbleModifier(CircleShape))
                        .clickable { showAddTracksDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = strings.addTracksButton,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .then(bubbleModifier(CircleShape))
                        .clickable(onClick = onDeletePlaylist),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = strings.deletePlaylist,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
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
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .then(bubbleModifier(RoundedCornerShape(20.dp)))
                            .clickable(enabled = playlist.songs.isNotEmpty(), onClick = { onPlayAll(false) })
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text(strings.playAction, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .then(bubbleModifier(CircleShape))
                            .clickable(enabled = playlist.songs.isNotEmpty(), onClick = { onPlayAll(true) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .then(bubbleModifier(RoundedCornerShape(22.dp)))
                                .clickable { showAddTracksDialog = true }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.dp))
                                Text(strings.addTracksButton, fontWeight = FontWeight.Bold)
                            }
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

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    bubbleModifier: @Composable (CornerBasedShape) -> Modifier = { Modifier }
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .then(bubbleModifier(RoundedCornerShape(22.dp)))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
