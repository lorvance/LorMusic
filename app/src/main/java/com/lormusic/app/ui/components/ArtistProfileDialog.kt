package com.lormusic.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lormusic.app.data.TagFixEngine
import com.lormusic.app.model.LocalStrings
import com.lormusic.app.model.Song

@Composable
fun ArtistProfileDialog(
    artistName: String,
    allArtistSongs: List<Song>,
    topTracks: List<Song>,
    albums: Map<String, List<Song>>,
    singlesAndEPs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    isFavorite: (String) -> Boolean,
    onDismiss: () -> Unit,
    onPlaySongAt: (List<Song>, Int) -> Unit,
    onPlayAll: (List<Song>, Boolean) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit
) {
    val strings = LocalStrings.current
    var avatarUrl by remember(artistName) { mutableStateOf<String?>(null) }

    LaunchedEffect(artistName) {
        val remoteAvatar = TagFixEngine.fetchArtistAvatar(artistName)
        if (!remoteAvatar.isNullOrBlank()) {
            avatarUrl = remoteAvatar
        } else {
            avatarUrl = allArtistSongs.firstOrNull { !it.albumArtUri.isNullOrBlank() }?.albumArtUri
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item(key = "header") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                    ) {
                        SongArtwork(
                            seed = kotlin.math.abs(artistName.hashCode()),
                            albumArtUri = avatarUrl,
                            cornerRadius = 0.dp,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.35f),
                                            Color.Black.copy(alpha = 0.55f),
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                )
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.TopStart)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                        ) {
                            Text(
                                text = strings.artistProfile.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = artistName,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "${allArtistSongs.size} ${strings.statsTracks.lowercase()} • ${albums.size} ${strings.artistAlbums.lowercase()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                item(key = "action_buttons") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onPlayAll(allArtistSongs, false) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            enabled = allArtistSongs.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.artistPlayAll, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onPlayAll(allArtistSongs, true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            enabled = allArtistSongs.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.artistShuffle, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (topTracks.isNotEmpty()) {
                    item(key = "section_top_tracks_title") {
                        Text(
                            text = strings.artistTopTracks,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }

                    items(topTracks.take(5), key = { "top_" + it.id }) { song ->
                        val isCurrent = currentSong?.id == song.id
                        val isFav = isFavorite(song.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val idx = allArtistSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                    onPlaySongAt(allArtistSongs, idx)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SongArtwork(
                                seed = song.coverColorSeed,
                                albumArtUri = song.albumArtUri,
                                songUri = song.uri,
                                cornerRadius = 12.dp,
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.album,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onToggleFavorite(song) }) {
                                Icon(
                                    imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onSongMoreClick(song) }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (albums.isNotEmpty()) {
                    item(key = "section_albums_title") {
                        Text(
                            text = strings.artistAlbums,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 12.dp)
                        )
                    }

                    item(key = "albums_carousel") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(albums.entries.toList(), key = { "album_" + it.key }) { (albumName, albumSongs) ->
                                val firstSong = albumSongs.firstOrNull()
                                Column(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .clickable { onPlayAll(albumSongs, false) }
                                ) {
                                    if (firstSong != null) {
                                        SongArtwork(
                                            seed = firstSong.coverColorSeed,
                                            albumArtUri = firstSong.albumArtUri,
                                            songUri = firstSong.uri,
                                            cornerRadius = 18.dp,
                                            modifier = Modifier.size(140.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = albumName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${albumSongs.size} ${strings.statsTracks.lowercase()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (singlesAndEPs.isNotEmpty()) {
                    item(key = "section_singles_title") {
                        Text(
                            text = strings.artistSinglesAndEPs,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
                        )
                    }

                    items(singlesAndEPs, key = { "single_" + it.id }) { song ->
                        val isCurrent = currentSong?.id == song.id
                        val isFav = isFavorite(song.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val idx = allArtistSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                    onPlaySongAt(allArtistSongs, idx)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SongArtwork(
                                seed = song.coverColorSeed,
                                albumArtUri = song.albumArtUri,
                                songUri = song.uri,
                                cornerRadius = 12.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.album.ifBlank { strings.artistSinglesAndEPs },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onToggleFavorite(song) }) {
                                Icon(
                                    imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onSongMoreClick(song) }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
