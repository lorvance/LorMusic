package com.lormusic.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lormusic.app.model.LocalStrings
import com.lormusic.app.model.Song
import com.lormusic.app.ui.components.SongRow

@Composable
fun SearchScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    onPlayQueue: (List<Song>, Int, Boolean) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val strings = LocalStrings.current

    val artists = remember(songs) {
        songs.map { it.artist }.distinct().take(8)
    }

    val results = remember(query, songs) {
        if (query.isBlank()) {
            songs
        } else {
            val q = query.trim()
            songs.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.artist.contains(q, ignoreCase = true) ||
                    it.album.contains(q, ignoreCase = true) ||
                    it.folderName.contains(q, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = strings.searchTitle,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(strings.searchPlaceholder) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = strings.clear)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            artists.forEach { artist ->
                val isSelected = query.equals(artist, ignoreCase = true)
                AssistChip(
                    onClick = { query = if (isSelected) "" else artist },
                    label = { Text(artist) },
                    shape = RoundedCornerShape(16.dp),
                    colors = if (isSelected) {
                        AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    } else {
                        AssistChipDefaults.assistChipColors()
                    }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = if (query.isBlank()) strings.allTracksCount.format(results.size) else strings.foundCount.format(results.size),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.nothingFoundQuery.format(query),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 180.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(results, key = { _, s -> s.id }) { index, song ->
                    Box(modifier = Modifier.animateItem()) {
                        SongRow(
                            song = song,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = isPlaying,
                            isFavorite = favoriteIds.contains(song.id),
                            onClick = { onPlayQueue(results, index, false) },
                            onToggleFavorite = { onToggleFavorite(song) },
                            onMoreClick = { onSongMoreClick(song) }
                        )
                    }
                }
            }
        }
    }
}
