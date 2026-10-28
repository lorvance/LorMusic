package com.lormusic.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lormusic.app.model.AppLanguage
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
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by playerViewModel.themeMode
            val useDynamicColor by playerViewModel.useDynamicColor
            val isHalloweenMode by playerViewModel.isHalloweenMode
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            LorMusicTheme(
                darkTheme = darkTheme,
                dynamicColor = useDynamicColor && !isHalloweenMode,
                isHalloweenMode = isHalloweenMode
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
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

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
    val isSoundCloudMode by playerViewModel.isSoundCloudMode
    val activeYtTab by playerViewModel.activeYtTab
    val activeScTab by playerViewModel.activeScTab
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

    var lastBackPressTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        playerViewModel.checkAndEnforceHalloweenExpiry()
    }

    BackHandler(enabled = (isYtMusicMode || isSoundCloudMode) && !showNowPlaying) {
        if (isSoundCloudMode) {
            if (activeScTab != ScTab.HOME) {
                playerViewModel.selectScTab(ScTab.HOME)
            } else {
                playerViewModel.exitSoundCloudMode()
            }
        } else if (isYtMusicMode) {
            if (activeYtTab != YtMusicTab.HOME) {
                playerViewModel.selectYtTab(YtMusicTab.HOME)
            } else {
                playerViewModel.exitYtMusicMode()
            }
        }
    }

    val hazeState = remember { HazeState() }
    val windowBackground = MaterialTheme.colorScheme.background
    val paintBackdrop: ContentDrawScope.() -> Unit = remember(windowBackground) {
        {
            drawRect(windowBackground)
            drawContent()
        }
    }
    val appBackdrop = rememberLayerBackdrop(onDraw = paintBackdrop)
    val useLiquidGlass = playerViewModel.useLiquidGlass.value
    val effectiveEnableBlur = enableBlur && !pureMaterialYou
    val effectiveUseLiquidGlass = useLiquidGlass && !pureMaterialYou
    val isGlassActive = effectiveUseLiquidGlass && isLiquidGlassSupported()
    val appLanguage by playerViewModel.appLanguage
    val currentStrings = remember(appLanguage) { resolveStrings(appLanguage) }

    val activity = context as? android.app.Activity

    BackHandler(enabled = !isYtMusicMode && !isSoundCloudMode && !showNowPlaying) {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 2000L) {
            activity?.finish()
        } else {
            lastBackPressTime = now
            android.widget.Toast.makeText(context, currentStrings.pressBackAgainToExit, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

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
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showUpdateDialog && enableBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(24.dp)
                    } else {
                        Modifier
                    }
                ),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val topLevelMode = when {
                        isYtMusicMode -> 1
                        isSoundCloudMode -> 2
                        else -> 0
                    }
                    AnimatedContent(
                        targetState = topLevelMode,
                        transitionSpec = {
                            (fadeIn(tween(260)) + scaleIn(initialScale = 0.97f, animationSpec = tween(260)))
                                .togetherWith(fadeOut(tween(200)) + scaleOut(targetScale = 0.97f, animationSpec = tween(200)))
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "modeSwitchTransition"
                    ) { mode ->
                        when (mode) {
                            1 -> {
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
                                useMyWaveCoverGradient = playerViewModel.useMyWaveCoverGradient.value,
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
                            onSignInNativeAccount = { name, email -> playerViewModel.setGoogleAccount(name, email) },
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
                    }
                    2 -> {
                        SoundCloudScreen(
                            activeTab = activeScTab,
                            onSelectTab = { playerViewModel.selectScTab(it) },
                            charts = playerViewModel.soundCloudCharts.value,
                            likes = playerViewModel.soundCloudLikes.value,
                            searchResults = playerViewModel.soundCloudSearchResults.value,
                            savedSongs = playerViewModel.savedScSongs.value,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            isFavorite = playerViewModel::isFavorite,
                            onToggleFavorite = playerViewModel::toggleFavorite,
                            onPlayQueue = { list, idx, shuffle ->
                                playerViewModel.setQueueAndPlay(list, idx, shuffle)
                            },
                            onSearch = playerViewModel::searchSoundCloud,
                            onRefreshCharts = playerViewModel::loadSoundCloudCharts,
                            profile = playerViewModel.soundCloudProfile.value,
                            onSignInSuccess = playerViewModel::onSoundCloudAuthSuccess,
                            onSignOut = playerViewModel::signOutSoundCloud,
                            playlists = playlists,
                            onAddToPlaylist = playerViewModel::addSongToPlaylist,
                            onCreateNewPlaylistWithSong = { name, song, isUnified ->
                                playerViewModel.createPlaylist(name, listOf(song), isUnified = isUnified)
                            },
                            onSwitchToLocal = {
                                playerViewModel.isSoundCloudMode.value = false
                                selectedTab = LorTab.LIBRARY
                            },
                            isLoadingCharts = playerViewModel.isLoadingSoundCloud.value,
                            isSearching = playerViewModel.isSearchingSoundCloud.value,
                            hazeState = hazeState,
                            enableBlur = effectiveEnableBlur,
                            useLiquidGlass = effectiveUseLiquidGlass
                        )
                    }
                    else -> {
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
                                    onOpenYtMusic = { playerViewModel.enterYtMusicMode() },
                                    onOpenSoundCloud = { playerViewModel.enterSoundCloudMode() }
                                )

                                LorTab.PLAYLISTS -> PlaylistsScreen(
                                    playlists = playlists,
                                    allSongs = allSongs.filter { !hiddenSongIds.contains(it.id) },
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    favoriteIds = favoriteIds,
                                    topPlayedSong = playerViewModel.topPlayedSong.value,
                                    totalListeningTimeMs = playerViewModel.totalListeningTimeMs.value,
                                    topAlbum = playerViewModel.topAlbum.value,
                                    onPlayQueue = { list, idx, shuffle ->
                                        playerViewModel.setQueueAndPlay(list, idx, shuffle)
                                    },
                                    onCreatePlaylist = playerViewModel::createPlaylist,
                                    onCreateUnifiedPlaylist = { name, songs ->
                                        playerViewModel.createPlaylist(name, songs, isUnified = true)
                                    },
                                    onDeletePlaylist = playerViewModel::deletePlaylist,
                                    onAddSongToPlaylist = playerViewModel::addSongToPlaylist,
                                    onAddSongsToPlaylist = playerViewModel::addSongsToPlaylist,
                                    onRemoveSongFromPlaylist = playerViewModel::removeSongFromPlaylist,
                                    onToggleFavorite = playerViewModel::toggleFavorite,
                                    hazeState = hazeState,
                                    enableBlur = effectiveEnableBlur,
                                    useLiquidGlass = effectiveUseLiquidGlass
                                )

                                LorTab.SETTINGS -> {
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
                                        hazeState = hazeState,
                                        useDynamicPlayerColors = playerViewModel.useDynamicPlayerColors.value,
                                        enableBetaUpdates = playerViewModel.enableBetaUpdates.value,
                                        useAlbumArtGradient = playerViewModel.useAlbumArtGradient.value,
                                        useMyWaveCoverGradient = playerViewModel.useMyWaveCoverGradient.value,
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
                                        onToggleDynamicPlayerColors = playerViewModel::setUseDynamicPlayerColors,
                                        onToggleBetaUpdates = playerViewModel::setEnableBetaUpdates,
                                        onToggleAlbumArtGradient = playerViewModel::setUseAlbumArtGradient,
                                        onToggleMyWaveCoverGradient = playerViewModel::setUseMyWaveCoverGradient,
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
                                        isHalloweenMode = playerViewModel.isHalloweenMode.value,
                                        onToggleHalloweenMode = playerViewModel::setHalloweenMode,
                                        onCheckForUpdates = { playerViewModel.checkForUpdates(silent = false) }
                                    )
                                }
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
                    .navigationBarsPadding()
                    .padding(bottom = if (currentSong != null) 140.dp else 75.dp)
            ) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(horizontal = 20.dp),
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
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clipToBounds()
            ) {
                val bottomScrimColor = MaterialTheme.colorScheme.background
                if (effectiveEnableBlur) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .hazeEffect(state = hazeState) {
                                blurEnabled = true
                                backgroundColor = bottomScrimColor
                                blurRadius = 24.dp
                                tints = listOf(HazeTint(bottomScrimColor.copy(alpha = 0.65f)))
                            }
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        bottomScrimColor.copy(alpha = 0.50f),
                                        bottomScrimColor.copy(alpha = 0.88f)
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
                                        Color.Transparent,
                                        bottomScrimColor.copy(alpha = 0.42f),
                                        bottomScrimColor.copy(alpha = 0.82f),
                                        bottomScrimColor.copy(alpha = 0.98f)
                                    )
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                    } else if (isSoundCloudMode) {
                        ScMusicBottomNavBar(
                            selected = activeScTab,
                            onSelect = { tab: com.lormusic.app.ui.components.ScTab ->
                                playerViewModel.selectScTab(tab)
                            },
                            hazeState = hazeState,
                            enableBlur = effectiveEnableBlur,
                            useLiquidGlass = effectiveUseLiquidGlass,
                            showBottomSearchButton = true,
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
                            onDownloadTrack = {
                                if (song.isRemoteSc || song.id.startsWith("sc_") || song.uri.startsWith("sc://")) {
                                    playerViewModel.downloadScSongWithTags(song)
                                } else {
                                    playerViewModel.downloadYtSongWithTags(song)
                                }
                            },
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
                            onTranslateLyrics = {
                                val target = when (appLanguage) {
                                    AppLanguage.UK -> "uk"
                                    AppLanguage.EN -> "en"
                                    else -> "ru"
                                }
                                playerViewModel.translateCurrentLyrics(target)
                            },
                            onOpenSongActions = { songForActionsSheet = song },
                            onOpenTagEditor = { songForTagEditor = song },
                            onOpenArtistProfile = { artist -> showNowPlaying = false; playerViewModel.openArtistProfile(artist) },
                            halloweenCandies = playerViewModel.halloweenCandies.value,
                            onEatCandies = {
                                playerViewModel.setHalloweenCandies(false)
                                playerViewModel.statusMessage.value = currentStrings.halloweenCandiesEaten
                            },
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
            isFavorite = playerViewModel.isFavorite(targetSong.id),
            isHidden = hiddenSongIds.contains(targetSong.id),
            playlists = playlists,
            onDismiss = { songForActionsSheet = null },
            onPlayNow = { playerViewModel.playSong(targetSong) },
            onPlayNext = { playerViewModel.playNextInQueue(targetSong) },
            onToggleFavorite = { playerViewModel.toggleFavorite(targetSong) },
            onOpenTagEditor = { songForTagEditor = targetSong },
            onOpenArtistProfile = { artist ->
                songForActionsSheet = null
                playerViewModel.openArtistProfile(artist)
            },
            onDownloadSong = {
                if (targetSong.isRemoteSc || targetSong.id.startsWith("sc_") || targetSong.uri.startsWith("sc://")) {
                    playerViewModel.downloadScSongWithTags(targetSong)
                } else {
                    playerViewModel.downloadYtSongWithTags(targetSong)
                }
            },
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
            onCreateUnifiedPlaylistWithSong = { name ->
                playerViewModel.createPlaylist(name, listOf(targetSong), isUnified = true)
            }
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
        val artistData = playerViewModel.currentArtistProfileData.value
        val artistSongs = playerViewModel.getArtistSongs(selectedArtist)
        val topTracks = playerViewModel.getArtistTopTracks(selectedArtist)
        val albums = playerViewModel.getArtistAlbums(selectedArtist)
        val singles = playerViewModel.getArtistSingles(selectedArtist)

        val effectiveDur = (if (durationMs > 0) durationMs else currentSong?.durationMs ?: 1L).coerceAtLeast(1L)
        val playerProgress = (positionMs.toFloat() / effectiveDur.toFloat()).coerceIn(0f, 1f)

        ArtistProfileDialog(
            artistName = selectedArtist,
            allArtistSongs = artistSongs,
            topTracks = topTracks,
            albums = albums,
            singlesAndEPs = singles,
            currentSong = currentSong,
            isPlaying = isPlaying,
            isFavorite = playerViewModel::isFavorite,
            progress = playerProgress,
            initialAvatarUrl = artistData?.avatarUrl,
            enableBlur = effectiveEnableBlur,
            useLiquidGlass = effectiveUseLiquidGlass,
            onDismiss = playerViewModel::closeArtistProfile,
            onPlaySongAt = { list, idx -> playerViewModel.setQueueAndPlay(list, idx, false) },
            onPlayAll = { list, shuffle -> playerViewModel.setQueueAndPlay(list, 0, shuffle) },
            onToggleFavorite = playerViewModel::toggleFavorite,
            onSongMoreClick = { songForActionsSheet = it },
            onTogglePlay = playerViewModel::togglePlayPause,
            onPrevious = playerViewModel::skipPrevious,
            onNext = playerViewModel::skipNext,
            onOpenPlayer = {
                showNowPlaying = true
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

    val showClassicRemovedNotice by playerViewModel.showClassicRemovedNotice
    if (showClassicRemovedNotice) {
        AlertDialog(
            onDismissRequest = { playerViewModel.dismissClassicRemovedNotice() },
            title = {
                Text(
                    text = currentStrings.classicRemovedTitle,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = currentStrings.classicRemovedMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { playerViewModel.dismissClassicRemovedNotice() },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(currentStrings.classicRemovedConfirm)
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
            appLanguage = appLanguage,
            hazeState = hazeState,
            enableBlur = enableBlur
        )
    }

    val showHalloweenGreeting by playerViewModel.showHalloweenGreeting
    if (showHalloweenGreeting) {
        AlertDialog(
            onDismissRequest = { playerViewModel.dismissHalloweenGreeting() },
            icon = {
                Text("🎃", fontSize = 42.sp)
            },
            title = {
                Text(
                    text = currentStrings.halloweenGreetingTitle,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = currentStrings.halloweenGreetingMsg,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { playerViewModel.selectHalloweenTreat() },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9E40),
                        contentColor = Color(0xFF2C1300)
                    )
                ) {
                    Text(currentStrings.halloweenTreatBtn, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { playerViewModel.selectHalloweenTrick() },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFFF7518)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF7518)
                    )
                ) {
                    Text(currentStrings.halloweenTrickBtn, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    val triggerTrick by playerViewModel.triggerHalloweenTrickJumpscare
    if (triggerTrick) {
        val view = LocalView.current
        var pumpkinScale by remember { mutableFloatStateOf(0.2f) }
        var shakeOffset by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(Unit) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            androidx.compose.animation.core.animate(
                initialValue = 0.2f,
                targetValue = 18f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 950,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            ) { value, _ ->
                pumpkinScale = value
                shakeOffset = (kotlin.math.sin(value * 9f) * 28f).toFloat()
            }
            delay(150)
            activity?.finishAffinity()
            kotlin.system.exitProcess(0)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {},
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎃",
                fontSize = 48.sp,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = pumpkinScale
                        scaleY = pumpkinScale
                        translationX = shakeOffset
                    }
            )
        }
    }
}
}
