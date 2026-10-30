package com.lormusic.app.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lormusic.app.R
import com.lormusic.app.data.SoundCloudRepository
import com.lormusic.app.model.LocalStrings
import com.lormusic.app.model.Playlist
import com.lormusic.app.model.Song
import com.lormusic.app.model.PlaybackContext
import com.lormusic.app.model.PlaybackContextType
import com.lormusic.app.model.SoundCloudUserProfile
import com.lormusic.app.ui.components.IosBackChevron
import com.lormusic.app.ui.components.ScTab
import com.lormusic.app.ui.components.SongActionsBottomSheet
import com.lormusic.app.ui.components.SongArtwork
import com.lormusic.app.ui.components.SongRow
import com.lormusic.app.ui.components.appBackdropSource
import com.lormusic.app.ui.components.liquidGlassBubble
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SoundCloudScreen(
    activeTab: ScTab,
    onSelectTab: (ScTab) -> Unit,
    charts: List<Song>,
    likes: List<Song> = emptyList(),
    searchResults: List<Song>,
    savedSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    isFavorite: (String) -> Boolean,
    onToggleFavorite: (Song) -> Unit,
    onPlayQueue: (List<Song>, Int, Boolean, PlaybackContext?) -> Unit = { _, _, _, _ -> },
    onSearch: (String) -> Unit,
    onRefreshCharts: () -> Unit,
    profile: SoundCloudUserProfile?,
    onSignInSuccess: (String) -> Unit,
    onSignOut: () -> Unit,
    playlists: List<Playlist>,
    onAddToPlaylist: (String, Song) -> Unit,
    onCreateNewPlaylistWithSong: (String, Song, Boolean) -> Unit,
    onSwitchToLocal: () -> Unit,
    isLoadingCharts: Boolean = false,
    isSearching: Boolean = false,
    hazeState: HazeState? = null,
    enableBlur: Boolean = true,
    useLiquidGlass: Boolean = false,
    betaSeparateHeaderBubbles: Boolean = false
) {
    val view = LocalView.current
    val strings = LocalStrings.current
    val coroutineScope = rememberCoroutineScope()

    var showWebLoginDialog by remember { mutableStateOf(false) }
    var showForceTokenDialog by remember { mutableStateOf(false) }
    var showAccountMenuDialog by remember { mutableStateOf(false) }
    var selectedActionSong by remember { mutableStateOf<Song?>(null) }
    var forceTokenInput by remember { mutableStateOf("") }
    var tokenError by remember { mutableStateOf<String?>(null) }
    var isValidatingToken by remember { mutableStateOf(false) }

    BackHandler {
        if (activeTab != ScTab.HOME) {
            onSelectTab(ScTab.HOME)
        } else {
            onSwitchToLocal()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (profile == null) {
            ScSignInRequiredGate(
                hazeState = null,
                enableBlur = enableBlur,
                useLiquidGlass = false,
                betaSeparateHeaderBubbles = betaSeparateHeaderBubbles,
                onWebLoginClick = { showWebLoginDialog = true },
                onTokenLoginClick = {
                    forceTokenInput = ""
                    tokenError = null
                    showForceTokenDialog = true
                },
                onBackToLocal = onSwitchToLocal
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                if (betaSeparateHeaderBubbles) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    if (activeTab != ScTab.HOME) {
                                        onSelectTab(ScTab.HOME)
                                    } else {
                                        onSwitchToLocal()
                                    }
                                }
                                .liquidGlassBubble(CircleShape, null, enableBlur, false),
                            contentAlignment = Alignment.Center
                        ) {
                            IosBackChevron(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .liquidGlassBubble(RoundedCornerShape(22.dp), null, enableBlur, false)
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
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_soundcloud),
                                            contentDescription = "SoundCloud",
                                            tint = Color(0xFF111111),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "SoundCloud",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Box(
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                showAccountMenuDialog = true
                            }
                            .liquidGlassBubble(CircleShape, null, enableBlur, false),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profile?.avatarUrl.isNullOrBlank()) {
                            SongArtwork(
                                seed = (profile?.username ?: "SoundCloud").hashCode(),
                                albumArtUri = profile?.avatarUrl,
                                cornerRadius = 22.dp,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = strings.signInSoundCloud,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                        }
                    }
                } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                if (activeTab != ScTab.HOME) {
                                    onSelectTab(ScTab.HOME)
                                } else {
                                    onSwitchToLocal()
                                }
                            }
                            .liquidGlassBubble(RoundedCornerShape(22.dp), null, enableBlur, false)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IosBackChevron(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_soundcloud),
                                        contentDescription = "SoundCloud",
                                        tint = Color(0xFF111111),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                showAccountMenuDialog = true
                            }
                            .liquidGlassBubble(CircleShape, null, enableBlur, false),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profile?.avatarUrl.isNullOrBlank()) {
                            SongArtwork(
                                seed = (profile?.username ?: "SoundCloud").hashCode(),
                                albumArtUri = profile?.avatarUrl,
                                cornerRadius = 22.dp,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = strings.signInSoundCloud,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
                }

                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        fadeIn(tween(220)).togetherWith(fadeOut(tween(180)))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = "scTabTransition"
                ) { tab ->
                    when (tab) {
                        ScTab.HOME -> ScHomeTab(
                            username = profile?.username ?: "",
                            likes = likes,
                            charts = charts,
                            isLoading = isLoadingCharts,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            isFavorite = isFavorite,
                            onPlayQueue = onPlayQueue,
                            onToggleFavorite = onToggleFavorite,
                            onSongMoreClick = { selectedActionSong = it },
                            onRefresh = onRefreshCharts,
                            onOpenSearch = { onSelectTab(ScTab.SEARCH) },
                            onSelectGenre = { genre ->
                                onSearch(genre)
                                onSelectTab(ScTab.SEARCH)
                            },
                            hazeState = hazeState
                        )

                        ScTab.CHARTS -> ScChartsTab(
                            charts = charts,
                            isLoading = isLoadingCharts,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            isFavorite = isFavorite,
                            onPlayQueue = onPlayQueue,
                            onToggleFavorite = onToggleFavorite,
                            onSongMoreClick = { selectedActionSong = it },
                            onRefresh = onRefreshCharts,
                            hazeState = hazeState
                        )

                        ScTab.LIBRARIES -> ScLibrariesTab(
                            savedSongs = savedSongs,
                            playlists = playlists,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            isFavorite = isFavorite,
                            onPlayQueue = onPlayQueue,
                            onToggleFavorite = onToggleFavorite,
                            onSongMoreClick = { selectedActionSong = it },
                            onCreatePlaylist = { name, isUnified ->
                                val songToUse = savedSongs.firstOrNull() ?: currentSong ?: charts.firstOrNull()
                                if (songToUse != null) {
                                    onCreateNewPlaylistWithSong(name, songToUse, isUnified)
                                }
                            },
                            hazeState = hazeState
                        )

                        ScTab.SEARCH -> ScSearchTab(
                            searchResults = searchResults,
                            isSearching = isSearching,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            isFavorite = isFavorite,
                            onPlayQueue = onPlayQueue,
                            onToggleFavorite = onToggleFavorite,
                            onSongMoreClick = { selectedActionSong = it },
                            onSearch = onSearch,
                            hazeState = hazeState
                        )
                    }
                }
            }
        }
    }

    if (showAccountMenuDialog) {
        if (profile != null) {
            ScUserProfileSheet(
                profile = profile,
                userLikes = likes,
                onDismiss = { showAccountMenuDialog = false },
                onSignOut = {
                    showAccountMenuDialog = false
                    onSignOut()
                },
                onSwitchAccount = {
                    showAccountMenuDialog = false
                    showWebLoginDialog = true
                },
                onForceTokenLogin = {
                    showAccountMenuDialog = false
                    forceTokenInput = ""
                    tokenError = null
                    showForceTokenDialog = true
                },
                onPlaySong = { s ->
                    val idx = likes.indexOf(s).coerceAtLeast(0)
                    onPlayQueue(likes, idx, false, PlaybackContext(PlaybackContextType.FAVORITES))
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showAccountMenuDialog = false },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_soundcloud),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "SoundCloud",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                showAccountMenuDialog = false
                                showWebLoginDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.scWebBrowserLogin)
                        }
                        OutlinedButton(
                            onClick = {
                                showAccountMenuDialog = false
                                forceTokenInput = ""
                                tokenError = null
                                showForceTokenDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.scForceTokenLogin)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAccountMenuDialog = false }) {
                        Text(strings.close)
                    }
                }
            )
        }
    }

    if (showForceTokenDialog) {
        AlertDialog(
            onDismissRequest = { if (!isValidatingToken) showForceTokenDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = strings.scForceTokenLogin,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.scSignInSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = forceTokenInput,
                        onValueChange = {
                            forceTokenInput = it
                            tokenError = null
                        },
                        label = { Text("OAuth Token") },
                        placeholder = { Text("2-293482-...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        isError = tokenError != null
                    )
                    if (tokenError != null) {
                        Text(
                            text = tokenError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val token = SoundCloudRepository.cleanToken(forceTokenInput)
                        if (token.isBlank()) {
                            tokenError = strings.scNoTracksAvailable
                            return@Button
                        }
                        isValidatingToken = true
                        tokenError = null
                        coroutineScope.launch {
                            val prof = withContext(Dispatchers.IO) {
                                SoundCloudRepository.fetchUserProfile(token)
                            }
                            isValidatingToken = false
                            if (prof != null) {
                                showForceTokenDialog = false
                                onSignInSuccess(token)
                            } else {
                                tokenError = "OAuth error / 401 Unauthorized"
                            }
                        }
                    },
                    enabled = !isValidatingToken && forceTokenInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isValidatingToken) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(strings.done)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForceTokenDialog = false },
                    enabled = !isValidatingToken
                ) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showWebLoginDialog) {
        ScRealWebLoginDialog(
            onDismiss = { showWebLoginDialog = false },
            onAuthSuccess = { token ->
                showWebLoginDialog = false
                onSignInSuccess(token)
            }
        )
    }

    if (selectedActionSong != null) {
        val s = selectedActionSong!!
        SongActionsBottomSheet(
            song = s,
            isFavorite = isFavorite(s.id),
            playlists = playlists,
            onDismiss = { selectedActionSong = null },
            onPlayNow = { onPlayQueue(listOf(s), 0, false, PlaybackContext(PlaybackContextType.SINGLE, s.title)) },
            onPlayNext = { onPlayQueue(listOf(s), 0, false, PlaybackContext(PlaybackContextType.SINGLE, s.title)) },
            onToggleFavorite = { onToggleFavorite(s) },
            onAddToPlaylist = { plId -> onAddToPlaylist(plId, s) },
            onCreateNewPlaylistWithSong = { name -> onCreateNewPlaylistWithSong(name, s, false) },
            onCreateUnifiedPlaylistWithSong = { name -> onCreateNewPlaylistWithSong(name, s, true) }
        )
    }
}

