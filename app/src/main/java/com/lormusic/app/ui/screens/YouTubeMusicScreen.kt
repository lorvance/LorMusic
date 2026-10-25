package com.lormusic.app.ui.screens

import com.lormusic.app.model.LocalStrings

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.accounts.AccountManager
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lormusic.app.data.YouTubeMusicRepository
import com.lormusic.app.model.GoogleAccountProfile
import com.lormusic.app.model.Playlist
import com.lormusic.app.model.Song
import com.lormusic.app.ui.components.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@Composable
fun YouTubeMusicScreen(
    activeTab: YtMusicTab,
    recentlyPlayed: List<Song>,
    recommendations: List<Song>,
    radioMixes: List<Pair<String, List<Song>>>,
    myWaveSongs: List<Song>,
    searchResults: List<Song>,
    isLoadingFeed: Boolean,
    isSearching: Boolean,
    downloadingSongIds: Set<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    playlists: List<Playlist>,
    savedYtSongs: List<Song>,
    googleAccount: GoogleAccountProfile?,
    hazeState: HazeState,
    enableBlur: Boolean = true,
    useLiquidGlass: Boolean = false,
    showTopSearchBar: Boolean = false,
    recentsViewMode: String = "grid",
    onToggleRecentsViewMode: (String) -> Unit = {},
    onSelectTab: (YtMusicTab) -> Unit = {},
    onPlayQueue: (List<Song>, Int, Boolean) -> Unit,
    onTogglePlay: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onDownloadSongWithTags: (Song) -> Unit,
    onSearchQuery: (String) -> Unit,
    onRefreshFeed: () -> Unit,
    onRefreshMyWave: (String?) -> Unit,
    onSignInNativeAccount: (String, String) -> Unit = { _, _ -> },
    onSignInWithCookie: (String) -> Unit,
    onSignOutGoogle: () -> Unit,
    onSyncGoogleLibrary: () -> Unit,
    onCreatePlaylist: (String, List<Song>) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onSongMoreClick: (Song) -> Unit,
    onSwitchToLocal: () -> Unit
) {
    val strings = LocalStrings.current
    var showWebLoginDialog by remember { mutableStateOf(false) }
    var showLoginChoiceDialog by remember { mutableStateOf(false) }
    var showAccountMenuDialog by remember { mutableStateOf(false) }

    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                val displayName = accountName.substringBefore("@")
                onSignInNativeAccount(displayName, accountName)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (googleAccount == null) {

            YtSignInRequiredGate(
                isSyncing = isLoadingFeed,
                hazeState = hazeState,
                enableBlur = enableBlur,
                useLiquidGlass = useLiquidGlass,
                onSignInClick = { showLoginChoiceDialog = true },
                onBackToLocal = onSwitchToLocal
            )
        } else {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(tween(220)).togetherWith(fadeOut(tween(180)))
                },
                label = "ytMusicTabContent"
            ) { tab ->
                when (tab) {
                    YtMusicTab.HOME -> YtHomeTab(
                        recentlyPlayed = recentlyPlayed,
                        recommendations = recommendations,
                        radioMixes = radioMixes,
                        isLoading = isLoadingFeed,
                        downloadingSongIds = downloadingSongIds,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        favoriteIds = favoriteIds,
                        googleAccount = googleAccount,
                        hazeState = hazeState,
                        enableBlur = enableBlur,
                        useLiquidGlass = useLiquidGlass,
                        showTopSearchBar = showTopSearchBar,
                        recentsViewMode = recentsViewMode,
                        onToggleRecentsViewMode = onToggleRecentsViewMode,
                        onOpenTopSearch = { onSelectTab(YtMusicTab.SEARCH) },
                        onPlayQueue = onPlayQueue,
                        onToggleFavorite = onToggleFavorite,
                        onDownloadSong = onDownloadSongWithTags,
                        onSongMoreClick = onSongMoreClick,
                        onRefresh = onRefreshFeed,
                        onOpenGoogleAuth = { showAccountMenuDialog = true },
                        onSyncGoogle = onSyncGoogleLibrary,
                        onBackToLocal = onSwitchToLocal
                    )

                    YtMusicTab.LIBRARIES -> YtLibrariesTab(
                        playlists = playlists,
                        favoriteSongs = (recentlyPlayed + recommendations + savedYtSongs)
                            .distinctBy { it.id }
                            .filter { favoriteIds.contains(it.id) },
                        savedYtSongs = savedYtSongs,
                        downloadingSongIds = downloadingSongIds,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        favoriteIds = favoriteIds,
                        hazeState = hazeState,
                        onPlayQueue = onPlayQueue,
                        onToggleFavorite = onToggleFavorite,
                        onDownloadSong = onDownloadSongWithTags,
                        onCreatePlaylist = onCreatePlaylist,
                        onDeletePlaylist = onDeletePlaylist,
                        onSongMoreClick = onSongMoreClick
                    )

                    YtMusicTab.MY_WAVE -> YtMyWaveTab(
                        waveSongs = myWaveSongs,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        favoriteIds = favoriteIds,
                        downloadingSongIds = downloadingSongIds,
                        hazeState = hazeState,
                        onPlayQueue = onPlayQueue,
                        onTogglePlay = onTogglePlay,
                        onToggleFavorite = onToggleFavorite,
                        onDownloadSong = onDownloadSongWithTags,
                        onRefreshWave = onRefreshMyWave,
                        onSongMoreClick = onSongMoreClick
                    )

                    YtMusicTab.SEARCH -> YtSearchTab(
                        searchResults = searchResults,
                        recommendations = recommendations,
                        isSearching = isSearching,
                        downloadingSongIds = downloadingSongIds,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        favoriteIds = favoriteIds,
                        hazeState = hazeState,
                        onSearch = onSearchQuery,
                        onPlayQueue = onPlayQueue,
                        onToggleFavorite = onToggleFavorite,
                        onDownloadSong = onDownloadSongWithTags,
                        onSongMoreClick = onSongMoreClick
                    )
                }
            }
        }
    }



    if (showLoginChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showLoginChoiceDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = strings.googleSignInTitle,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.googleSignInSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            showLoginChoiceDialog = false
                            try {
                                val intent = AccountManager.newChooseAccountIntent(
                                    null,
                                    null,
                                    arrayOf("com.google"),
                                    null,
                                    null,
                                    null,
                                    null
                                )
                                accountPickerLauncher.launch(intent)
                            } catch (_: Exception) {
                                showWebLoginDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.nativeGoogleSignIn, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            showLoginChoiceDialog = false
                            showWebLoginDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Language, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.webGoogleSignIn)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLoginChoiceDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showWebLoginDialog) {
        YtMusicRealWebLoginDialog(
            onDismiss = { showWebLoginDialog = false },
            onAuthSuccess = { cookie ->
                showWebLoginDialog = false
                onSignInWithCookie(cookie)
            }
        )
    }

    if (showAccountMenuDialog && googleAccount != null) {
        AlertDialog(
            onDismissRequest = { showAccountMenuDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CloudDone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = googleAccount.displayName,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = googleAccount.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.googleConnectedDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    FilledTonalButton(
                        onClick = {
                            showAccountMenuDialog = false
                            onSyncGoogleLibrary()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.syncWithGoogle)
                    }
                    OutlinedButton(
                        onClick = {
                            showAccountMenuDialog = false
                            showWebLoginDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.SwitchAccount, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.switchAccount)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountMenuDialog = false }) {
                    Text(strings.close)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        runCatching {
                            CookieManager.getInstance().removeAllCookies(null)
                            CookieManager.getInstance().flush()
                        }
                        showAccountMenuDialog = false
                        onSignOutGoogle()
                    }
                ) {
                    Text(strings.signOut, color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YtMusicRealWebLoginDialog(
    onDismiss: () -> Unit,
    onAuthSuccess: (String) -> Unit
) {
    var isPageLoading by remember { mutableStateOf(true) }
    var currentUrl by remember {
        mutableStateOf(
            "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&uilel=3&passive=true&continue=" +
                "https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Dru%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F"
        )
    }
    var hasCompletedAuth by remember { mutableStateOf(false) }
    val strings = LocalStrings.current

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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = strings.close)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.googleSignInTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = strings.googleSignInSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isPageLoading) {
                        ExpressiveLoadingIndicator(size = 28.dp)
                    }
                    Spacer(Modifier.width(6.dp))
                    FilledTonalButton(
                        onClick = {
                            val cookie = CookieManager.getInstance().getCookie("https://music.youtube.com")
                                ?: CookieManager.getInstance().getCookie("https://www.youtube.com")
                            if (!cookie.isNullOrBlank() && YouTubeMusicRepository.hasValidAuthCookie(cookie)) {
                                hasCompletedAuth = true
                                onAuthSuccess(cookie)
                            }
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

                                    userAgentString =
                                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isPageLoading = true
                                        if (url != null) currentUrl = url
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isPageLoading = false
                                        if (url != null) {
                                            currentUrl = url
                                            val ytmCookie = cookieManager.getCookie("https://music.youtube.com")
                                                ?: cookieManager.getCookie(url)
                                            if (!hasCompletedAuth &&
                                                url.contains("music.youtube.com") &&
                                                YouTubeMusicRepository.hasValidAuthCookie(ytmCookie)
                                            ) {
                                                hasCompletedAuth = true
                                                cookieManager.flush()
                                                onAuthSuccess(ytmCookie!!)
                                            }
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        return false
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

@Composable
private fun YtSignInRequiredGate(
    isSyncing: Boolean,
    hazeState: HazeState,
    enableBlur: Boolean = true,
    useLiquidGlass: Boolean = false,
    onSignInClick: () -> Unit,
    onBackToLocal: () -> Unit
) {
    val strings = LocalStrings.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val bubbleModifier: @Composable (CornerBasedShape) -> Modifier = { shape ->
        Modifier.liquidGlassBubble(
            shape = shape,
            hazeState = hazeState,
            enableBlur = enableBlur,
            isLiquidGlass = useLiquidGlass
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .hazeSource(state = hazeState),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isSyncing) {
                        ExpressiveLoadingIndicator(size = 68.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = strings.syncingWithGoogle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.LockPerson,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = strings.googleRequiredTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = strings.googleRequiredDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = onSignInClick,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Login, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.signInGoogle, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = onBackToLocal,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.backToLocal)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .clickable { onBackToLocal() }
                    .then(bubbleModifier(RoundedCornerShape(23.dp)))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = strings.backToLocal,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "YouTube Music",
                                tint = Color(0xFF111111),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YtHomeTab(
    recentlyPlayed: List<Song>,
    recommendations: List<Song>,
    radioMixes: List<Pair<String, List<Song>>>,
    isLoading: Boolean,
    downloadingSongIds: Set<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    googleAccount: GoogleAccountProfile?,
    hazeState: HazeState,
    enableBlur: Boolean = true,
    useLiquidGlass: Boolean = false,
    showTopSearchBar: Boolean = false,
    recentsViewMode: String = "grid",
    onToggleRecentsViewMode: (String) -> Unit = {},
    onOpenTopSearch: () -> Unit = {},
    onPlayQueue: (List<Song>, Int, Boolean) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onDownloadSong: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit,
    onRefresh: () -> Unit,
    onOpenGoogleAuth: () -> Unit,
    onSyncGoogle: () -> Unit,
    onBackToLocal: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    var avatarBitmap by remember(googleAccount?.avatarUrl) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(googleAccount?.avatarUrl) {
        val url = googleAccount?.avatarUrl
        if (!url.isNullOrBlank()) {
            avatarBitmap = loadSongBitmap(context, "yt_avatar_${url.hashCode()}", url, null)
        }
    }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val accentColor = MaterialTheme.colorScheme.primary
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

    Box(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .hazeSource(state = hazeState)
        ) {
            LazyColumn(
                flingBehavior = smoothFling,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = statusBarTop + 72.dp,
                    bottom = 225.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item(key = "recents_section") {
                    val isGrid = recentsViewMode.equals("grid", ignoreCase = true)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.recentsTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = strings.recentsSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (recentlyPlayed.isNotEmpty()) {
                                    TextButton(onClick = { onPlayQueue(recentlyPlayed.take(22), 0, false) }) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(strings.playAll, maxLines = 1)
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val nextMode = if (isGrid) "carousel" else "grid"
                                        onToggleRecentsViewMode(nextMode)
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isGrid) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
                                        contentDescription = strings.switchLayout,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))

                        val recents22 = recentlyPlayed.take(22).ifEmpty { recommendations.take(12) }

                        if (isGrid) {
                            val chunkedColumns = recents22.chunked(4)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(chunkedColumns, key = { colIdx, _ -> "rec_col_$colIdx" }) { _, columnSongs ->
                                    Column(
                                        modifier = Modifier.width(300.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        columnSongs.forEach { song ->
                                            YtRecentGridItem(
                                                song = song,
                                                isCurrent = currentSong?.id == song.id,
                                                isPlaying = isPlaying,
                                                onClick = {
                                                    val flatIndex = recents22.indexOfFirst { it.id == song.id }
                                                    onPlayQueue(recents22, flatIndex.coerceAtLeast(0), false)
                                                },
                                                onMoreClick = { onSongMoreClick(song) }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(recents22, key = { idx, s -> "rec_${s.id}_$idx" }) { index, song ->
                                    YtRecentSongCard(
                                        song = song,
                                        isCurrent = currentSong?.id == song.id,
                                        isPlaying = isPlaying,
                                        onClick = { onPlayQueue(recents22, index, false) },
                                        onDownload = { onDownloadSong(song) },
                                        isDownloading = downloadingSongIds.contains(song.id)
                                    )
                                }
                            }
                        }
                    }
                }

                if (radioMixes.isNotEmpty()) {
                    item(key = "radio_mixes_section") {
                        Column {
                            Text(
                                text = strings.radioMixesTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = strings.radioMixesSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(10.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                itemsIndexed(radioMixes, key = { idx, pair -> "radio_${idx}_${pair.first}" }) { _, (radioTitle, tracks) ->
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .width(210.dp)
                                            .clickable {
                                                if (tracks.isNotEmpty()) {
                                                    onPlayQueue(tracks, 0, false)
                                                }
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val previewSong = tracks.firstOrNull()
                                                if (previewSong != null) {
                                                    SongArtwork(
                                                        seed = previewSong.coverColorSeed,
                                                        albumArtUri = previewSong.albumArtUri,
                                                        songUri = previewSong.uri,
                                                        cornerRadius = 16.dp,
                                                        modifier = Modifier.size(58.dp)
                                                    )
                                                }
                                                Spacer(Modifier.weight(1f))
                                                FilledIconButton(
                                                    onClick = {
                                                        if (tracks.isNotEmpty()) onPlayQueue(tracks, 0, true)
                                                    },
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(Icons.Filled.Radio, contentDescription = null, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                            Spacer(Modifier.height(10.dp))
                                            Text(
                                                text = radioTitle,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = tracks.joinToString(", ") { it.artist }.ifBlank { strings.mixMode },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "recommendations_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.recommendationsTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = strings.recommendationsSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = onRefresh) {
                            if (isLoading) {
                                ExpressiveLoadingIndicator(size = 28.dp)
                            } else {
                                Icon(Icons.Filled.Refresh, contentDescription = strings.refreshStream)
                            }
                        }
                    }
                }

                if (isLoading && recommendations.isEmpty()) {
                    item(key = "loading_feed_box") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ExpressiveLoadingIndicator(size = 64.dp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = strings.loadingYtFeed,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                itemsIndexed(
                    items = recommendations,
                    key = { idx, s -> "yt_rec_${s.id}_$idx" }
                ) { index, song ->
                    YtSongRowWithDownload(
                        song = song,
                        isCurrentSong = currentSong?.id == song.id,
                        isPlaying = isPlaying,
                        isFavorite = favoriteIds.contains(song.id),
                        isDownloading = downloadingSongIds.contains(song.id),
                        onClick = { onPlayQueue(recommendations, index, false) },
                        onToggleFavorite = { onToggleFavorite(song) },
                        onDownload = { onDownloadSong(song) },
                        onMoreClick = { onSongMoreClick(song) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .clickable { onBackToLocal() }
                    .then(bubbleModifier(RoundedCornerShape(23.dp)))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = strings.backToLocal,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "YouTube Music",
                                tint = Color(0xFF111111),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showTopSearchBar) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable { onOpenTopSearch() }
                            .then(bubbleModifier(CircleShape)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = strings.searchTitle,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .clickable { onOpenGoogleAuth() }
                        .then(bubbleModifier(CircleShape)),
                    contentAlignment = Alignment.Center
                ) {
                    if (googleAccount != null) {
                        val avatar = avatarBitmap
                        if (avatar != null) {
                            Image(
                                bitmap = avatar,
                                contentDescription = googleAccount.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            val initial = (googleAccount.displayName.ifBlank { googleAccount.email })
                                .firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = initial,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = strings.signInGoogle,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YtRecentGridItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val strings = LocalStrings.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp)) {
                SongArtwork(
                    seed = song.coverColorSeed,
                    albumArtUri = song.albumArtUri,
                    songUri = song.uri,
                    cornerRadius = 10.dp,
                    modifier = Modifier.fillMaxSize()
                )
                if (isCurrent && isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedEqualizerBars(
                            modifier = Modifier.size(16.dp),
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = strings.songActions,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun YtRecentSongCard(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isDownloading: Boolean,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    val strings = LocalStrings.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isCurrent) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        modifier = Modifier
            .width(148.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                SongArtwork(
                    seed = song.coverColorSeed,
                    albumArtUri = song.albumArtUri,
                    songUri = song.uri,
                    cornerRadius = 16.dp,
                    modifier = Modifier.fillMaxSize()
                )
                if (!song.isLocal) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(30.dp)
                            .clickable { onDownload() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isDownloading) {
                                ExpressiveLoadingIndicator(
                                    size = 18.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = strings.downloadTrack,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
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
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun YtLibrariesTab(
    playlists: List<Playlist>,
    favoriteSongs: List<Song>,
    savedYtSongs: List<Song>,
    downloadingSongIds: Set<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    hazeState: HazeState,
    onPlayQueue: (List<Song>, Int, Boolean) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onDownloadSong: (Song) -> Unit,
    onCreatePlaylist: (String, List<Song>) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onSongMoreClick: (Song) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    val strings = LocalStrings.current
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .hazeSource(state = hazeState)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = strings.ytLibrariesTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = strings.ytLibrariesSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            FilledTonalButton(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.playlistNameLabel)
            }
        }

        Spacer(Modifier.height(14.dp))

        val activePl = selectedPlaylist
        if (activePl != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { selectedPlaylist = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activePl.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.tracksCountFormat.format(activePl.songs.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
                if (activePl.songs.isNotEmpty()) {
                    Button(
                        onClick = { onPlayQueue(activePl.songs, 0, false) },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(strings.playAction)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 220.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(activePl.songs) { idx, s ->
                    YtSongRowWithDownload(
                        song = s,
                        isCurrentSong = currentSong?.id == s.id,
                        isPlaying = isPlaying,
                        isFavorite = favoriteIds.contains(s.id),
                        isDownloading = downloadingSongIds.contains(s.id),
                        onClick = { onPlayQueue(activePl.songs, idx, false) },
                        onToggleFavorite = { onToggleFavorite(s) },
                        onDownload = { onDownloadSong(s) },
                        onMoreClick = { onSongMoreClick(s) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 220.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "playlists_grid_header") {
                    Text(
                        text = strings.myPlaylistsCount.format(playlists.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(playlists, key = { "yt_pl_${it.id}" }) { pl ->
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPlaylist = pl }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (pl.isSystem) Icons.Filled.Favorite else Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pl.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strings.tracksCountFormat.format(pl.songs.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                            if (pl.songs.isNotEmpty()) {
                                FilledTonalIconButton(
                                    onClick = { onPlayQueue(pl.songs, 0, false) }
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = strings.playAction)
                                }
                            }
                            if (!pl.isSystem) {
                                IconButton(onClick = { onDeletePlaylist(pl.id) }) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = strings.deletePlaylist
                                    )
                                }
                            }
                        }
                    }
                }

                if (favoriteSongs.isNotEmpty() || savedYtSongs.isNotEmpty()) {
                    item(key = "fav_tracks_header") {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = strings.favoritesAndSynced,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    val combinedFavs = (favoriteSongs + savedYtSongs).distinctBy { it.id }
                    itemsIndexed(combinedFavs, key = { idx, s -> "yt_lib_song_${s.id}_$idx" }) { idx, song ->
                        YtSongRowWithDownload(
                            song = song,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = isPlaying,
                            isFavorite = favoriteIds.contains(song.id),
                            isDownloading = downloadingSongIds.contains(song.id),
                            onClick = { onPlayQueue(combinedFavs, idx, false) },
                            onToggleFavorite = { onToggleFavorite(song) },
                            onDownload = { onDownloadSong(song) },
                            onMoreClick = { onSongMoreClick(song) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(strings.newLibraryPlaylist) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text(strings.playlistNameLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName.trim(), emptyList())
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
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
private fun YtMyWaveTab(
    waveSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    downloadingSongIds: Set<String>,
    hazeState: HazeState,
    onPlayQueue: (List<Song>, Int, Boolean) -> Unit,
    onTogglePlay: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onDownloadSong: (Song) -> Unit,
    onRefreshWave: (String?) -> Unit,
    onSongMoreClick: (Song) -> Unit
) {
    val strings = LocalStrings.current
    val moods: List<Pair<String, String?>> = listOf(
        strings.moodForYourTaste to null,
        strings.moodEnergetic to "electronic dance",
        strings.moodCalm to "chill ambient",
        strings.moodNightVibe to "synthwave retrowave",
        strings.moodInstrumental to "instrumental piano"
    )
    var selectedMood by remember(strings) { mutableStateOf(strings.moodForYourTaste) }

    val infiniteTransition = rememberInfiniteTransition(label = "myWaveGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wavePulse"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .hazeSource(state = hazeState)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 225.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "wave_hero") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(210.dp)
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(185.dp)
                            .scale(if (isPlaying) pulseScale else 1f)
                            .blur(40.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    WavyPlayPauseButton(
                        isPlaying = isPlaying,
                        onClick = {
                            if (isPlaying) {
                                onTogglePlay()
                            } else if (waveSongs.isNotEmpty()) {
                                onPlayQueue(waveSongs, 0, true)
                            }
                        },
                        size = 112.dp
                    )
                }

                Text(
                    text = strings.myWaveTitle,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when (strings) {
                    com.lormusic.app.model.StringsEn -> "Personal stream based on your favorites and listening history"
                    com.lormusic.app.model.StringsUk -> "Персональний потік на основі твоїх улюблених та нещодавно прослуханих треків"
                    else -> "Персональный поток на основе твоих любимых и недавно прослушанных треков"
                },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    moods.forEach { (label, query) ->
                        FilterChip(
                            selected = selectedMood == label,
                            onClick = {
                                selectedMood = label
                                onRefreshWave(query)
                            },
                            label = { Text(label) },
                            leadingIcon = if (selectedMood == label) {
                                { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                }
            }
        }

        item(key = "wave_list_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.nowInMyWaveFormat.format(waveSongs.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onRefreshWave(moods.find { it.first == selectedMood }?.second) }) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.refreshStream)
                }
            }
        }

        itemsIndexed(waveSongs, key = { idx, s -> "wave_${s.id}_$idx" }) { index, song ->
            YtSongRowWithDownload(
                song = song,
                isCurrentSong = currentSong?.id == song.id,
                isPlaying = isPlaying,
                isFavorite = favoriteIds.contains(song.id),
                isDownloading = downloadingSongIds.contains(song.id),
                onClick = { onPlayQueue(waveSongs, index, false) },
                onToggleFavorite = { onToggleFavorite(song) },
                onDownload = { onDownloadSong(song) },
                onMoreClick = { onSongMoreClick(song) }
            )
        }
    }
}

@Composable
private fun YtSearchTab(
    searchResults: List<Song>,
    recommendations: List<Song>,
    isSearching: Boolean,
    downloadingSongIds: Set<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    hazeState: HazeState,
    onSearch: (String) -> Unit,
    onPlayQueue: (List<Song>, Int, Boolean) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onDownloadSong: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val strings = LocalStrings.current
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .hazeSource(state = hazeState)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = strings.ytSearchHeader,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            placeholder = { Text(strings.ytSearchPlaceholder) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (isSearching) {
                    ExpressiveLoadingIndicator(size = 24.dp)
                } else if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        onSearch("")
                    }) {
                        Icon(Icons.Filled.Clear, contentDescription = strings.clear)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearch(query)
                    focusManager.clearFocus()
                }
            ),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        val displayed = if (query.isBlank() && searchResults.isEmpty()) recommendations else searchResults

        LazyColumn(
            flingBehavior = rememberSmoothFlingBehavior(),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 225.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(displayed, key = { idx, s -> "yt_s_${s.id}_$idx" }) { index, song ->
                YtSongRowWithDownload(
                    song = song,
                    isCurrentSong = currentSong?.id == song.id,
                    isPlaying = isPlaying,
                    isFavorite = favoriteIds.contains(song.id),
                    isDownloading = downloadingSongIds.contains(song.id),
                    onClick = { onPlayQueue(displayed, index, false) },
                    onToggleFavorite = { onToggleFavorite(song) },
                    onDownload = { onDownloadSong(song) },
                    onMoreClick = { onSongMoreClick(song) }
                )
            }
        }
    }
}

@Composable
private fun YtSongRowWithDownload(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    isDownloading: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onMoreClick: () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val strings = LocalStrings.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isCurrentSong) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongArtwork(
                seed = song.coverColorSeed,
                albumArtUri = song.albumArtUri,
                songUri = song.uri,
                cornerRadius = 14.dp,
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${song.artist} • ${formatDurationShort(song.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = strings.favorites,
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    YouTubeMusicRepository.shareSongLink(context, song)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = strings.shareSongLink,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onDownload,
                enabled = !isDownloading,
                modifier = Modifier.size(36.dp)
            ) {
                if (isDownloading) {
                    ExpressiveLoadingIndicator(size = 22.dp)
                } else {
                    Icon(
                        imageVector = if (song.isLocal) Icons.Filled.DownloadDone else Icons.Filled.Download,
                        contentDescription = strings.downloadTrack,
                        tint = if (song.isLocal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = strings.more,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
