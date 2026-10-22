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
import com.lormusic.app.ui.components.isLiquidGlassSupported
import com.lormusic.app.ui.components.liquidGlass
import com.lormusic.app.ui.components.liquidGlassBubble
import com.lormusic.app.ui.components.rememberSmoothFlingBehavior
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

private const val HIDDEN_FOLDER_NAME = "Скрытые"

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
    onOpenTagFix: () -> Unit,
    onOpenYtMusic: () -> Unit = {},
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
    var headerHeightDp by remember { mutableStateOf(144.dp) }

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
        Modifier.background(baseSurface.copy(alpha = 0.96f))
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .hazeSource(state = hazeState)
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
                                searchQuery.isNotBlank() -> "По запросу «$searchQuery» ничего не найдено"
                                selectedFilter == LibraryFilter.HIDDEN -> "В папке «Скрытые» пока нет треков"
                                selectedFilter == LibraryFilter.FAVORITES -> "В «Избранном» пока нет треков"
                                selectedFilter == LibraryFilter.LOCAL -> "Локальные аудиофайлы не найдены"
                                else -> "Список треков пуст"
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
                        top = headerHeightDp + 8.dp,
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
                                            text = "Музыка с устройства",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Разрешите доступ к аудиофайлам",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                    FilledTonalButton(onClick = onRequestPermissionAndScan) {
                                        Text("Подключить")
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

        if (!classicLibraryDesign) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        headerHeightDp = with(density) { coords.size.height.toDp() }
                    }
            ) {
                if (enableBlur) {
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                            contentDescription = "Поиск",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .then(bubbleModifier(RoundedCornerShape(22.dp)))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.navLibrary,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            Row(
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
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.PlayArrow,
                                                    contentDescription = "YT Music",
                                                    tint = Color(0xFF111111),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "YT Music",
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
                                        .padding(horizontal = 12.dp),
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
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
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
                                    contentDescription = "Действия",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )

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

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
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
                                    contentDescription = "Сортировка",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )

                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    SortOption.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.label) },
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
                    }

                    AnimatedVisibility(visible = isSearchVisible) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Поиск по названию, артисту, папке…") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Очистить")
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
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        headerHeightDp = with(density) { coords.size.height.toDp() }
                    }
                    .shadow(
                        elevation = 16.dp,
                        shape = topHeaderShape,
                        ambientColor = Color.Black.copy(alpha = 0.55f),
                        spotColor = Color.Black.copy(alpha = 0.55f)
                    )
                    .clip(topHeaderShape)
                    .then(headerBlurModifier)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.06f),
                                Color.White.copy(alpha = 0.20f)
                            )
                        ),
                        shape = topHeaderShape
                    )
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 10.dp, top = 6.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Медиатека",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )

                    if (showTopSearchBar) {
                        FilledTonalIconButton(
                            onClick = {
                                isTopSearchExpanded = !isTopSearchExpanded
                                if (!isTopSearchExpanded) searchQuery = ""
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isTopSearchExpanded) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = "Поиск",
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }

                    AssistChip(
                        onClick = onOpenTagFix,
                        label = { Text("TagFix") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = "TagFix",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(Modifier.width(6.dp))

                    AssistChip(
                        onClick = { showSortMenu = true },
                        label = {
                            val shortLabel = when (selectedSort) {
                                SortOption.TITLE_ASC -> "А–Я"
                                SortOption.TITLE_DESC -> "Я–А"
                                SortOption.DATE_DESC -> "Новые"
                                SortOption.ARTIST -> "Артист"
                                SortOption.DURATION -> "Время"
                            }
                            Text(shortLabel)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Сортировка",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(14.dp)
                    )

                    IconButton(
                        onClick = onRequestPermissionAndScan,
                        enabled = !isScanningLocal,
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (isScanningLocal) {
                            ExpressiveLoadingIndicator(size = 24.dp)
                        } else {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Сканировать память",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box {
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
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

                AnimatedVisibility(visible = isSearchVisible) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Поиск по названию, артисту, папке…") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Очистить")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (filteredSongs.isNotEmpty()) {
                        Button(
                            onClick = { onPlayQueue(filteredSongs, 0, false) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Слушать (${filteredSongs.size})", style = MaterialTheme.typography.labelMedium)
                        }
                        FilledTonalButton(
                            onClick = { onPlayQueue(filteredSongs, 0, true) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Микс", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    visibleFilters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { onSelectFilter(filter) },
                            label = {
                                val labelText = if (filter == LibraryFilter.HIDDEN && hiddenSongIds.isNotEmpty()) {
                                    "${filter.label} (${hiddenSongIds.size})"
                                } else {
                                    filter.label
                                }
                                Text(labelText, style = MaterialTheme.typography.labelMedium)
                            },
                            leadingIcon = if (filter == LibraryFilter.HIDDEN) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.VisibilityOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else null,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = selectedFilter == LibraryFilter.FOLDERS && availableFolders.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val activeFolder = selectedFolder ?: availableFolders.firstOrNull()
                        availableFolders.forEach { folder ->
                            val isHiddenFolder = folder == HIDDEN_FOLDER_NAME
                            AssistChip(
                                onClick = { selectedFolder = folder },
                                label = { Text(folder, style = MaterialTheme.typography.labelMedium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isHiddenFolder) Icons.Filled.VisibilityOff else Icons.Filled.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                colors = if (activeFolder == folder) {
                                    AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                } else {
                                    AssistChipDefaults.assistChipColors()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
}