/**
 * Gate requiring sign-in to SoundCloud (Принудительный вход, как в Google)
 */
@Composable
private fun ScSignInRequiredGate(
    hazeState: HazeState?,
    enableBlur: Boolean,
    useLiquidGlass: Boolean,
    betaSeparateHeaderBubbles: Boolean = false,
    onWebLoginClick: () -> Unit,
    onTokenLoginClick: () -> Unit,
    onBackToLocal: () -> Unit
) {
    val view = LocalView.current
    val strings = LocalStrings.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onBackToLocal()
                    }
                    .liquidGlassBubble(RoundedCornerShape(22.dp), null, enableBlur, false)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IosBackChevron(color = MaterialTheme.colorScheme.onSurface)
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_soundcloud),
                                contentDescription = "SoundCloud",
                                tint = Color(0xFF111111),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_soundcloud),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = strings.signInSoundCloud,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = strings.scSignInSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = onWebLoginClick,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(strings.scWebBrowserLogin)
                }

                OutlinedButton(
                    onClick = onTokenLoginClick,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.scForceTokenLogin)
                }
            }
        }
    }
}

@Composable
private fun ScHomeTab(
    username: String,
    likes: List<Song>,
    charts: List<Song>,
    isLoading: Boolean,
    currentSong: Song?,
    isPlaying: Boolean,
    isFavorite: (String) -> Boolean,
    onPlayQueue: (List<Song>, Int, Boolean, PlaybackContext?) -> Unit = { _, _, _, _ -> },
    onToggleFavorite: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit,
    onRefresh: () -> Unit,
    onOpenSearch: () -> Unit,
    onSelectGenre: (String) -> Unit,
    hazeState: HazeState? = null
) {
    val strings = LocalStrings.current
    val genres = remember(strings) {
        listOf(
            strings.scGenreAll,
            strings.scGenrePhonk,
            strings.scGenreElectronic,
            strings.scGenreHipHop,
            strings.scGenreRemix,
            strings.scGenreRock,
            strings.scGenreTrance,
            strings.scGenreDancehall
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(if (hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier)
            .appBackdropSource(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        if (likes.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = strings.scYourLikes,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { onPlayQueue(likes, 0, true, PlaybackContext(PlaybackContextType.FAVORITES)) }) {
                        Icon(Icons.Filled.Shuffle, contentDescription = strings.shuffle)
                    }
                }
            }

            item {
                val gridSongs = likes.take(4)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gridSongs.chunked(2).forEach { rowSongs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowSongs.forEach { song ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onPlayQueue(likes, likes.indexOf(song), false, PlaybackContext(PlaybackContextType.FAVORITES)) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SongArtwork(
                                            seed = song.coverColorSeed,
                                            albumArtUri = song.albumArtUri,
                                            songUri = song.uri,
                                            cornerRadius = 16.dp,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 10.dp)
                                        ) {
                                            Text(
                                                text = song.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = song.artist,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            if (rowSongs.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            Text(
                text = strings.scTrendingByGenre,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(genres) { genre ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelectGenre(genre) }
                    ) {
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        if (isLoading && charts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                }
            }
        } else if (charts.isNotEmpty()) {
            itemsIndexed(charts.take(6)) { idx, song ->
                SongRow(
                    song = song,
                    isPlaying = isPlaying && currentSong?.id == song.id,
                    isCurrentSong = currentSong?.id == song.id,
                    isFavorite = isFavorite(song.id),
                    onClick = { onPlayQueue(charts, idx, false, PlaybackContext(PlaybackContextType.SINGLE, charts.getOrNull(idx)?.title)) },
                    onToggleFavorite = { onToggleFavorite(song) },
                    onMoreClick = { onSongMoreClick(song) }
                )
            }
        }

        item {
            Spacer(Modifier.height(20.dp))
            Text(
                text = strings.scMadeForYou,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    MadeForYouCard(
                        title = strings.scDailyDropsTitle,
                        subtitle = strings.scDailyDropsDesc,
                        badge = "DAILY",
                        onClick = { onPlayQueue(charts.shuffled(), 0, true, PlaybackContext(PlaybackContextType.SINGLE, charts.firstOrNull()?.title)) }
                    )
                }
                item {
                    MadeForYouCard(
                        title = strings.scWeeklyWaveTitle,
                        subtitle = strings.scWeeklyWaveDesc,
                        badge = "WEEKLY",
                        onClick = { onPlayQueue(charts.reversed(), 0, true, PlaybackContext(PlaybackContextType.SINGLE, charts.lastOrNull()?.title)) }
                    )
                }
            }
        }

        if (charts.size > 6) {
            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.scMoreOfWhatYouLike,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = strings.rescan)
                    }
                }
            }
            itemsIndexed(charts.drop(6)) { idx, song ->
                SongRow(
                    song = song,
                    isPlaying = isPlaying && currentSong?.id == song.id,
                    isCurrentSong = currentSong?.id == song.id,
                    isFavorite = isFavorite(song.id),
                    onClick = { onPlayQueue(charts, idx + 6, false, PlaybackContext(PlaybackContextType.SINGLE, charts.getOrNull(idx + 6)?.title)) },
                    onToggleFavorite = { onToggleFavorite(song) },
                    onMoreClick = { onSongMoreClick(song) }
                )
            }
        }
    }
}

