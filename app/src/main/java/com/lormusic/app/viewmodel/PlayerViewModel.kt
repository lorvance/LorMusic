package com.lormusic.app.viewmodel

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.ComponentName
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.mutableStateOf
import java.io.File
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.lormusic.app.data.*
import com.lormusic.app.model.*
import com.lormusic.app.service.PlaybackService
import com.lormusic.app.service.SharedPlayerHolder
import com.lormusic.app.ui.components.YtMusicTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository(application)
    private val exoPlayer: ExoPlayer = SharedPlayerHolder.getOrCreate(application)

    private var rawAllSongs: List<Song> = SampleData.songs
    val tagOverrides = mutableStateOf(prefs.getTagOverrides())
    val allSongs = mutableStateOf<List<Song>>(SampleData.songs)
    val localSongsCount = mutableStateOf(0)
    val isScanningLocal = mutableStateOf(false)
    val favoriteIds = mutableStateOf(prefs.getFavoriteIds())
    val hiddenSongIds = mutableStateOf(prefs.getHiddenSongIds())
    val customPlaylists = mutableStateOf<List<Playlist>>(emptyList())
    val recentlyPlayedIds = mutableStateOf(prefs.getRecentlyPlayedIds())
    private var pendingDeleteSong: Song? = null

    val currentSong = mutableStateOf<Song?>(null)
    val isPlaying = mutableStateOf(false)
    val positionMs = mutableStateOf(0L)
    val durationMs = mutableStateOf(0L)
    val queue = mutableStateOf<List<Song>>(emptyList())
    val shuffleOn = mutableStateOf(false)
    val repeatMode = mutableStateOf(RepeatMode.OFF)
    val playbackSpeed = mutableStateOf(prefs.getPlaybackSpeed())
    val statusMessage = mutableStateOf<String?>(null)

    val currentLyrics = mutableStateOf<List<LyricLine>>(emptyList())
    val isLoadingLyrics = mutableStateOf(false)
    private var lyricsJob: Job? = null

    val sleepTimerRemainingSec = mutableStateOf<Int?>(null)
    private var sleepTimerJob: Job? = null

    val themeMode = mutableStateOf(prefs.getThemeMode())
    val useDynamicColor = mutableStateOf(prefs.getUseDynamicColor())
    val useSquigglySlider = mutableStateOf(prefs.getUseSquigglySlider())
    val enableBlur = mutableStateOf(prefs.getEnableBlur())
    val useLiquidGlass = mutableStateOf(prefs.getUseLiquidGlass())
    val enableBetaUpdates = mutableStateOf(prefs.getEnableBetaUpdates())
    val useAlbumArtGradient = mutableStateOf(prefs.getUseAlbumArtGradient())
    val autoTagFixEnabled = mutableStateOf<Boolean>(prefs.getAutoTagFixEnabled())
    val appLanguage = mutableStateOf(prefs.getAppLanguage())
    val recentsViewMode = mutableStateOf(prefs.getRecentsViewMode())
    val autoTagFixDetectedSongs = mutableStateOf<List<Song>>(emptyList())
    val showAutoTagFixPrompt = mutableStateOf(false)
    val searchBarPlacement = mutableStateOf(prefs.getSearchBarPlacement())
    val enablePlayerSwipe = mutableStateOf(prefs.getEnablePlayerSwipe())
    val hideDemoTracks = mutableStateOf(prefs.getHideDemoTracks())
    val selectedSort = mutableStateOf(prefs.getSortOption())
    val selectedFilter = mutableStateOf(prefs.getLibraryFilter())
    val selectedAppIcon = mutableStateOf(prefs.getAppIconOption())

    val isYtMusicMode = mutableStateOf(false)
    val activeYtTab = mutableStateOf(YtMusicTab.HOME)
    val ytRecommendations = mutableStateOf<List<Song>>(emptyList())
    val ytRadioMixes = mutableStateOf<List<Pair<String, List<Song>>>>(emptyList())
    val ytMyWaveSongs = mutableStateOf<List<Song>>(emptyList())
    val ytSearchResults = mutableStateOf<List<Song>>(emptyList())
    val isLoadingYtFeed = mutableStateOf(false)
    val isSearchingYt = mutableStateOf(false)
    val downloadingSongIds = mutableStateOf<Set<String>>(emptySet())
    val savedYtSongs = mutableStateOf(loadCleanSavedYtSongs())
    val ytMusicCookie = mutableStateOf(prefs.getYtMusicCookie())
    val googleAccount = mutableStateOf<GoogleAccountProfile?>(loadInitialGoogleProfile())
    private var ytSearchJob: Job? = null

    val updateManager = UpdateManager(application)
    val updateState = mutableStateOf<UpdateState>(UpdateState.Idle)
    val showUpdateDialog = mutableStateOf(false)
    val availableUpdateInfo = mutableStateOf<AppUpdateInfo?>(null)

    private fun isLegacyPreviewSong(song: Song): Boolean {
        return song.id.startsWith("yt_dz_") ||
            song.id.startsWith("yt_it_") ||
            song.uri.contains("dzcdn.net", ignoreCase = true) ||
            song.uri.contains("mzstatic.com", ignoreCase = true) ||
            song.uri.contains("itunes.apple.com", ignoreCase = true)
    }

    private fun loadCleanSavedYtSongs(): List<Song> {
        val raw = prefs.getSavedYtSongs()
        val cleaned = raw
            .filterNot { isLegacyPreviewSong(it) }
            .map { song ->
                if (song.id.startsWith("yt_v_") && !song.isLocal) {
                    song.copy(uri = "yt://${song.id.removePrefix("yt_v_")}")
                } else {
                    song
                }
            }
        if (cleaned != raw) {
            prefs.saveYtSongs(cleaned)
        }
        return cleaned
    }

    val classicLibraryDesign = mutableStateOf(prefs.getClassicLibraryDesign())
    val selectedArtistProfile = mutableStateOf<String?>(null)

    private fun loadInitialGoogleProfile(): GoogleAccountProfile? {
        val cookie = prefs.getYtMusicCookie()
        if (!YouTubeMusicRepository.hasValidAuthCookie(cookie)) {
            prefs.saveGoogleAccount(null, null, null)
            return null
        }
        val name = prefs.getGoogleAccountName()
        val email = prefs.getGoogleAccountEmail()
        val avatar = prefs.getGoogleAccountAvatar()
        return if (!name.isNullOrBlank() && !email.isNullOrBlank()) {
            GoogleAccountProfile(displayName = name, email = email, avatarUrl = avatar)
        } else null
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying.value = playing
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val mediaId = mediaItem?.mediaId ?: return
            val found = queue.value.find { it.id == mediaId }
                ?: allSongs.value.find { it.id == mediaId }
                ?: ytRecommendations.value.find { it.id == mediaId }
                ?: ytMyWaveSongs.value.find { it.id == mediaId }
                ?: ytSearchResults.value.find { it.id == mediaId }
            if (found != null) {
                val withTags = found.withOverride(tagOverrides.value[found.id])
                currentSong.value = withTags
                durationMs.value = exoPlayer.duration.takeIf { it > 0 } ?: withTags.durationMs
                positionMs.value = exoPlayer.currentPosition.coerceAtLeast(0L)
                prefs.saveLastPlayedSongId(withTags.id)
                recordRecentlyPlayed(withTags)
                loadLyricsFor(withTags)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                val dur = exoPlayer.duration
                if (dur > 0) {
                    durationMs.value = dur
                }
            } else if (playbackState == Player.STATE_ENDED) {
                if (repeatMode.value == RepeatMode.OFF && exoPlayer.currentMediaItemIndex >= queue.value.lastIndex) {
                    isPlaying.value = false
                    positionMs.value = 0L
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            statusMessage.value = "Не удалось воспроизвести трек: ${error.localizedMessage ?: "ошибка источника"}"
            isPlaying.value = false
        }
    }

    init {
        exoPlayer.addListener(playerListener)
        exoPlayer.setPlaybackSpeed(playbackSpeed.value)

        viewModelScope.launch {
            val context = getApplication<Application>()
            val readyDemos = withContext(Dispatchers.IO) {
                DemoSynthGenerator.ensureDemoTracksReady(context, SampleData.songs)
            }
            val scannedLocal = withContext(Dispatchers.IO) {
                LocalMusicScanner.scanDeviceMusic(context)
            }
            localSongsCount.value = scannedLocal.size
            val comparator = AlphaNumericComparator()
            val combinedRaw = (scannedLocal + readyDemos).sortedWith { a, b ->
                comparator.compare(a.title, b.title)
            }
            rawAllSongs = combinedRaw
            val overrides = tagOverrides.value
            val combined = combinedRaw.map { it.withOverride(overrides[it.id]) }
            allSongs.value = combined
            rebuildPlaylists(combined)

            val lastId = prefs.getLastPlayedSongId()
            val initialSong = combined.find { it.id == lastId } ?: combined.firstOrNull()
            if (currentSong.value == null && initialSong != null) {
                currentSong.value = initialSong
                durationMs.value = initialSong.durationMs
                queue.value = combined
                loadLyricsFor(initialSong)
            }

            val existingCookie = ytMusicCookie.value
            if (!existingCookie.isNullOrBlank() && YouTubeMusicRepository.hasValidAuthCookie(existingCookie)) {
                syncGoogleLibrary()
            }

            if (autoTagFixEnabled.value) {
                checkBrokenTracksForAutoTagFix()
            }

            delay(2000)
            checkForUpdates(silent = true)
        }

        viewModelScope.launch {
            while (true) {
                delay(100)
                if (exoPlayer.isPlaying) {
                    positionMs.value = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val realDur = exoPlayer.duration
                    if (realDur > 0 && realDur != durationMs.value) {
                        durationMs.value = realDur
                    }
                }
            }
        }
    }

    fun getRecentlyPlayedSongs(): List<Song> {
        if (googleAccount.value == null || !YouTubeMusicRepository.hasValidAuthCookie(ytMusicCookie.value)) {
            return emptyList()
        }
        val pool = (savedYtSongs.value + ytRecommendations.value + ytMyWaveSongs.value)
            .filterNot { isLegacyPreviewSong(it) || it.isLocal }
            .distinctBy { it.id }
            .associateBy { it.id }
        return recentlyPlayedIds.value
            .mapNotNull { pool[it] }
            .filterNot { it.isLocal }
            .take(22)
    }

    private fun recordRecentlyPlayed(song: Song) {
        if (!song.isLocal && (song.isRemoteYt || song.id.startsWith("yt_"))) {
            prefs.addRecentlyPlayedId(song.id)
            recentlyPlayedIds.value = prefs.getRecentlyPlayedIds()
            val updatedSaved = (listOf(song) + savedYtSongs.value)
                .distinctBy { it.id }
                .take(60)
            savedYtSongs.value = updatedSaved
            prefs.saveYtSongs(updatedSaved)
        }
    }

    fun saveSongTagOverride(override: SongTagOverride) {
        val updatedMap = tagOverrides.value.toMutableMap()
        updatedMap[override.songId] = override
        prefs.saveTagOverrides(updatedMap)
        tagOverrides.value = updatedMap
        applyTagOverridesToState(updatedMap)
        statusMessage.value = "Теги трека «${override.title}» сохранены"
    }

    fun applyTagFixProposals(proposals: List<TagFixProposal>) {
        if (proposals.isEmpty()) return
        val updatedMap = tagOverrides.value.toMutableMap()
        proposals.forEach { p ->
            val existing = updatedMap[p.originalSong.id]
            val merged = SongTagOverride(
                songId = p.originalSong.id,
                title = p.proposedTitle,
                artist = p.proposedArtist,
                album = p.proposedAlbum,
                genre = p.proposedGenre.ifBlank { existing?.genre ?: p.originalSong.genre },
                year = p.proposedYear.ifBlank { existing?.year ?: p.originalSong.year },
                trackNumber = existing?.trackNumber ?: p.originalSong.trackNumber,
                albumArtUri = p.proposedCoverUri ?: existing?.albumArtUri ?: p.originalSong.albumArtUri,
                customLyrics = existing?.customLyrics ?: p.originalSong.customLyrics
            )
            updatedMap[p.originalSong.id] = merged
        }
        prefs.saveTagOverrides(updatedMap)
        tagOverrides.value = updatedMap
        applyTagOverridesToState(updatedMap)
        statusMessage.value = "TagFix успешно обновил теги и обложки (${proposals.size} треков)"
    }

    private fun applyTagOverridesToState(overrides: Map<String, SongTagOverride>) {
        val comparator = AlphaNumericComparator()
        val updatedAll = rawAllSongs
            .map { it.withOverride(overrides[it.id]) }
            .sortedWith { a, b -> comparator.compare(a.title, b.title) }
        allSongs.value = updatedAll
        queue.value = queue.value.map { it.withOverride(overrides[it.id]) }
        currentSong.value = currentSong.value?.let { cur ->
            val updatedCur = cur.withOverride(overrides[cur.id])
            loadLyricsFor(updatedCur)
            updatedCur
        }
        rebuildPlaylists(updatedAll)
    }

    fun setAppIconOption(option: AppIconOption) {
        selectedAppIcon.value = option
        prefs.saveAppIconOption(option)

        try {
            val app = getApplication<Application>()
            val pm = app.packageManager
            val pkg = app.packageName

            val targetComponent = ComponentName(pkg, option.aliasClassName)
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            AppIconOption.entries.filter { it != option }.forEach { other ->
                val comp = ComponentName(pkg, other.aliasClassName)
                pm.setComponentEnabledSetting(
                    comp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
            statusMessage.value = "Иконка изменена на «${option.label}»"
        } catch (_: Exception) {
            statusMessage.value = "Выбрана иконка «${option.label}»"
        }
    }

    fun enterYtMusicMode() {
        isYtMusicMode.value = true
        activeYtTab.value = YtMusicTab.HOME
        if (googleAccount.value != null && YouTubeMusicRepository.hasValidAuthCookie(ytMusicCookie.value)) {
            if (ytRecommendations.value.isEmpty()) {
                refreshYtHomeFeed()
            }
            if (ytMyWaveSongs.value.isEmpty()) {
                refreshYtMyWave(null)
            }
        } else {
            ytRecommendations.value = emptyList()
            ytRadioMixes.value = emptyList()
            ytMyWaveSongs.value = emptyList()
            ytSearchResults.value = emptyList()
        }
    }

    fun exitYtMusicMode() {
        isYtMusicMode.value = false
    }

    fun selectYtTab(tab: YtMusicTab) {
        activeYtTab.value = tab
        if (tab == YtMusicTab.MY_WAVE &&
            ytMyWaveSongs.value.isEmpty() &&
            googleAccount.value != null &&
            YouTubeMusicRepository.hasValidAuthCookie(ytMusicCookie.value)
        ) {
            refreshYtMyWave(null)
        }
    }

    fun refreshYtHomeFeed() {
        val cookie = ytMusicCookie.value
        if (googleAccount.value == null || !YouTubeMusicRepository.hasValidAuthCookie(cookie)) {
            ytRecommendations.value = emptyList()
            ytRadioMixes.value = emptyList()
            return
        }
        if (isLoadingYtFeed.value) return
        viewModelScope.launch {
            isLoadingYtFeed.value = true
            val seedSongs = getRecentlyPlayedSongs() + allSongs.value.filter { favoriteIds.value.contains(it.id) }
            val (recs, radioTracks) = YouTubeMusicRepository.loadPersonalizedHomeFeed(seedSongs, cookie)
            ytRecommendations.value = recs
            val mixes = buildList {
                if (recs.isNotEmpty()) {
                    add("Микс для тебя" to recs.take(10))
                }
                if (radioTracks.isNotEmpty()) {
                    add("Радио YouTube Music" to radioTracks.take(10))
                    add("Ночной вайб" to radioTracks.shuffled().take(8))
                }
            }
            ytRadioMixes.value = mixes
            if (ytMyWaveSongs.value.isEmpty() && recs.isNotEmpty()) {
                ytMyWaveSongs.value = (recs + radioTracks).distinctBy { it.id }.shuffled()
            }
            isLoadingYtFeed.value = false
        }
    }

    fun refreshYtMyWave(moodQuery: String?) {
        val cookie = ytMusicCookie.value
        if (googleAccount.value == null || !YouTubeMusicRepository.hasValidAuthCookie(cookie)) {
            ytMyWaveSongs.value = emptyList()
            return
        }
        viewModelScope.launch {
            val seedSongs = getRecentlyPlayedSongs() + allSongs.value.filter { favoriteIds.value.contains(it.id) }
            val wave = if (!moodQuery.isNullOrBlank()) {
                YouTubeMusicRepository.searchYtMusic(moodQuery, limit = 24, cookie = cookie)
            } else {
                YouTubeMusicRepository.loadMyWaveTracks(seedSongs, YouTubeMusicRepository.WaveMood.FOR_YOU, cookie = cookie)
            }
            ytMyWaveSongs.value = wave
        }
    }

    fun searchYtMusic(query: String) {
        ytSearchJob?.cancel()
        val cookie = ytMusicCookie.value
        if (query.isBlank() || googleAccount.value == null || !YouTubeMusicRepository.hasValidAuthCookie(cookie)) {
            ytSearchResults.value = emptyList()
            isSearchingYt.value = false
            return
        }
        ytSearchJob = viewModelScope.launch {
            delay(280)
            isSearchingYt.value = true
            val results = YouTubeMusicRepository.searchYtMusic(query, cookie = cookie)
            ytSearchResults.value = results
            isSearchingYt.value = false
        }
    }

    fun downloadYtSongWithTags(song: Song) {
        if (downloadingSongIds.value.contains(song.id)) return
        viewModelScope.launch {
            downloadingSongIds.value = downloadingSongIds.value + song.id
            statusMessage.value = "Скачивание «${song.title}» со всеми тегами ID3v2.3 и обложкой…"
            val result = YouTubeMusicRepository.downloadTrackToPhoneWithTags(getApplication(), song)
            downloadingSongIds.value = downloadingSongIds.value - song.id

            if (result != null) {
                val (downloaded, override) = result
                val updatedOverrides = tagOverrides.value.toMutableMap()
                updatedOverrides[override.songId] = override
                prefs.saveTagOverrides(updatedOverrides)
                tagOverrides.value = updatedOverrides

                rawAllSongs = listOf(downloaded) + rawAllSongs.filterNot { it.id == downloaded.id }
                applyTagOverridesToState(updatedOverrides)
                localSongsCount.value = allSongs.value.count { it.isLocal }

                val updatedSaved = listOf(downloaded) + savedYtSongs.value.filterNot { it.id == song.id || it.id == downloaded.id }
                savedYtSongs.value = updatedSaved
                prefs.saveYtSongs(updatedSaved)
                if (autoTagFixEnabled.value) {
                    checkBrokenTracksForAutoTagFix()
                }
                statusMessage.value = "Трек «${downloaded.title}» скачан локально со всеми тегами и обложкой!"
            } else {
                statusMessage.value = "Не удалось скачать трек «${song.title}»"
            }
        }
    }

    fun signInWithCookie(cookie: String) {
        ytMusicCookie.value = cookie
        prefs.saveYtMusicCookie(cookie)
        viewModelScope.launch {
            isLoadingYtFeed.value = true
            val syncData = YouTubeMusicRepository.syncAuthenticatedAccount(cookie)
            val profile = syncData.profile ?: GoogleAccountProfile(
                displayName = "Аккаунт YouTube Music",
                email = "YouTube Music Sync"
            )
            googleAccount.value = profile
            prefs.saveGoogleAccount(profile.displayName, profile.email, profile.avatarUrl)

            val newSaved = (syncData.historySongs + syncData.likedSongs + savedYtSongs.value)
                .distinctBy { it.id }
                .take(120)
            savedYtSongs.value = newSaved
            prefs.saveYtSongs(newSaved)

            if (syncData.historySongs.isNotEmpty()) {
                syncData.historySongs.take(20).reversed().forEach { s ->
                    prefs.addRecentlyPlayedId(s.id)
                }
                recentlyPlayedIds.value = prefs.getRecentlyPlayedIds()
            }

            if (syncData.likedSongs.isNotEmpty()) {
                val updatedFavs = favoriteIds.value.toMutableSet()
                syncData.likedSongs.forEach { updatedFavs.add(it.id) }
                favoriteIds.value = updatedFavs
                prefs.saveFavoriteIds(updatedFavs)
            }

            if (syncData.homeRecommendations.isNotEmpty() || syncData.likedSongs.isNotEmpty()) {
                val recs = syncData.homeRecommendations.ifEmpty { syncData.likedSongs }
                val radioPool = (syncData.likedSongs + syncData.homeRecommendations).distinctBy { it.id }
                ytRecommendations.value = recs
                ytRadioMixes.value = buildList {
                    if (recs.isNotEmpty()) add("Микс для тебя" to recs.take(10))
                    if (radioPool.isNotEmpty()) {
                        add("Радио YouTube Music" to radioPool.take(10))
                        add("Ночной вайб" to radioPool.shuffled().take(8))
                    }
                }
                ytMyWaveSongs.value = radioPool.shuffled()
            } else {
                isLoadingYtFeed.value = false
                refreshYtHomeFeed()
            }
            isLoadingYtFeed.value = false
            rebuildPlaylists(allSongs.value)
        }
    }

    fun signOutGoogle() {
        googleAccount.value = null
        ytMusicCookie.value = null
        ytRecommendations.value = emptyList()
        ytRadioMixes.value = emptyList()
        ytMyWaveSongs.value = emptyList()
        ytSearchResults.value = emptyList()
        savedYtSongs.value = emptyList()
        prefs.saveGoogleAccount(null, null, null)
        prefs.saveYtMusicCookie(null)
        prefs.saveYtSongs(emptyList())
        try {
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
        } catch (_: Exception) {
        }
        statusMessage.value = "Вы вышли из аккаунта Google / YouTube Music"
    }

    fun syncGoogleLibrary() {
        val cookie = ytMusicCookie.value
        if (!cookie.isNullOrBlank() && YouTubeMusicRepository.hasValidAuthCookie(cookie)) {
            signInWithCookie(cookie)
        } else {
            refreshYtHomeFeed()
            refreshYtMyWave(null)
        }
    }

    fun loadLyricsFor(song: Song) {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            isLoadingLyrics.value = true
            currentLyrics.value = emptyList()
            val lines = LyricsRepository.getLyricsForSong(getApplication(), song)
            currentLyrics.value = lines
            isLoadingLyrics.value = false
        }
    }

    fun rescanLocalMusic() {
        if (isScanningLocal.value) return
        viewModelScope.launch {
            isScanningLocal.value = true
            val context = getApplication<Application>()
            val readyDemos = rawAllSongs.filter { !it.isLocal }.ifEmpty {
                withContext(Dispatchers.IO) {
                    DemoSynthGenerator.ensureDemoTracksReady(context, SampleData.songs)
                }
            }
            val scannedLocal = withContext(Dispatchers.IO) {
                LocalMusicScanner.scanDeviceMusic(context)
            }
            localSongsCount.value = scannedLocal.size
            val comparator = AlphaNumericComparator()
            val combinedRaw = (scannedLocal + readyDemos).sortedWith { a, b ->
                comparator.compare(a.title, b.title)
            }
            rawAllSongs = combinedRaw
            applyTagOverridesToState(tagOverrides.value)
            isScanningLocal.value = false
            if (autoTagFixEnabled.value) {
                checkBrokenTracksForAutoTagFix()
            }
            statusMessage.value = if (scannedLocal.isEmpty()) {
                "Локальные аудиофайлы не найдены на устройстве"
            } else {
                "Найдено локальных треков: ${scannedLocal.size}"
            }
        }
    }

    private fun rebuildPlaylists(songsPool: List<Song>) {
        val songMap = (songsPool + savedYtSongs.value).associateBy { it.id }
        val saved = prefs.getCustomPlaylists()
        if (saved == null) {
            val initial = SampleData.playlists.map { pl ->
                pl.copy(songs = pl.songs.map { s -> songMap[s.id] ?: s })
            }
            customPlaylists.value = initial
            persistPlaylists(initial)
        } else {
            customPlaylists.value = saved.map { sp ->
                Playlist(
                    id = sp.id,
                    name = sp.name,
                    songs = sp.songIds.mapNotNull { songMap[it] },
                    coverColorSeed = sp.coverColorSeed
                )
            }
        }
    }

    private fun persistPlaylists(playlists: List<Playlist>) {
        prefs.saveCustomPlaylists(
            playlists.map { pl ->
                StoredPlaylistData(
                    id = pl.id,
                    name = pl.name,
                    songIds = pl.songs.map { it.id },
                    coverColorSeed = pl.coverColorSeed
                )
            }
        )
    }

    fun getAllPlaylistsWithFavorites(): List<Playlist> {
        val pool = (allSongs.value + savedYtSongs.value).distinctBy { it.id }
        val favSongs = pool.filter { favoriteIds.value.contains(it.id) }
        val favPlaylist = Playlist(
            id = "sys_favorites",
            name = "Мне нравится",
            songs = favSongs,
            coverColorSeed = 777,
            isSystem = true
        )
        return listOf(favPlaylist) + customPlaylists.value
    }

    fun isFavorite(songId: String): Boolean = favoriteIds.value.contains(songId)

    fun isSongHidden(songId: String): Boolean = hiddenSongIds.value.contains(songId)

    fun toggleHiddenSong(song: Song) {
        val updated = hiddenSongIds.value.toMutableSet()
        val nowHidden = if (updated.contains(song.id)) {
            updated.remove(song.id)
            false
        } else {
            updated.add(song.id)
            true
        }
        hiddenSongIds.value = updated
        prefs.saveHiddenSongIds(updated)

        if (nowHidden && selectedFilter.value != LibraryFilter.HIDDEN) {
            val idxInQueue = queue.value.indexOfFirst { it.id == song.id }
            if (idxInQueue >= 0 && currentSong.value?.id != song.id) {
                removeQueueItemAt(idxInQueue)
            }
        }

        statusMessage.value = if (nowHidden) {
            "Трек «${song.title}» скрыт из потока (перемещён в «Скрытые»)"
        } else {
            "Трек «${song.title}» возвращён в основной поток"
        }
    }

    fun deleteSongFromDevice(song: Song, onNeedIntentSender: (IntentSender) -> Unit) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                var deleted = false
                if (!song.filePath.isNullOrBlank()) {
                    val file = File(song.filePath)
                    if (file.exists() && file.delete()) {
                        deleted = true
                    }
                }
                if (song.uri.startsWith("content://")) {
                    val uri = Uri.parse(song.uri)
                    val rows = withContext(Dispatchers.IO) {
                        context.contentResolver.delete(uri, null, null)
                    }
                    if (rows > 0) {
                        deleted = true
                    }
                } else if (song.uri.startsWith("file://")) {
                    val path = Uri.parse(song.uri).path
                    if (!path.isNullOrBlank() && File(path).delete()) {
                        deleted = true
                    }
                }

                if (deleted) {
                    finalizeSongDeletionFromState(song)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && song.uri.startsWith("content://")) {
                    pendingDeleteSong = song
                    val pi = MediaStore.createDeleteRequest(
                        context.contentResolver,
                        listOf(Uri.parse(song.uri))
                    )
                    onNeedIntentSender(pi.intentSender)
                } else {
                    finalizeSongDeletionFromState(song)
                }
            } catch (secEx: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && song.uri.startsWith("content://")) {
                    runCatching {
                        pendingDeleteSong = song
                        val pi = MediaStore.createDeleteRequest(
                            context.contentResolver,
                            listOf(Uri.parse(song.uri))
                        )
                        onNeedIntentSender(pi.intentSender)
                    }.onFailure {
                        statusMessage.value = "Не удалось запросить удаление файла"
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && secEx is RecoverableSecurityException) {
                    pendingDeleteSong = song
                    onNeedIntentSender(secEx.userAction.actionIntent.intentSender)
                } else {
                    statusMessage.value = "Нет прав для удаления файла с устройства"
                }
            } catch (_: Exception) {
                statusMessage.value = "Ошибка при удалении файла с устройства"
            }
        }
    }

    fun onDeviceDeletePermissionResult(granted: Boolean) {
        val song = pendingDeleteSong ?: return
        pendingDeleteSong = null
        if (granted) {
            finalizeSongDeletionFromState(song)
        } else {
            statusMessage.value = "Удаление файла отменено"
        }
    }

    private fun finalizeSongDeletionFromState(song: Song) {
        rawAllSongs = rawAllSongs.filterNot { it.id == song.id }
        allSongs.value = allSongs.value.filterNot { it.id == song.id }
        localSongsCount.value = allSongs.value.count { it.isLocal }

        val qIdx = queue.value.indexOfFirst { it.id == song.id }
        if (qIdx >= 0) {
            removeQueueItemAt(qIdx)
        }
        if (currentSong.value?.id == song.id) {
            currentSong.value = queue.value.firstOrNull() ?: allSongs.value.firstOrNull()
        }
        rebuildPlaylists(allSongs.value)
        statusMessage.value = "Трек «${song.title}» удалён с устройства"
    }

    fun toggleFavorite(song: Song) {
        val updated = favoriteIds.value.toMutableSet()
        val added = if (updated.contains(song.id)) {
            updated.remove(song.id)
            false
        } else {
            updated.add(song.id)
            if (song.isRemoteYt || song.id.startsWith("yt_")) {
                val saved = (listOf(song) + savedYtSongs.value).distinctBy { it.id }
                savedYtSongs.value = saved
                prefs.saveYtSongs(saved)
            }
            true
        }
        favoriteIds.value = updated
        prefs.saveFavoriteIds(updated)
        statusMessage.value = if (added) "Добавлено в «Мне нравится»" else "Удалено из «Мне нравится»"
    }

    fun createPlaylist(name: String, initialSongs: List<Song> = emptyList()) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val newPlaylist = Playlist(
            id = "pl_${System.currentTimeMillis()}",
            name = cleanName,
            songs = initialSongs,
            coverColorSeed = cleanName.hashCode()
        )
        val updated = customPlaylists.value + newPlaylist
        customPlaylists.value = updated
        persistPlaylists(updated)
        statusMessage.value = "Плейлист «$cleanName» создан"
    }

    fun deletePlaylist(playlistId: String) {
        val updated = customPlaylists.value.filterNot { it.id == playlistId }
        customPlaylists.value = updated
        persistPlaylists(updated)
        statusMessage.value = "Плейлист удалён"
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        if (playlistId == "sys_favorites") {
            if (!isFavorite(song.id)) toggleFavorite(song)
            return
        }
        if (song.isRemoteYt || song.id.startsWith("yt_")) {
            val saved = (listOf(song) + savedYtSongs.value).distinctBy { it.id }
            savedYtSongs.value = saved
            prefs.saveYtSongs(saved)
        }
        val updated = customPlaylists.value.map { pl ->
            if (pl.id == playlistId) {
                if (pl.songs.any { it.id == song.id }) {
                    statusMessage.value = "Трек уже есть в плейлисте «${pl.name}»"
                    pl
                } else {
                    statusMessage.value = "Добавлено в «${pl.name}»"
                    pl.copy(songs = pl.songs + song)
                }
            } else pl
        }
        customPlaylists.value = updated
        persistPlaylists(updated)
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        if (playlistId == "sys_favorites") {
            (allSongs.value + savedYtSongs.value).find { it.id == songId }?.let { toggleFavorite(it) }
            return
        }
        val updated = customPlaylists.value.map { pl ->
            if (pl.id == playlistId) {
                pl.copy(songs = pl.songs.filterNot { it.id == songId })
            } else pl
        }
        customPlaylists.value = updated
        persistPlaylists(updated)
    }

    fun removeQueueItemAt(index: Int) {
        val q = queue.value.toMutableList()
        if (index !in q.indices) return
        q.removeAt(index)
        queue.value = q
        if (index < exoPlayer.mediaItemCount) {
            exoPlayer.removeMediaItem(index)
        }
    }

    fun setQueueAndPlay(songs: List<Song>, startIndex: Int, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        val effectiveShuffle = shuffle || shuffleOn.value
        shuffleOn.value = effectiveShuffle
        exoPlayer.shuffleModeEnabled = effectiveShuffle

        val effectiveQueue = if (effectiveShuffle) songs.shuffled() else songs
        val targetIdx = if (effectiveShuffle) 0 else startIndex.coerceIn(0, effectiveQueue.lastIndex)

        queue.value = effectiveQueue
        val targetSong = effectiveQueue[targetIdx]
        currentSong.value = targetSong
        durationMs.value = targetSong.durationMs
        positionMs.value = 0L
        prefs.saveLastPlayedSongId(targetSong.id)
        recordRecentlyPlayed(targetSong)
        loadLyricsFor(targetSong)

        startPlaybackServiceIfNeeded()

        val mediaItems = effectiveQueue.map { it.toMediaItem() }
        exoPlayer.setMediaItems(mediaItems, targetIdx, 0L)
        exoPlayer.prepare()
        exoPlayer.play()
        isPlaying.value = true
    }

    fun playSong(song: Song) {
        val currentQueue = queue.value.ifEmpty { allSongs.value }
        val idx = currentQueue.indexOfFirst { it.id == song.id }
        if (idx >= 0) {
            setQueueAndPlay(currentQueue, idx)
        } else {
            val newQueue = listOf(song) + currentQueue
            setQueueAndPlay(newQueue, 0)
        }
    }

    fun playNextInQueue(song: Song) {
        val q = queue.value.toMutableList()
        val current = currentSong.value
        if (current == null || q.isEmpty()) {
            setQueueAndPlay(listOf(song), 0)
            return
        }
        val curIdx = q.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
        val insertIdx = (curIdx + 1).coerceAtMost(q.size)
        q.add(insertIdx, song)
        queue.value = q
        exoPlayer.addMediaItem(insertIdx, song.toMediaItem())
        statusMessage.value = "«${song.title}» сыграет следующим"
    }

    fun togglePlayPause() {
        val song = currentSong.value ?: allSongs.value.firstOrNull() ?: return
        if (exoPlayer.mediaItemCount == 0) {
            val q = queue.value.ifEmpty { allSongs.value }
            val idx = q.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            setQueueAndPlay(q, idx)
            return
        }
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            isPlaying.value = false
        } else {

            startPlaybackServiceIfNeeded()
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0L)
            }
            exoPlayer.play()
            isPlaying.value = true
        }
    }

    fun seekTo(ms: Long) {
        val target = ms.coerceAtLeast(0L)
        exoPlayer.seekTo(target)
        positionMs.value = target
    }

    fun skipNext() {
        val q = queue.value
        if (q.isEmpty()) return
        if (shuffleOn.value) {
            if (exoPlayer.hasNextMediaItem()) {
                exoPlayer.seekToNextMediaItem()
                exoPlayer.play()
            } else {
                val nextIdx = q.indices.random()
                setQueueAndPlay(q, nextIdx, shuffle = true)
            }
        } else {
            val current = currentSong.value
            val idx = if (current != null) q.indexOfFirst { it.id == current.id } else exoPlayer.currentMediaItemIndex
            val nextIdx = if (idx >= 0 && idx < q.lastIndex) idx + 1 else 0
            if (nextIdx == 0 && repeatMode.value == RepeatMode.OFF && idx == q.lastIndex) {
                isPlaying.value = false
                positionMs.value = 0L
                seekTo(0L)
            } else {
                if (nextIdx in 0 until exoPlayer.mediaItemCount && !exoPlayer.shuffleModeEnabled) {
                    exoPlayer.seekToDefaultPosition(nextIdx)
                    exoPlayer.play()
                } else {
                    setQueueAndPlay(q, nextIdx, shuffle = false)
                }
            }
        }
    }

    fun skipPrevious() {
        if (exoPlayer.currentPosition > 3500L) {
            seekTo(0L)
            return
        }
        val q = queue.value
        if (q.isEmpty()) return
        if (shuffleOn.value) {
            if (exoPlayer.hasPreviousMediaItem()) {
                exoPlayer.seekToPreviousMediaItem()
                exoPlayer.play()
            } else {
                val prevIdx = q.indices.random()
                setQueueAndPlay(q, prevIdx, shuffle = true)
            }
        } else {
            val current = currentSong.value
            val idx = if (current != null) q.indexOfFirst { it.id == current.id } else exoPlayer.currentMediaItemIndex
            val prevIdx = if (idx > 0) idx - 1 else (if (repeatMode.value == RepeatMode.ALL) q.lastIndex else 0)
            if (prevIdx in 0 until exoPlayer.mediaItemCount && !exoPlayer.shuffleModeEnabled) {
                exoPlayer.seekToDefaultPosition(prevIdx)
                exoPlayer.play()
            } else {
                setQueueAndPlay(q, prevIdx, shuffle = false)
            }
        }
    }

    fun toggleShuffle() {
        val newValue = !shuffleOn.value
        shuffleOn.value = newValue
        exoPlayer.shuffleModeEnabled = newValue
    }

    fun cycleRepeatMode() {
        val next = when (repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        repeatMode.value = next
        exoPlayer.repeatMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        playbackSpeed.value = clamped
        exoPlayer.setPlaybackSpeed(clamped)
        prefs.savePlaybackSpeed(clamped)
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            sleepTimerRemainingSec.value = null
            statusMessage.value = "Таймер сна выключен"
            return
        }
        val totalSeconds = minutes * 60
        sleepTimerRemainingSec.value = totalSeconds
        statusMessage.value = "Таймер сна установлен на $minutes мин."
        sleepTimerJob = viewModelScope.launch {
            var rem = totalSeconds
            while (rem > 0) {
                delay(1000)
                rem--
                sleepTimerRemainingSec.value = rem
            }
            sleepTimerRemainingSec.value = null
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
                isPlaying.value = false
            }
            statusMessage.value = "Воспроизведение остановлено по таймеру сна"
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
        prefs.saveThemeMode(mode)
    }

    fun setUseDynamicColor(enabled: Boolean) {
        useDynamicColor.value = enabled
        prefs.saveUseDynamicColor(enabled)
    }

    fun setUseSquigglySlider(enabled: Boolean) {
        useSquigglySlider.value = enabled
        prefs.saveUseSquigglySlider(enabled)
    }

    fun setEnableBlur(enabled: Boolean) {
        enableBlur.value = enabled
        prefs.saveEnableBlur(enabled)
    }

    fun setSearchBarPlacement(placement: SearchBarPlacement) {
        searchBarPlacement.value = placement
        prefs.saveSearchBarPlacement(placement)
    }

    fun setEnablePlayerSwipe(enabled: Boolean) {
        enablePlayerSwipe.value = enabled
        prefs.saveEnablePlayerSwipe(enabled)
    }

    fun setHideDemoTracks(hide: Boolean) {
        hideDemoTracks.value = hide
        prefs.saveHideDemoTracks(hide)
        if (hide && selectedFilter.value == LibraryFilter.DEMO) {
            setLibraryFilter(LibraryFilter.ALL)
        }
    }

    fun setSortOption(option: SortOption) {
        selectedSort.value = option
        prefs.saveSortOption(option)
    }

    fun setLibraryFilter(filter: LibraryFilter) {
        selectedFilter.value = filter
        prefs.saveLibraryFilter(filter)
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }

    private fun startPlaybackServiceIfNeeded() {
        try {
            val app = getApplication<Application>()
            val intent = Intent(app, PlaybackService::class.java)
            app.startService(intent)
        } catch (_: Exception) {

        }
    }

    private fun Song.toMediaItem(): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
        if (!albumArtUri.isNullOrBlank()) {
            metadataBuilder.setArtworkUri(Uri.parse(albumArtUri))
        }
        val effectiveUri = if (id.startsWith("yt_v_") && !isLocal) {
            "yt://${id.removePrefix("yt_v_")}"
        } else {
            uri
        }
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(effectiveUri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    fun setUseLiquidGlass(enabled: Boolean) {
        useLiquidGlass.value = enabled
        prefs.saveUseLiquidGlass(enabled)
    }

    fun setClassicLibraryDesign(enabled: Boolean) {
        classicLibraryDesign.value = enabled
        prefs.setClassicLibraryDesign(enabled)
    }

    fun openArtistProfile(artist: String) {
        val trimmed = artist.trim()
        if (trimmed.isNotBlank()) {
            selectedArtistProfile.value = trimmed
        }
    }

    fun closeArtistProfile() {
        selectedArtistProfile.value = null
    }

    fun setEnableBetaUpdates(enabled: Boolean) {
        enableBetaUpdates.value = enabled
        prefs.saveEnableBetaUpdates(enabled)
        checkForUpdates(silent = true)
    }

    fun setUseAlbumArtGradient(enabled: Boolean) {
        useAlbumArtGradient.value = enabled
        prefs.saveUseAlbumArtGradient(enabled)
    }

    fun setAutoTagFixEnabled(enabled: Boolean) {
        autoTagFixEnabled.value = enabled
        prefs.saveAutoTagFix(enabled)
        if (enabled) {
            checkBrokenTracksForAutoTagFix()
        }
    }

    fun setRecentsViewMode(mode: String) {
        recentsViewMode.value = mode
        prefs.saveRecentsViewMode(mode)
    }

    fun setAppLanguage(lang: com.lormusic.app.model.AppLanguage) {
        appLanguage.value = lang
        prefs.saveAppLanguage(lang)
    }

    fun dismissAutoTagFixPrompt() {
        showAutoTagFixPrompt.value = false
    }

    fun checkBrokenTracksForAutoTagFix() {
        if (!autoTagFixEnabled.value) return
        val broken = allSongs.value.filter { it.isLocal && TagFixEngine.isBrokenTrack(it) }
        autoTagFixDetectedSongs.value = broken
        if (broken.isNotEmpty()) {
            showAutoTagFixPrompt.value = true
        }
    }

    fun recognizeSongWithShazam(song: Song, onResult: (TagFixProposal?) -> Unit) {
        viewModelScope.launch {
            val proposal = TagFixEngine.recognizeTrackShazam(getApplication(), song)
            onResult(proposal)
        }
    }

    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) updateState.value = UpdateState.Checking
            val result = updateManager.checkForUpdates("1.6.0", includeBeta = enableBetaUpdates.value)
            updateState.value = result
            if (result is UpdateState.Available) {
                availableUpdateInfo.value = result.info
                showUpdateDialog.value = true
            } else if (!silent) {
                if (result is UpdateState.UpToDate) {
                    statusMessage.value = "У вас установлена актуальная версия (1.6.0)"
                } else if (result is UpdateState.Error) {
                    statusMessage.value = result.message
                }
            }
        }
    }

    fun startDownloadingUpdate(info: AppUpdateInfo) {
        viewModelScope.launch {
            updateState.value = UpdateState.Downloading(0f, 0L, 1L)
            val file = updateManager.downloadUpdate(info) { progress, downloaded, total ->
                updateState.value = UpdateState.Downloading(progress, downloaded, total)
            }
            if (file != null && file.exists()) {
                updateState.value = UpdateState.ReadyToInstall(file)
                updateManager.installApk(file)
            } else {
                updateState.value = UpdateState.Error("Не удалось скачать обновление")
            }
        }
    }

    fun installDownloadedUpdate(file: File) {
        updateManager.installApk(file)
    }

    fun dismissUpdateDialog() {
        showUpdateDialog.value = false
    }

    override fun onCleared() {
        exoPlayer.removeListener(playerListener)
        super.onCleared()
    }
}
