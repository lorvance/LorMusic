package com.lormusic.app.ui.components

import com.lormusic.app.model.LocalStrings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lormusic.app.data.TagFixEngine
import com.lormusic.app.model.Song
import com.lormusic.app.model.SongTagOverride
import com.lormusic.app.model.TagFixProposal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditorSheet(
    song: Song,
    onDismiss: () -> Unit,
    onSave: (SongTagOverride) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    var title by remember(song.id) { mutableStateOf(song.title) }
    var artist by remember(song.id) { mutableStateOf(song.artist) }
    var album by remember(song.id) { mutableStateOf(song.album) }
    var genre by remember(song.id) { mutableStateOf(song.genre.ifBlank { "Indie / Pop" }) }
    var year by remember(song.id) { mutableStateOf(song.year.ifBlank { "2026" }) }
    var trackNumber by remember(song.id) { mutableStateOf(song.trackNumber.ifBlank { "1" }) }
    var customLyrics by remember(song.id) { mutableStateOf(song.customLyrics.orEmpty()) }
    var coverUri by remember(song.id) { mutableStateOf(song.albumArtUri) }
    var isSearchingOnline by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { pickedUri ->
        if (pickedUri != null) {
            scope.launch {
                val savedUri = TagFixEngine.copyPickedCoverToDisk(context, song.id, pickedUri)
                if (savedUri != null) {
                    coverUri = savedUri
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.EditNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strings.tagEditorTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = strings.close)
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SongArtwork(
                    seed = song.coverColorSeed,
                    albumArtUri = coverUri,
                    songUri = song.uri,
                    cornerRadius = 22.dp,
                    modifier = Modifier.size(104.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.coverFromGallery)
                    }
                    OutlinedButton(
                        onClick = {
                            if (!isSearchingOnline) {
                                isSearchingOnline = true
                                scope.launch {
                                    val found = TagFixEngine.analyzeSingleSong(
                                        context,
                                        song.copy(title = title, artist = artist, album = album)
                                    )
                                    title = found.proposedTitle
                                    artist = found.proposedArtist
                                    album = found.proposedAlbum
                                    if (found.proposedYear.isNotBlank()) year = found.proposedYear
                                    if (found.proposedGenre.isNotBlank()) genre = found.proposedGenre
                                    if (!found.proposedCoverUri.isNullOrBlank()) coverUri = found.proposedCoverUri
                                    isSearchingOnline = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isSearchingOnline) {
                            ExpressiveLoadingIndicator(size = 20.dp)
                        } else {
                            Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(strings.autoSearchTags)
                    }
                    Spacer(Modifier.height(6.dp))
                    FilledTonalButton(
                        onClick = {
                            if (!isSearchingOnline) {
                                isSearchingOnline = true
                                scope.launch {
                                    val found = TagFixEngine.recognizeTrackShazam(context, song)
                                    title = found.proposedTitle
                                    artist = found.proposedArtist
                                    album = found.proposedAlbum
                                    if (found.proposedYear.isNotBlank()) year = found.proposedYear
                                    if (found.proposedGenre.isNotBlank()) genre = found.proposedGenre
                                    if (!found.proposedCoverUri.isNullOrBlank()) coverUri = found.proposedCoverUri
                                    isSearchingOnline = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Hearing, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.recognizeViaShazam)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(strings.trackTitleField) },
                leadingIcon = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text(strings.artistField) },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = album,
                onValueChange = { album = it },
                label = { Text(strings.albumField) },
                leadingIcon = { Icon(Icons.Filled.Album, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text(strings.genreField) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1.3f)
                )
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text(strings.yearField) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(0.9f)
                )
                OutlinedTextField(
                    value = trackNumber,
                    onValueChange = { trackNumber = it },
                    label = { Text("№") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(0.7f)
                )
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = customLyrics,
                onValueChange = { customLyrics = it },
                label = { Text(strings.lyricsField) },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(strings.cancel)
                }
                Button(
                    onClick = {
                        onSave(
                            SongTagOverride(
                                songId = song.id,
                                title = title.trim().ifBlank { song.title },
                                artist = artist.trim().ifBlank { song.artist },
                                album = album.trim().ifBlank { song.album },
                                genre = genre.trim(),
                                year = year.trim(),
                                trackNumber = trackNumber.trim(),
                                albumArtUri = coverUri,
                                customLyrics = customLyrics.trim().takeIf { it.isNotBlank() }
                            )
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.save)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagFixModalSheet(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onApplyProposals: (List<TagFixProposal>) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    val problematicIds = remember(songs) {
        songs.filter { TagFixEngine.isProblematicSong(it) }.map { it.id }.toSet()
    }
    var selectedIds by remember(songs) {
        mutableStateOf(if (problematicIds.isNotEmpty()) problematicIds else songs.take(10).map { it.id }.toSet())
    }

    var isAnalyzing by remember { mutableStateOf(false) }
    var recognizingSongId by remember { mutableStateOf<String?>(null) }
    var progressCurrent by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(0) }
    var proposals by remember { mutableStateOf<List<TagFixProposal>?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoFixHigh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (proposals == null) strings.tagFixTitle else strings.tagFixConfirmTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (proposals == null) {
                            strings.tagFixSubtitle
                        } else {
                            strings.tagFixConfirmSubtitle
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            when {
                isAnalyzing -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ExpressiveLoadingIndicator(size = 64.dp)
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = strings.tagFixProgressFormat.format(progressCurrent, progressTotal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = strings.tagFixShazamHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }
                }

                proposals != null -> {
                    val currentProposals = proposals!!
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(currentProposals) { idx, prop ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (prop.isAudioRecognized) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val updated = currentProposals.toMutableList()
                                        updated[idx] = prop.copy(isSelected = !prop.isSelected)
                                        proposals = updated
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SongArtwork(
                                        seed = prop.originalSong.coverColorSeed,
                                        albumArtUri = prop.proposedCoverUri,
                                        songUri = prop.originalSong.uri,
                                        cornerRadius = 14.dp,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (prop.isAudioRecognized) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = strings.tagFixShazamMatchTitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                        }
                                        Text(
                                            text = strings.tagFixWasFormat.format(prop.originalSong.title, prop.originalSong.artist),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = prop.proposedTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${prop.proposedArtist} • ${prop.proposedAlbum}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (recognizingSongId == null) {
                                                recognizingSongId = prop.originalSong.id
                                                scope.launch {
                                                    val shazamRes = TagFixEngine.recognizeTrackShazam(context, prop.originalSong)
                                                    val updated = currentProposals.toMutableList()
                                                    updated[idx] = shazamRes.copy(isSelected = true)
                                                    proposals = updated
                                                    recognizingSongId = null
                                                }
                                            }
                                        }
                                    ) {
                                        if (recognizingSongId == prop.originalSong.id) {
                                            ExpressiveLoadingIndicator(size = 20.dp)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.Hearing,
                                                contentDescription = strings.recognizeViaShazam,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Checkbox(
                                        checked = prop.isSelected,
                                        onCheckedChange = { checked ->
                                            val updated = currentProposals.toMutableList()
                                            updated[idx] = prop.copy(isSelected = checked)
                                            proposals = updated
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { proposals = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(strings.back)
                        }
                        val confirmed = currentProposals.filter { it.isSelected }
                        Button(
                            onClick = {
                                onApplyProposals(confirmed)
                                onDismiss()
                            },
                            enabled = confirmed.isNotEmpty(),
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.tagFixConfirmCount.format(confirmed.size))
                        }
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedIds == problematicIds && problematicIds.isNotEmpty(),
                            onClick = { selectedIds = problematicIds },
                            label = { Text(strings.tagFixProblematicTab.format(problematicIds.size)) },
                            shape = RoundedCornerShape(14.dp)
                        )
                        FilterChip(
                            selected = selectedIds.size == songs.size,
                            onClick = { selectedIds = songs.map { it.id }.toSet() },
                            label = { Text(strings.tagFixAllTab.format(songs.size)) },
                            shape = RoundedCornerShape(14.dp)
                        )
                        FilterChip(
                            selected = selectedIds.isEmpty(),
                            onClick = { selectedIds = emptySet() },
                            label = { Text(strings.reset) },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(songs, key = { it.id }) { song ->
                            val isSelected = selectedIds.contains(song.id)
                            val isRandom = TagFixEngine.isRandomOrUnnamed(song)
                            val isProb = problematicIds.contains(song.id)

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val next = selectedIds.toMutableSet()
                                        if (isSelected) next.remove(song.id) else next.add(song.id)
                                        selectedIds = next
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SongArtwork(
                                        seed = song.coverColorSeed,
                                        albumArtUri = song.albumArtUri,
                                        songUri = song.uri,
                                        cornerRadius = 12.dp,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val hint = when {
                                            isRandom -> strings.tagFixRandomNameHint
                                            isProb -> strings.tagFixNeedsCleaningHint.format(song.artist)
                                            else -> song.artist
                                        }
                                        Text(
                                            text = hint,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isRandom) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (recognizingSongId == null) {
                                                recognizingSongId = song.id
                                                scope.launch {
                                                    val shazamRes = TagFixEngine.recognizeTrackShazam(context, song)
                                                    proposals = listOf(shazamRes.copy(isSelected = true))
                                                    recognizingSongId = null
                                                }
                                            }
                                        }
                                    ) {
                                        if (recognizingSongId == song.id) {
                                            ExpressiveLoadingIndicator(size = 20.dp)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.Hearing,
                                                contentDescription = strings.recognizeViaShazam,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            val next = selectedIds.toMutableSet()
                                            if (checked) next.add(song.id) else next.remove(song.id)
                                            selectedIds = next
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val targetSongs = songs.filter { selectedIds.contains(it.id) }
                            if (targetSongs.isNotEmpty()) {
                                isAnalyzing = true
                                progressCurrent = 0
                                progressTotal = targetSongs.size
                                scope.launch {
                                    val analyzed = TagFixEngine.analyzeSongs(context, targetSongs) { cur, tot ->
                                        progressCurrent = cur
                                        progressTotal = tot
                                    }
                                    proposals = analyzed
                                    isAnalyzing = false
                                }
                            }
                        },
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.tagFixFixSelected.format(selectedIds.size))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