@Composable
private fun MadeForYouCard(
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .width(220.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_soundcloud),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ScChartsTab(
    charts: List<Song>,
    isLoading: Boolean,
    currentSong: Song?,
    isPlaying: Boolean,
    isFavorite: (String) -> Boolean,
    onPlayQueue: (List<Song>, Int, Boolean, PlaybackContext?) -> Unit = { _, _, _, _ -> },
    onToggleFavorite: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit,
    onRefresh: () -> Unit,
    hazeState: HazeState? = null
) {
    val strings = LocalStrings.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(if (hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier)
            .appBackdropSource(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.scTopCharts,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = String.format(strings.tracksCountFormat, charts.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = strings.rescan)
                    }
                    if (charts.isNotEmpty()) {
                        Button(
                            onClick = { onPlayQueue(charts, 0, true, PlaybackContext(PlaybackContextType.SINGLE, charts.firstOrNull()?.title)) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.shuffle)
                        }
                    }
                }
            }
        }

        if (isLoading && charts.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                }
            }
        } else if (charts.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text(text = strings.scNoTracksAvailable, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            itemsIndexed(charts) { idx, song ->
                SongRow(
                    song = song,
                    isPlaying = isPlaying && currentSong?.id == song.id,
                    isCurrentSong = currentSong?.id == song.id,
                    isFavorite = isFavorite(song.id),
                    onClick = { onPlayQueue(charts, idx, false, PlaybackContext(PlaybackContextType.SINGLE, charts.getOrNull(idx)?.title)) },
                    onToggleFavorite = { onToggleFavorite(song) },
                    onMoreClick = { onSongMoreClick(song) }
                )
            }
        }
    }
}

