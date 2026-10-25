package com.lormusic.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lormusic.app.model.LocalStrings
import com.lormusic.app.model.SearchBarPlacement
import com.lormusic.app.model.Song
import com.lormusic.app.model.ThemeMode
import com.lormusic.app.model.resolveStrings
import com.lormusic.app.ui.components.LocalPureMaterialYou
import com.lormusic.app.ui.components.*
import com.lormusic.app.ui.components.backdrop.backdrops.layerBackdrop
import com.lormusic.app.ui.components.backdrop.backdrops.rememberLayerBackdrop
import com.lormusic.app.ui.screens.*
import com.lormusic.app.ui.theme.LorMusicTheme
import com.lormusic.app.viewmodel.PlayerViewModel
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by playerViewModel.themeMode
            val useDynamicColor by playerViewModel.useDynamicColor
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            LorMusicTheme(
                darkTheme = darkTheme,
                dynamicColor = useDynamicColor
            ) {
                LorMusicMainContent(playerViewModel)
            }
        }
    }
}

@Composable
fun LorMusicMainContent(playerViewModel: PlayerViewModel) {
    val context = LocalContext.current
    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, requiredPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStoragePermission = granted
        if (granted) {
            playerViewModel.rescanLocalMusic()
        }
    }

    val deleteIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        playerViewModel.onDeviceDeletePermissionResult(result.resultCode == Activity.RESULT_OK)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var showBackupNoticeDialog by remember { mutableStateOf(!playerViewModel.hasSeenBackupNotice()) }

    var selectedTab by remember { mutableStateOf(LorTab.LIBRARY) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var songForActionsSheet by remember { mutableStateOf<Song?>(null) }
    var songForTagEditor by remember { mutableStateOf<Song?>(null) }
    var showTagFixSheet by remember { mutableStateOf(false) }
    var isBottomLocalSearchActive by remember { mutableStateOf(false) }

    var showLaunchSplash by rememberSaveable { mutableStateOf(true) }

    val allSongs by playerViewModel.allSongs
    val localSongsCount by playerViewModel.localSongsCount
    val isScanningLocal by playerViewModel.isScanningLocal
    val favoriteIds by playerViewModel.favoriteIds
    val hiddenSongIds by playerViewModel.hiddenSongIds
    val selectedFilter by playerViewModel.selectedFilter
    val selectedSort by playerViewModel.selectedSort
    val selectedAppIcon by playerViewModel.selectedAppIcon
    val currentSong by playerViewModel.currentSong
    val isPlaying by playerViewModel.isPlaying
    val positionMs by playerViewModel.positionMs
    val durationMs by playerViewModel.durationMs
    val queue by playerViewModel.queue
    val shuffleOn by playerViewModel.shuffleOn
    val repeatMode by playerViewModel.repeatMode
    val playbackSpeed by playerViewModel.playbackSpeed
    val sleepTimerRemainingSec by playerViewModel.sleepTimerRemainingSec
    val themeMode by playerViewModel.themeMode
    val useDynamicColor by playerViewModel.useDynamicColor
    val useSquigglySlider by playerViewModel.useSquigglySlider
    val enableBlur by playerViewModel.enableBlur
    val pureMaterialYou by playerViewModel.pureMaterialYou
    val searchBarPlacement by playerViewModel.searchBarPlacement
    val enablePlayerSwipe by playerViewModel.enablePlayerSwipe
    val hideDemoTracks by playerViewModel.hideDemoTracks
    val currentLyrics by playerViewModel.currentLyrics
    val isLoadingLyrics by playerViewModel.isLoadingLyrics
    val showUpdateDialog by playerViewModel.showUpdateDialog
    val availableUpdateInfo by playerViewModel.availableUpdateInfo
    val updateState by playerViewModel.updateState
    val statusMessage by playerViewModel.statusMessage

    val isLocalSearchTop = searchBarPlacement == SearchBarPlacement.LOCAL_TOP_YT_BOTTOM ||
        searchBarPlacement == SearchBarPlacement.ALWAYS_TOP
    val isYtSearchTop = searchBarPlacement == SearchBarPlacement.LOCAL_BOTTOM_YT_TOP ||
        searchBarPlacement == SearchBarPlacement.ALWAYS_TOP

    val isYtMusicMode by playerViewModel.isYtMusicMode
    val activeYtTab by playerViewModel.activeYtTab
    val ytRecommendations by playerViewModel.ytRecommendations
    val ytRadioMixes by playerViewModel.ytRadioMixes
    val ytMyWaveSongs by playerViewModel.ytMyWaveSongs
    val ytSearchResults by playerViewModel.ytSearchResults
    val isLoadingYtFeed by playerViewModel.isLoadingYtFeed
    val isSearchingYt by playerViewModel.isSearchingYt
    val downloadingSongIds by playerViewModel.downloadingSongIds
    val savedYtSongs by playerViewModel.savedYtSongs
    val googleAccount by playerViewModel.googleAccount

    val playlists = playerViewModel.getAllPlaylistsWithFavorites()

    BackHandler(enabled = isYtMusicMode && !showNowPlaying) {
        if (activeYtTab != YtMusicTab.HOME) {
            playerViewModel.selectYtTab(YtMusicTab.HOME)
        } else {
            playerViewModel.exitYtMusicMode()
        }
    }

    val hazeState = remember { HazeState() }
    val useLiquidGlass = playerViewModel.useLiquidGlass.value
    val effectiveEnableBlur = enableBlur && !pureMaterialYou
    val effectiveUseLiquidGlass = useLiquidGlass && !pureMaterialYou
    val isGlassActive = effectiveUseLiquidGlass && isLiquidGlassSupported()
    val windowBackground = MaterialTheme.colorScheme.background
    val paintBackdrop: ContentDrawScope.() -> Unit = remember(windowBackground) {
        {
            drawRect(windowBackground)
            drawContent()
        }
    }
    val appBackdrop = rememberLayerBackdrop(onDraw = paintBackdrop)
    val appLanguage by playerViewModel.appLanguage
    val currentStrings = remember(appLanguage) { resolveStrings(appLanguage) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            playerViewModel.clearStatusMessage()
        }
    }

    CompositionLocalProvider(
        LocalAppBackdrop provides appBackdrop,
        LocalLiquidGlassEnabled provides isGlassActive,
        LocalPureMaterialYou provides pureMaterialYou,
        LocalStrings provides currentStrings
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .then(if (isGlassActive) Modifier.layerBackdrop(appBackdrop) else Modifier)
                ) {
                    AnimatedContent(
                        targetState = isYtMusicMode,
                        transitionSpec = {
                            (fadeIn(tween(260)) + scaleIn(initialScale = 0.97f, animationSpec = tween(260)))
                                .togetherWith(fadeOut(tween(200)) + scaleOut(targetScale = 0.97f, animationSpec = tween(200)))
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "modeSwitchTransition"
                    ) { ytMode ->
                        if (ytMode) {
                            YouTubeMusicScreen(
                                activeTab = activeYtTab,
                                recentlyPlayed = playerViewModel.getRecentlyPlayedSongs(),
                                recommendations = ytRecommendations,
                                radioMixes = ytRadioMixes,
                                myWaveSongs = ytMyWaveSongs,
                                searchResults = ytSearchResults,
                                isLoadingFeed = isLoadingYtFeed,
                                isSearching = isSearchingYt,
                                downloadingSongIds = downloadingSongIds,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                favoriteIds = favoriteIds,
                                playlists = playlists,
                                savedYtSongs = savedYtSongs,
                                googleAccount = googleAccount,
                                hazeState = hazeState,
                                enableBlur = effectiveEnableBlur,
                                useLiquidGlass = effectiveUseLiquidGlass,
                                showTopSearchBar = isYtSearchTop,
                                recentsViewMode = playerViewModel.recentsViewMode.value,
                                onToggleRecentsViewMode = playerViewModel::setRecentsViewMode,
                            onSelectTab = playerViewModel::selectYtTab,
                            onPlayQueue = { list, idx, shuffle ->
                                playerViewModel.setQueueAndPlay(list, idx, shuffle)
                            },
                            onTogglePlay = playerViewModel::togglePlayPause,
                            onToggleFavorite = playerViewModel::toggleFavorite,
                            onDownloadSongWithTags = playerViewModel::downloadYtSongWithTags,
                            onSearchQuery = playerViewModel::searchYtMusic,
                            onRefreshFeed = playerViewModel::refreshYtHomeFeed,
                            onRefreshMyWave = playerViewModel::refreshYtMyWave,
                            onSignInWithCookie = playerViewModel::signInWithCookie,
                            onSignOutGoogle = playerViewModel::signOutGoogle,
                            onSyncGoogleLibrary = playerViewModel::syncGoogleLibrary,
                            onCreatePlaylist = playerViewModel::createPlaylist,
                            onDeletePlaylist = playerViewModel::deletePlaylist,
                            onSongMoreClick = { songForActionsSheet = it },
                            onSwitchToLocal = {
                                playerViewModel.exitYtMusicMode()
                                selectedTab = LorTab.LIBRARY
                            }
                        )
                    } else {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                val forward = targetState.ordinal >= initialState.ordinal
                                val enterSlide = slideInHorizontally(
                                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                                    initialOffsetX = { if (forward) it / 8 else -it / 8 }
                                ) + fadeIn(tween(220))
                                val exitSlide = slideOutHorizontally(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                                    targetOffsetX = { if (forward) -it / 8 else it / 8 }
                                ) + fadeOut(tween(180))
                                enterSlide.togetherWith(exitSlide)
                            },
                            modifier = Modifier.fillMaxSize(),
                            label = "tabTransition"
                        ) { tab ->
                            when (tab) {
                                LorTab.LIBRARY -> LibraryScreen(
                                    songs = allSongs,
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    favoriteIds = favoriteIds,
                                    hiddenSongIds = hiddenSongIds,
                                    hideDemoTracks = hideDemoTracks,
                                    enableBlur = effectiveEnableBlur,
                                    useLiquidGlass = effectiveUseLiquidGlass,
                                    classicLibraryDesign = playerViewModel.classicLibraryDesign.value,
                                    showTopSearchBar = isLocalSearchTop,
                                    externalSearchActive = isBottomLocalSearchActive,
                                    selectedFilter = selectedFilter,
                                    selectedSort = selectedSort,
                                    hasStoragePermission = hasStoragePermission,
                                    isScanningLocal = isScanningLocal,
                                    hazeState = hazeState,
                                    onSelectFilter = playerViewModel::setLibraryFilter,
                                    onSelectSort = playerViewModel::setSortOption,
                                    onRequestPermissionAndScan = {
                                        if (hasStoragePermission) {
                                            playerViewModel.rescanLocalMusic()
                                        } else {
                                            permissionLauncher.launch(requiredPermission)
                                        }
                                    },
                                    onPlayQueue = { list, idx, shuffle ->
                                        playerViewModel.setQueueAndPlay(list, idx, shuffle)
                                    },
                                    onToggleFavorite = playerViewModel::toggleFavorite,
                                    onSongMoreClick = { songForActionsSheet = it },
                                    onOpenTagFix = { showTagFixSheet = true },
                                    onOpenYtMusic = { playerViewModel.enterYtMusicMode() }
                                )

                                LorTab.PLAYLISTS -> Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .hazeSource(state = hazeState)
                                ) {
                                    PlaylistsScreen(
                                        playlists = playlists,
                                        allSongs = allSongs.filter { !hiddenSongIds.contains(it.id) },
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        favoriteIds = favoriteIds,
                                        onPlayQueue = { list, idx, shuffle ->
                                            playerViewModel.setQueueAndPlay(list, idx, shuffle)
                                        },
                                        onCreatePlaylist = playerViewModel::createPlaylist,
                                        onDeletePlaylist = playerViewModel::deletePlaylist,
                                        onAddSongToPlaylist = playerViewModel::addSongToPlaylist,
                                        onAddSongsToPlaylist = playerViewModel::addSongsToPlaylist,
                                        onRemoveSongFromPlaylist = playerViewModel::removeSongFromPlaylist,
                                        onToggleFavorite = playerViewModel::toggleFavorite,
                                        hazeState = hazeState,
                                        enableBlur = effectiveEnableBlur,
                                        useLiquidGlass = effectiveUseLiquidGlass
                                    )
                                }

                                LorTab.SETTINGS -> Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .hazeSource(state = hazeState)
                                ) {
                                    val visibleTotalSongs = remember(allSongs, hideDemoTracks, hiddenSongIds) {
                                        allSongs.count {
                                            (!hideDemoTracks || it.isLocal) && !hiddenSongIds.contains(it.id)
                                        }
                                    }
                                    ProfileScreen(
                                        totalSongsCount = visibleTotalSongs,
                                        localSongsCount = localSongsCount,
                                        favoritesCount = favoriteIds.size,
                                        playlistsCount = playlists.size,
                                        themeMode = themeMode,
                                        useDynamicColor = useDynamicColor,
                                        useSquigglySlider = useSquigglySlider,
                                        enableBlur = enableBlur,
                                        pureMaterialYou = pureMaterialYou,
                                        showNavBarLabels = playerViewModel.showNavBarLabels.value,
                                        useLiquidGlass = playerViewModel.useLiquidGlass.value,
                                        classicLibraryDesign = playerViewModel.classicLibraryDesign.value,
                                        useDynamicPlayerColors = playerViewModel.useDynamicPlayerColors.value,
                                        enableBetaUpdates = playerViewModel.enableBetaUpdates.value,
                                        useAlbumArtGradient = playerViewModel.useAlbumArtGradient.value,
                                        autoTagFixEnabled = playerViewModel.autoTagFixEnabled.value,
                                        appLanguage = appLanguage,
                                        searchBarPlacement = searchBarPlacement,
                                        enablePlayerSwipe = enablePlayerSwipe,
                                        hideDemoTracks = hideDemoTracks,
                                        selectedAppIcon = selectedAppIcon,
                                        playbackSpeed = playbackSpeed,
                                        sleepTimerRemainingSec = sleepTimerRemainingSec,
                                        onChangeThemeMode = playerViewModel::setThemeMode,
                                        onToggleDynamicColor = playerViewModel::setUseDynamicColor,
                                        onToggleSquigglySlider = playerViewModel::setUseSquigglySlider,
                                        onToggleBlur = playerViewModel::setEnableBlur,
                                        onTogglePureMaterialYou = playerViewModel::setPureMaterialYou,
                                        onToggleShowNavBarLabels = playerViewModel::setShowNavBarLabels,
                                        onToggleLiquidGlass = playerViewModel::setUseLiquidGlass,
                                        onToggleClassicLibraryDesign = playerViewModel::setClassicLibraryDesign,
                                        onToggleDynamicPlayerColors = playerViewModel::setUseDynamicPlayerColors,
                                        onToggleBetaUpdates = playerViewModel::setEnableBetaUpdates,
                                        onToggleAlbumArtGradient = playerViewModel::setUseAlbumArtGradient,
                                        onToggleAutoTagFix = playerViewModel::setAutoTagFixEnabled,
                                        onSelectLanguage = playerViewModel::setAppLanguage,
                                        onChangeSearchBarPlacement = playerViewModel::setSearchBarPlacement,
                                        onTogglePlayerSwipe = playerViewModel::setEnablePlayerSwipe,
                                        onToggleHideDemoTracks = playerViewModel::setHideDemoTracks,
                                        onSelectAppIcon = playerViewModel::setAppIconOption,
                                        onChangePlaybackSpeed = playerViewModel::setPlaybackSpeed,
                                        onSetSleepTimer = playerViewModel::setSleepTimer,
                                        onRescanLocalMusic = {
                                            if (hasStoragePermission) {
                                                playerViewModel.rescanLocalMusic()
                                            } else {
                                                permissionLauncher.launch(requiredPermission)
                                            }
                                        },
                                        onOpenTagFix = { showTagFixSheet = true },
                                        onExportBackup = playerViewModel::exportBackup,
                                        onImportBackup = playerViewModel::importBackup,
                                        onCheckForUpdates = { playerViewModel.checkForUpdates(silent = false) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                if (enableBlur) {
                    val bottomScrimColor = MaterialTheme.colorScheme.background
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .hazeEffect(state = hazeState) {
                                blurEnabled = true
                                backgroundColor = bottomScrimColor
                                blurRadius = 32.dp
                                tints = listOf(HazeTint(bottomScrimColor.copy(alpha = 0.65f)))
                            }
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        bottomScrimColor.copy(alpha = 0.40f),
                                        bottomScrimColor.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        snackbar = { data ->
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = data.visuals.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                data.visuals.actionLabel?.let { actionLabel ->
                                    TextButton(
                                        onClick = { data.performAction() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = actionLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
                    AnimatedVisibility(
                        visible = downloadingSongIds.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        val activeSongId = downloadingSongIds.firstOrNull()
                        val activeSong = activeSongId?.let { id ->
                            playerViewModel.downloadingSongs.value[id]
                                ?: allSongs.firstOrNull { it.id == id }
                                ?: savedYtSongs.firstOrNull { it.id == id }
                                ?: ytSearchResults.firstOrNull { it.id == id }
                                ?: ytRecommendations.firstOrNull { it.id == id }
                                ?: currentSong?.takeIf { it.id == id }
                        }
                        val progressFrac = activeSongId?.let { playerViewModel.downloadProgress.value[it] } ?: 0f
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = activeSong?.let { "${currentStrings.downloadingSong}: ${it.title}" }
                                            ?: currentStrings.downloadingSong,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (progressFrac > 0f) {
                                        Text(
                                            text = "${(progressFrac * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                WavyLinearProgressIndicator(
                                    progress = if (progressFrac > 0f) progressFrac else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(16.dp),
                                    activeColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = currentSong != null,
                        enter = fadeIn(tween(220)) + slideInVertically { it / 2 },
                        exit = fadeOut(tween(180)) + slideOutVertically { it / 2 }
                    ) {
                        currentSong?.let { song ->
                            val effectiveDur = (if (durationMs > 0) durationMs else song.durationMs).coerceAtLeast(1L)
                            MiniPlayer(
                                song = song,
                                isPlaying = isPlaying,
                                isFavorite = favoriteIds.contains(song.id),
                                progress = (positionMs.toFloat() / effectiveDur.toFloat()).coerceIn(0f, 1f),
                                hazeState = hazeState,
                                enableBlur = effectiveEnableBlur,
                                useLiquidGlass = effectiveUseLiquidGlass,
                                enableSwipe = enablePlayerSwipe,
                                onTogglePlay = playerViewModel::togglePlayPause,
                                onPrevious = playerViewModel::skipPrevious,
                                onNext = playerViewModel::skipNext,
                                onToggleFavorite = { playerViewModel.toggleFavorite(song) },
                                onOpen = { showNowPlaying = true }
                            )
                        }
                    }

                    if (isYtMusicMode) {
                        YtMusicBottomNavBar(
                            selected = activeYtTab,
                            onSelect = { tab ->
                                playerViewModel.selectYtTab(tab)
                            },
                            hazeState = hazeState,
                            enableBlur = effectiveEnableBlur,
                            useLiquidGlass = effectiveUseLiquidGlass,
                            showBottomSearchButton = !isYtSearchTop,
                            showLabels = playerViewModel.showNavBarLabels.value
                        )
                    } else {
                        LorBottomNavBar(
                            selected = selectedTab,
                            onSelect = { tab ->
                                selectedTab = tab
                            },
                            hazeState = hazeState,
                            enableBlur = effectiveEnableBlur,
                            useLiquidGlass = effectiveUseLiquidGlass,
                            showBottomSearchButton = !isLocalSearchTop,
                            showLabels = playerViewModel.showNavBarLabels.value,
                            isLocalSearchActive = !isLocalSearchTop && isBottomLocalSearchActive && selectedTab == LorTab.LIBRARY,
                            onToggleLocalSearch = {
                                if (selectedTab != LorTab.LIBRARY) {
                                    selectedTab = LorTab.LIBRARY
                                    isBottomLocalSearchActive = true
                                } else {
                                    isBottomLocalSearchActive = !isBottomLocalSearchActive
                                }
                            }
                        )
                    }
                }
            }

    if (showBackupNoticeDialog) {
        AlertDialog(
            onDismissRequest = {
                playerViewModel.dismissBackupNotice()
                showBackupNoticeDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(currentStrings.backupWarningTitle)
                }
            },
            text = {
                Text(
                    text = currentStrings.backupWarningMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        playerViewModel.dismissBackupNotice()
                        showBackupNoticeDialog = false
                        val json = playerViewModel.exportBackup()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("LorMusic Backup", json)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, currentStrings.backupCopiedToast, android.widget.Toast.LENGTH_LONG).show()
                        try {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, json)
                                putExtra(Intent.EXTRA_SUBJECT, "LorMusic-Settings-Backup.json")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, currentStrings.shareBackup))
                        } catch (_: Exception) {}
                    }
                ) {
                    Text(currentStrings.createBackup)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        playerViewModel.dismissBackupNotice()
                        showBackupNoticeDialog = false
                    }
                ) {
                    Text(currentStrings.noThanks)
                }
            }
        )
    }

            AnimatedVisibility(
                visible = showNowPlaying && currentSong != null,
                enter = slideInVertically(
                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                    initialOffsetY = { it }
                ) + fadeIn(tween(240)),
                exit = slideOutVertically(
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    targetOffsetY = { it }
                ) + fadeOut(tween(200))
            ) {
                currentSong?.let { song ->
                    Surface(modifier = Modifier.fillMaxSize()) {
                        NowPlayingScreen(
                            song = song,
                            isPlaying = isPlaying,
                            isFavorite = favoriteIds.contains(song.id),
                            positionMs = positionMs,
                            durationMs = durationMs,
                            shuffleOn = shuffleOn,
                            repeatMode = repeatMode,
                            playbackSpeed = playbackSpeed,
                            sleepTimerRemainingSec = sleepTimerRemainingSec,
                            queue = queue,
                            lyrics = currentLyrics,
                            isLoadingLyrics = isLoadingLyrics,
                            useSquigglySlider = useSquigglySlider,
                            useAlbumArtGradient = playerViewModel.useAlbumArtGradient.value,
                            enableSwipe = enablePlayerSwipe,
                            useDynamicPlayerColors = playerViewModel.useDynamicPlayerColors.value,
                            playlists = playlists,
                            isDownloading = downloadingSongIds.contains(song.id),
                            downloadProgressFraction = playerViewModel.downloadProgress.value[song.id] ?: 0f,
                            onAddToPlaylist = { plId, targetSong -> playerViewModel.addSongToPlaylist(plId, targetSong) },
                            onRemoveFromPlaylist = { plId, songId -> playerViewModel.removeSongFromPlaylist(plId, songId) },
                            onCreatePlaylist = { name -> playerViewModel.createPlaylist(name, listOf(song)) },
                            onDownloadTrack = { playerViewModel.downloadYtSongWithTags(song) },
                            onTogglePlay = playerViewModel::togglePlayPause,
                            onNext = playerViewModel::skipNext,
                            onPrevious = playerViewModel::skipPrevious,
                            onSeek = playerViewModel::seekTo,
                            onToggleShuffle = playerViewModel::toggleShuffle,
                            onCycleRepeat = playerViewModel::cycleRepeatMode,
                            onToggleFavorite = { playerViewModel.toggleFavorite(song) },
                            onCycleSpeed = {
                                val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                val nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                                playerViewModel.setPlaybackSpeed(speeds[nextIdx])
                            },
                            onSelectSleepTimer = playerViewModel::setSleepTimer,
                            onPlayFromQueue = { idx ->
                                playerViewModel.setQueueAndPlay(queue, idx, false)
                            },
                            onRemoveFromQueue = playerViewModel::removeQueueItemAt,
                            onRefreshLyrics = { playerViewModel.loadLyricsFor(song) },
                            onOpenSongActions = { songForActionsSheet = song },
                            onOpenTagEditor = { songForTagEditor = song },
                            onOpenArtistProfile = playerViewModel::openArtistProfile,
                            onClose = { showNowPlaying = false }
                        )
                    }
                }
            }

            if (showLaunchSplash) {
                MaterialYouLaunchSplash(
                    iconOption = selectedAppIcon
                )
            }
        }
    }

    songForActionsSheet?.let { targetSong ->
        SongActionsBottomSheet(
            song = targetSong,
            isFavorite = favoriteIds.contains(targetSong.id),
            isHidden = hiddenSongIds.contains(targetSong.id),
            playlists = playlists,
            onDismiss = { songForActionsSheet = null },
            onPlayNow = { playerViewModel.playSong(targetSong) },
            onPlayNext = { playerViewModel.playNextInQueue(targetSong) },
            onToggleFavorite = { playerViewModel.toggleFavorite(targetSong) },
            onOpenTagEditor = { songForTagEditor = targetSong },
            onDownloadSong = { playerViewModel.downloadYtSongWithTags(targetSong) },
            onToggleHidden = { playerViewModel.toggleHiddenSong(targetSong) },
            onDeleteFromDevice = {
                playerViewModel.deleteSongFromDevice(targetSong) { intentSender ->
                    deleteIntentLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
            },
            onAddToPlaylist = { playlistId ->
                playerViewModel.addSongToPlaylist(playlistId, targetSong)
            },
            onCreateNewPlaylistWithSong = { name ->
                playerViewModel.createPlaylist(name, listOf(targetSong))
            },
            onOpenArtistProfile = playerViewModel::openArtistProfile
        )
    }

    songForTagEditor?.let { editSong ->
        TagEditorSheet(
            song = editSong,
            onDismiss = { songForTagEditor = null },
            onSave = { override ->
                playerViewModel.saveSongTagOverride(override)
                songForTagEditor = null
            }
        )
    }

    val selectedArtist = playerViewModel.selectedArtistProfile.value
    if (selectedArtist != null) {
        ArtistProfileSheet(
            artistName = selectedArtist,
            allSongs = allSongs,
            currentSong = currentSong,
            isPlaying = isPlaying,
            favoriteIds = favoriteIds,
            onDismiss = playerViewModel::closeArtistProfile,
            onPlayQueue = { list, idx, shuffle ->
                playerViewModel.setQueueAndPlay(list, idx, shuffle)
            },
            onToggleFavorite = playerViewModel::toggleFavorite,
            onSongMoreClick = { songForActionsSheet = it },
            onSearchOnYouTube = { query ->
                playerViewModel.enterYtMusicMode()
                playerViewModel.searchYtMusic(query)
            }
        )
    }

    if (showTagFixSheet) {
        TagFixModalSheet(
            songs = allSongs,
            onDismiss = { showTagFixSheet = false },
            onApplyProposals = { proposals ->
                playerViewModel.applyTagFixProposals(proposals)
                showTagFixSheet = false
            }
        )
    }

    if (playerViewModel.showAutoTagFixPrompt.value && playerViewModel.autoTagFixDetectedSongs.value.isNotEmpty()) {
        val brokenCount = playerViewModel.autoTagFixDetectedSongs.value.size
        AlertDialog(
            onDismissRequest = { playerViewModel.dismissAutoTagFixPrompt() },
            icon = {
                Icon(
                    imageVector = Icons.Filled.ElectricBolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = currentStrings.autoTagFixPromptTitle,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = currentStrings.autoTagFixPromptMessage.format(brokenCount)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        playerViewModel.dismissAutoTagFixPrompt()
                        showTagFixSheet = true
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(currentStrings.fixNow)
                }
            },
            dismissButton = {
                TextButton(onClick = { playerViewModel.dismissAutoTagFixPrompt() }) {
                    Text(currentStrings.remindLaterBtn)
                }
            }
        )
    }

    if (showUpdateDialog && availableUpdateInfo != null) {
        UpdateDialog(
            updateInfo = availableUpdateInfo!!,
            updateState = updateState,
            onDownload = {
                playerViewModel.startDownloadingUpdate(availableUpdateInfo!!)
            },
            onInstall = { file ->
                playerViewModel.installDownloadedUpdate(file)
            },
            onDismiss = {
                playerViewModel.dismissUpdateDialog()
            },
            appLanguage = appLanguage
        )
    }
    }
}
