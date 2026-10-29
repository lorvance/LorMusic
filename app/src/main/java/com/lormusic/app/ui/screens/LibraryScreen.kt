package com.lormusic.app.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lormusic.app.data.AlphaNumericComparator
import com.lormusic.app.model.LibraryFilter
import com.lormusic.app.model.LocalStrings
import com.lormusic.app.model.Song
import com.lormusic.app.model.SortOption
import com.lormusic.app.ui.components.ExpressiveLoadingIndicator
import com.lormusic.app.ui.components.SongRow
import com.lormusic.app.ui.components.appBackdropSource
import com.lormusic.app.ui.components.isLiquidGlassSupported
import com.lormusic.app.ui.components.liquidGlass
import com.lormusic.app.ui.components.liquidGlassBubble
import com.lormusic.app.ui.components.rememberSmoothFlingBehavior
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

private const val HIDDEN_FOLDER_NAME = "__hidden__"

@Composable
fun LibraryScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    hiddenSongIds: Set<String> = emptySet(),
    hideDemoTracks: Boolean = false,
    enableBlur: Boolean = true,
    useLiquidGlass: Boolean = false,
    classicLibraryDesign: Boolean = false,
    showTopSearchBar: Boolean = true,
    externalSearchActive: Boolean = false,
    selectedFilter: LibraryFilter,
    selectedSort: SortOption,
    hasStoragePermission: Boolean,
    isScanningLocal: Boolean,
    hazeState: HazeState,
    onSelectFilter: (LibraryFilter) -> Unit,
    onSelectSort: (SortOption) -> Unit,
    onRequestPermissionAndScan: () -> Unit,
    showBackupNotice: Boolean = false,
    onDismissBackupNotice: () -> Unit = {},
    onExportBackup: () -> Unit = {},
    onOpenTagFix: () -> Unit,
    onOpenYtMusic: () -> Unit = {},
    onOpenSoundCloud: () -> Unit = {},
    onPlayQueue: (List<Song>, Int, Boolean) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit
) {
    val strings = LocalStrings.current
    val view = LocalView.current
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var isTopSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val isSearchVisible = if (showTopSearchBar) isTopSearchExpanded else externalSearchActive
    LaunchedEffect(isSearchVisible) {
        if (!isSearchVisible) {
            searchQuery = ""
        }
    }

    val density = LocalDensity.current
    var headerHeightDp by remember { mutableStateOf(175.dp) }

    val baseSongsPool = remember(songs, hideDemoTracks) {
        if (hideDemoTracks) songs.filter { it.isLocal } else songs
    }

    val availableFolders = remember(baseSongsPool, hiddenSongIds) {
        val normalFolders = baseSongsPool
            .filter { !hiddenSongIds.contains(it.id) }
            .map { it.folderName }
            .distinct()
            .sorted()
        if (baseSongsPool.any { hiddenSongIds.contains(it.id) }) {
            normalFolders + HIDDEN_FOLDER_NAME
        } else {
            normalFolders
        }
    }

    val visibleFilters = remember(hideDemoTracks, hiddenSongIds) {
        LibraryFilter.entries.filter { filter ->
            when (filter) {
                LibraryFilter.DEMO -> !hideDemoTracks
                LibraryFilter.HIDDEN -> true
                else -> true
            }
        }
    }

    val filteredSongs = remember(
        baseSongsPool,
        favoriteIds,
        hiddenSongIds,
        selectedFilter,
        selectedSort,
        selectedFolder,
        searchQuery
    ) {
        val alphaComparator = AlphaNumericComparator()
        val unhiddenPool = baseSongsPool.filter { !hiddenSongIds.contains(it.id) }
        val hiddenPool = baseSongsPool.filter { hiddenSongIds.contains(it.id) }

        val base = when (selectedFilter) {
            LibraryFilter.ALL -> unhiddenPool
            LibraryFilter.FAVORITES -> unhiddenPool.filter { favoriteIds.contains(it.id) }
            LibraryFilter.RECENT -> unhiddenPool.sortedByDescending { it.dateAddedSec }
            LibraryFilter.FOLDERS -> {
                val targetFolder = selectedFolder ?: availableFolders.firstOrNull()
                when (targetFolder) {
                    null -> unhiddenPool
                    HIDDEN_FOLDER_NAME -> hiddenPool
                    else -> unhiddenPool.filter { it.folderName == targetFolder }
                }
            }
            LibraryFilter.LOCAL -> unhiddenPool.filter { it.isLocal }
            LibraryFilter.DEMO -> unhiddenPool.filter { !it.isLocal }
            LibraryFilter.HIDDEN -> hiddenPool
        }
        val searched = if (searchQuery.isBlank()) {
            base
        } else {
            val q = searchQuery.trim().lowercase()
            base.filter {
                it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q) ||
                    it.folderName.lowercase().contains(q)
            }
        }
        if (selectedFilter == LibraryFilter.RECENT && selectedSort == SortOption.TITLE_ASC) {
            searched
        } else {
            when (selectedSort) {
                SortOption.TITLE_ASC -> searched.sortedWith { a, b -> alphaComparator.compare(a.title, b.title) }
                SortOption.TITLE_DESC -> searched.sortedWith { a, b -> alphaComparator.compare(b.title, a.title) }
                SortOption.DATE_DESC -> searched.sortedByDescending { it.dateAddedSec }
                SortOption.ARTIST -> searched.sortedWith { a, b -> alphaComparator.compare(a.artist, b.artist) }
                SortOption.DURATION -> searched.sortedByDescending { it.durationMs }
            }
        }
    }

    val baseSurface = MaterialTheme.colorScheme.surface
    val topHeaderShape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    val smoothFling = rememberSmoothFlingBehavior()

    val bubbleModifier: @Composable (CornerBasedShape) -> Modifier = { shape ->
        Modifier.liquidGlassBubble(
            shape = shape,
            hazeState = hazeState,
            enableBlur = enableBlur,
            isLiquidGlass = useLiquidGlass
        )
    }

    val headerBlurModifier = if (enableBlur) {
        Modifier.hazeEffect(state = hazeState) {
            blurEnabled = true
            backgroundColor = baseSurface
            blurRadius = 30.dp
            tints = listOf(HazeTint(baseSurface.copy(alpha = 0.52f)))
        }
    } else {
        Modifier.background(
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                    MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
                    MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
                    Color.Transparent
                )
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .hazeSource(state = hazeState)
                .appBackdropSource()
        ) {
            if (filteredSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = headerHeightDp, bottom = 140.dp, start = 32.dp, end = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (selectedFilter == LibraryFilter.HIDDEN) Icons.Filled.VisibilityOff else Icons.Filled.MusicOff,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = when {
                                searchQuery.isNotBlank() -> "«$searchQuery» — ${strings.emptyTrackList}"
                                selectedFilter == LibraryFilter.HIDDEN -> strings.noTracksInHidden
                                selectedFilter == LibraryFilter.FAVORITES -> strings.noTracksInFavorites
                                selectedFilter == LibraryFilter.LOCAL -> strings.noTracksLocal
                                else -> strings.emptyTrackList
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    flingBehavior = smoothFling,
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = headerHeightDp + 12.dp,
                        bottom = 185.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!hasStoragePermission) {
                        item(key = "perm_card") {
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = strings.deviceMusicTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = strings.deviceMusicPermissionDesc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                    FilledTonalButton(onClick = onRequestPermissionAndScan) {
                                        Text(strings.grantPermission)
                                    }
                                }
                            }
                        }
                    }

                    itemsIndexed(
                        items = filteredSongs,
                        key = { _, s -> s.id },
                        contentType = { _, _ -> "song_row" }
                    ) { index, song ->
                        SongRow(
                            song = song,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = isPlaying,
                            isFavorite = favoriteIds.contains(song.id),
                            onClick = { onPlayQueue(filteredSongs, index, false) },
                            onToggleFavorite = { onToggleFavorite(song) },
                            onMoreClick = { onSongMoreClick(song) }
                        )
                    }
                }
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
                val topScrimColor = MaterialTheme.colorScheme.background
                if (enableBlur) {
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    @Composable
                    fun MoreMenuDropdown() {
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.playAll) },
                                leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onPlayQueue(filteredSongs, 0, false)
                                },
                                enabled = filteredSongs.isNotEmpty()
                            )
                            DropdownMenuItem(
                                text = { Text(strings.shuffleAll) },
                                leadingIcon = { Icon(Icons.Filled.Shuffle, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onPlayQueue(filteredSongs, 0, true)
                                },
                                enabled = filteredSongs.isNotEmpty()
                            )
                            DropdownMenuItem(
                                text = { Text(strings.rescan) },
                                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onRequestPermissionAndScan()
                                },
                                enabled = !isScanningLocal
                            )
                        }
                    }

                    @Composable
                    fun MoreButtonBubble() {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    showMoreMenu = true
                                }
                                .then(bubbleModifier(CircleShape)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = strings.actions,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            MoreMenuDropdown()
                        }
                    }

                    @Composable
                    fun SortButtonBubble() {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    showSortMenu = true
                                }
                                .then(bubbleModifier(CircleShape)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = strings.sorting,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.getDisplayName(strings)) },
                                        leadingIcon = {
                                            if (selectedSort == option) {
                                                Icon(Icons.Filled.Check, contentDescription = null)
                                            }
                                        },
                                        onClick = {
                                            onSelectSort(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showTopSearchBar) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                        isTopSearchExpanded = !isTopSearchExpanded
                                        if (!isTopSearchExpanded) searchQuery = ""
                                    }
                                    .then(bubbleModifier(CircleShape)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isTopSearchExpanded) Icons.Filled.Close else Icons.Filled.Search,
                                    contentDescription = strings.searchTitle,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            MoreButtonBubble()
                        }

                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .then(bubbleModifier(RoundedCornerShape(22.dp)))
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.navLibrary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        if (showTopSearchBar) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MoreButtonBubble()
                                SortButtonBubble()
                            }
                        } else {
                            SortButtonBubble()
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    onOpenYtMusic()
                                }
                                .then(bubbleModifier(RoundedCornerShape(20.dp)))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "YouTube Music",
                                            tint = Color(0xFF111111),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "YouTube Music",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    onOpenTagFix()
                                }
                                .then(bubbleModifier(RoundedCornerShape(20.dp)))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoFixHigh,
                                    contentDescription = "TagFix",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "TagFix",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    onOpenSoundCloud()
                                }
                                .then(bubbleModifier(RoundedCornerShape(20.dp)))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(com.lormusic.app.R.drawable.ic_soundcloud),
                                    contentDescription = "SoundCloud",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "SoundCloud",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isSearchVisible) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(strings.searchPlaceholder) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = strings.clear)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(bubbleModifier(RoundedCornerShape(18.dp)))
                        )
                    }
                }
            }
        }
    }