@Composable
private fun ScLibrariesTab(
    savedSongs: List<Song>,
    playlists: List<Playlist>,
    currentSong: Song?,
    isPlaying: Boolean,
    isFavorite: (String) -> Boolean,
    onPlayQueue: (List<Song>, Int, Boolean, PlaybackContext?) -> Unit = { _, _, _, _ -> },
    onToggleFavorite: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit,
    onCreatePlaylist: (String, Boolean) -> Unit,
    hazeState: HazeState? = null
) {
    val strings = LocalStrings.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var isUnifiedToggle by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(if (hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier)
            .appBackdropSource(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.ytLibraries,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = {
                    newPlaylistName = ""
                    isUnifiedToggle = false
                    showCreateDialog = true
                }) {
                    Icon(Icons.Filled.Add, contentDescription = strings.newPlaylistDialogTitle)
                }
            }
        }

        val scPlaylists = playlists.filter { it.isUnified || it.songs.any { s -> s.isRemoteSc } }
        if (scPlaylists.isNotEmpty()) {
            item {
                Text(
                    text = strings.navPlaylists,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                )
            }
            items(scPlaylists) { pl ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPlayQueue(pl.songs, 0, false, PlaybackContext(PlaybackContextType.PLAYLIST, pl.name)) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (pl.isUnified) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (pl.isUnified) Icons.Filled.Layers else Icons.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pl.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${String.format(strings.tracksCountFormat, pl.songs.size)} • ${if (pl.isUnified) strings.unifiedPlaylist else "SoundCloud"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Text(
                text = strings.scSavedTracks,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
            )
        }

        if (savedSongs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = strings.scNoSavedTracks, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            itemsIndexed(savedSongs) { idx, song ->
                SongRow(
                    song = song,
                    isPlaying = isPlaying && currentSong?.id == song.id,
                    isCurrentSong = currentSong?.id == song.id,
                    isFavorite = isFavorite(song.id),
                    onClick = { onPlayQueue(savedSongs, idx, false, PlaybackContext(PlaybackContextType.SINGLE, savedSongs.getOrNull(idx)?.title)) },
                    onToggleFavorite = { onToggleFavorite(song) },
                    onMoreClick = { onSongMoreClick(song) }
                )
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(strings.newPlaylistDialogTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text(strings.playlistNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = isUnifiedToggle,
                            onCheckedChange = { isUnifiedToggle = it }
                        )
                        Text("Создать как общий плейлист")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName.trim(), isUnifiedToggle)
                            showCreateDialog = false
                        }
                    },
                    shape = RoundedCornerShape(14.dp)
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
private fun ScSearchTab(
    searchResults: List<Song>,
    isSearching: Boolean,
    currentSong: Song?,
    isPlaying: Boolean,
    isFavorite: (String) -> Boolean,
    onPlayQueue: (List<Song>, Int, Boolean, PlaybackContext?) -> Unit = { _, _, _, _ -> },
    onToggleFavorite: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit,
    onSearch: (String) -> Unit,
    hazeState: HazeState? = null
) {
    val strings = LocalStrings.current
    var query by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onSearch(it)
                    },
                    placeholder = { Text(strings.soundCloudPlaceholder) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        onSearch("")
                    }) {
                        Icon(Icons.Filled.Clear, contentDescription = strings.clear)
                    }
                }
            }
        }

        if (isSearching) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        } else if (searchResults.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier)
                    .appBackdropSource(),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                itemsIndexed(searchResults) { idx, song ->
                    SongRow(
                        song = song,
                        isPlaying = isPlaying && currentSong?.id == song.id,
                        isCurrentSong = currentSong?.id == song.id,
                        isFavorite = isFavorite(song.id),
                        onClick = { onPlayQueue(searchResults, idx, false, PlaybackContext(PlaybackContextType.SINGLE, searchResults.getOrNull(idx)?.title)) },
                        onToggleFavorite = { onToggleFavorite(song) },
                        onMoreClick = { onSongMoreClick(song) }
                    )
                }
            }
        } else if (query.isNotBlank()) {
            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                Text(text = String.format(strings.nothingFoundQuery, query), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ScRealWebLoginDialog(
    onDismiss: () -> Unit,
    onAuthSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    var isPageLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf("https://soundcloud.com/signin") }
    var hasCompletedAuth by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun extractAndSubmit(webView: WebView?, urlStr: String?, isManual: Boolean = false) {
        if (hasCompletedAuth) return
        val cookieManager = CookieManager.getInstance()
        val cookie = cookieManager.getCookie("https://soundcloud.com")
            ?: cookieManager.getCookie(urlStr ?: currentUrl)
            ?: ""
        val match = Regex("""oauth_token=([a-zA-Z0-9_\-]+)""").find(cookie)
        if (match != null) {
            val token = SoundCloudRepository.cleanToken(match.groupValues[1])
            if (token.length > 8 && token != "guest") {
                hasCompletedAuth = true
                cookieManager.flush()
                onAuthSuccess(token)
                return
            }
        }
        webView?.evaluateJavascript(
            """(function(){
                try {
                    if (window._lastScAuth && window._lastScAuth.length > 8) return window._lastScAuth;
                    for (var i = 0; i < localStorage.length; i++) {
                        var k = (localStorage.key(i) || '').toLowerCase();
                        if (k.indexOf('oauth') !== -1 || k.indexOf('token') !== -1 || k.indexOf('auth') !== -1 || k.indexOf('session') !== -1) {
                            var v = localStorage.getItem(localStorage.key(i)) || '';
                            var m = v.match(/2-[0-9]{5,}-[0-9]{5,}-[a-zA-Z0-9_-]+/);
                            if (m && m[0]) return m[0];
                            if (v.indexOf('2-') !== -1) {
                                var sub = v.substring(v.indexOf('2-'));
                                var clean = sub.split(/["';,\s]/)[0];
                                if (clean.length > 15) return clean;
                            }
                            if (v.length > 20 && !v.startsWith('{') && !v.startsWith('[')) return v;
                        }
                    }
                    for (var j = 0; j < sessionStorage.length; j++) {
                        var sk = (sessionStorage.key(j) || '').toLowerCase();
                        if (sk.indexOf('oauth') !== -1 || sk.indexOf('token') !== -1) {
                            var sv = sessionStorage.getItem(sessionStorage.key(j)) || '';
                            var sm = sv.match(/2-[0-9]{5,}-[0-9]{5,}-[a-zA-Z0-9_-]+/);
                            if (sm && sm[0]) return sm[0];
                            if (sv.indexOf('2-') !== -1) {
                                var sub2 = sv.substring(sv.indexOf('2-'));
                                var clean2 = sub2.split(/["';,\s]/)[0];
                                if (clean2.length > 15) return clean2;
                            }
                        }
                    }
                    var cm = document.cookie.match(/oauth_token=([a-zA-Z0-9_\-]+)/);
                    if (cm && cm[1] && cm[1].length > 8) return cm[1];
                } catch(e) {}
                return '';
            })()""".trimIndent()
        ) { res ->
            val cleaned = SoundCloudRepository.cleanToken(res)
            if (cleaned.isNotBlank() && cleaned != "guest" && cleaned.length > 8) {
                hasCompletedAuth = true
                cookieManager.flush()
                onAuthSuccess(cleaned)
            } else if (isManual) {
                android.widget.Toast.makeText(
                    context,
                    strings.scSignInSubtitle,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = strings.close)
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_soundcloud),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = strings.signInSoundCloud,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Войдите через Google или аккаунт",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (isPageLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    FilledTonalButton(
                        onClick = {
                            extractAndSubmit(webViewRef, currentUrl, isManual = true)
                        },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(strings.done)
                    }
                }

                HorizontalDivider()

                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewRef = this
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    setSupportZoom(true)
                                    setSupportMultipleWindows(false)
                                    javaScriptCanOpenWindowsAutomatically = true
                                    userAgentString =
                                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                                        runCatching {
                                            val headers = request.requestHeaders
                                            val authHeader = headers?.get("Authorization") ?: headers?.get("authorization")
                                            if (!authHeader.isNullOrBlank()) {
                                                val token = SoundCloudRepository.cleanToken(authHeader)
                                                if (token.length > 8 && token != "guest" && !hasCompletedAuth) {
                                                    hasCompletedAuth = true
                                                    view.post {
                                                        runCatching { onAuthSuccess(token) }
                                                    }
                                                }
                                            }
                                        }
                                        return super.shouldInterceptRequest(view, request)
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isPageLoading = true
                                        if (url != null) currentUrl = url
                                        view?.evaluateJavascript(
                                            """(function() {
                                                if (window._scAuthHooked) return;
                                                window._scAuthHooked = true;
                                                var orig = XMLHttpRequest.prototype.setRequestHeader;
                                                XMLHttpRequest.prototype.setRequestHeader = function(h, v) {
                                                    if (h && h.toLowerCase() === 'authorization' && v) {
                                                        window._lastScAuth = v;
                                                    }
                                                    return orig.apply(this, arguments);
                                                };
                                                var origFetch = window.fetch;
                                                if (origFetch) {
                                                    window.fetch = function(input, init) {
                                                        try {
                                                            if (init && init.headers) {
                                                                var hdrs = init.headers;
                                                                var a = (hdrs instanceof Headers) ? hdrs.get('authorization') : (hdrs['Authorization'] || hdrs['authorization']);
                                                                if (a) window._lastScAuth = a;
                                                            }
                                                        } catch(e) {}
                                                        return origFetch.apply(this, arguments);
                                                    };
                                                }
                                            })();""".trimIndent(), null
                                        )
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isPageLoading = false
                                        if (url != null) {
                                            currentUrl = url
                                            view?.evaluateJavascript(
                                                """(function() {
                                                    if (window._scAuthHooked) return;
                                                    window._scAuthHooked = true;
                                                    var orig = XMLHttpRequest.prototype.setRequestHeader;
                                                    XMLHttpRequest.prototype.setRequestHeader = function(h, v) {
                                                        if (h && h.toLowerCase() === 'authorization' && v) {
                                                            window._lastScAuth = v;
                                                        }
                                                        return orig.apply(this, arguments);
                                                    };
                                                    var origFetch = window.fetch;
                                                    if (origFetch) {
                                                        window.fetch = function(input, init) {
                                                            try {
                                                                if (init && init.headers) {
                                                                    var hdrs = init.headers;
                                                                    var a = (hdrs instanceof Headers) ? hdrs.get('authorization') : (hdrs['Authorization'] || hdrs['authorization']);
                                                                    if (a) window._lastScAuth = a;
                                                                }
                                                            } catch(e) {}
                                                            return origFetch.apply(this, arguments);
                                                        };
                                                    }
                                                })();""".trimIndent(), null
                                            )
                                            val isAuthed = url.contains("/discover") ||
                                                    url.contains("/you") ||
                                                    url.contains("/feed") ||
                                                    url.contains("/stream") ||
                                                    url.contains("/settings")
                                            if (!hasCompletedAuth && isAuthed && !url.contains("/signin") && !url.contains("/login")) {
                                                extractAndSubmit(view, url, isManual = false)
                                            }
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val reqUrl = request?.url?.toString() ?: return false
                                        val uri = request.url
                                        val scheme = uri?.scheme?.lowercase() ?: ""
                                        if (scheme != "http" && scheme != "https") {
                                            try {
                                                if (scheme == "intent") {
                                                    val intent = Intent.parseUri(reqUrl, Intent.URI_INTENT_SCHEME)
                                                    val fallbackUrl = intent?.getStringExtra("browser_fallback_url")
                                                    if (!fallbackUrl.isNullOrBlank()) {
                                                        view?.loadUrl(fallbackUrl)
                                                    }
                                                }
                                            } catch (_: Exception) {}
                                            return true
                                        }
                                        return false
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onCreateWindow(
                                        view: WebView?,
                                        isDialog: Boolean,
                                        isUserGesture: Boolean,
                                        resultMsg: android.os.Message?
                                    ): Boolean {
                                        return try {
                                            val href = view?.handler?.obtainMessage()
                                            view?.requestFocusNodeHref(href)
                                            val popupUrl = href?.data?.getString("url")
                                            if (!popupUrl.isNullOrBlank()) {
                                                view.loadUrl(popupUrl)
                                            }
                                            false
                                        } catch (_: Exception) {
                                            false
                                        }
                                    }
                                }
                                loadUrl(currentUrl)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Modern Material 3 SoundCloud Profile Dialog / Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScUserProfileSheet(
    profile: SoundCloudUserProfile,
    userLikes: List<Song>,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit,
    onSwitchAccount: () -> Unit,
    onForceTokenLogin: () -> Unit,
    onPlaySong: (Song) -> Unit
) {
    val strings = LocalStrings.current
    var showLogoutConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.scProfileTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = strings.close)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (!profile.avatarUrl.isNullOrBlank()) {
                            SongArtwork(
                                seed = profile.username.hashCode(),
                                albumArtUri = profile.avatarUrl,
                                cornerRadius = 36.dp,
                                modifier = Modifier.size(72.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.username,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!profile.permalinkUrl.isNullOrBlank()) {
                        Text(
                            text = "@${profile.permalinkUrl.trimEnd('/').substringAfterLast('/')}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_soundcloud),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "SoundCloud",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            val displayLikes = if (profile.likesCount > 0) profile.likesCount else userLikes.size
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = displayLikes.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.scLikedTracksCount,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (profile.trackCount > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = profile.trackCount.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.scUploadedTracksCount,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (profile.followersCount > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = profile.followersCount.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.scFollowersCount,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (userLikes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = strings.scYourLikes,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                LazyColumn(modifier = Modifier.heightIn(max = 190.dp)) {
                    items(userLikes.take(4)) { song ->
                        ListItem(
                            headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingContent = {
                                SongArtwork(
                                    seed = song.coverColorSeed,
                                    albumArtUri = song.albumArtUri,
                                    songUri = song.uri,
                                    cornerRadius = 10.dp,
                                    modifier = Modifier.size(40.dp)
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable {
                                onPlaySong(song)
                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onSwitchAccount()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.switchAccount)
                }

                Button(
                    onClick = { showLogoutConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.signOut)
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(strings.scSignOutConfirmTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.scSignOutConfirmDesc) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onDismiss()
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(strings.signOut)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
