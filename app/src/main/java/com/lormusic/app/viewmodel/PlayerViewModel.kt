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
import androidx.core.content.ContextCompat
import androidx.compose.runtime.mutableStateOf
import java.io.File
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.lormusic.app.BuildConfig
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
    val localFavoriteIds = mutableStateOf(prefs.getLocalFavoriteIds())
    val ytFavoriteIds = mutableStateOf(prefs.getYtFavoriteIds())
    val favoriteIds = mutableStateOf(localFavoriteIds.value)
    val topPlayedSong = mutableStateOf(prefs.getTopPlayedSong())
    val totalListeningTimeMs = mutableStateOf(prefs.getTotalListeningTimeMs())
    val topAlbum = mutableStateOf(prefs.getTopAlbum())
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
    val betaSeparateHeaderBubbles = mutableStateOf(prefs.getBetaSeparateHeaderBubbles())
    val betaDynamicPlaybackContext = mutableStateOf(prefs.getBetaDynamicPlaybackContext())
    val currentPlaybackContext = mutableStateOf<PlaybackContext?>(null)
    val useAlbumArtGradient = mutableStateOf(prefs.getUseAlbumArtGradient())
    val useMyWaveCoverGradient = mutableStateOf(prefs.getUseMyWaveCoverGradient())
    val useDynamicPlayerColors = mutableStateOf(prefs.getUseDynamicPlayerColors())
    val pureMaterialYou = mutableStateOf(prefs.getPureMaterialYou())
    val showNavBarLabels = mutableStateOf(prefs.getShowNavBarLabels())
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
    val isHalloweenMode = mutableStateOf(prefs.getHalloweenMode())
    val halloweenCandies = mutableStateOf(prefs.getHalloweenCandies())
    val showHalloweenGreeting = mutableStateOf(false)
    val triggerHalloweenTrickJumpscare = mutableStateOf(false)

    val isYtMusicMode = mutableStateOf(false)
    val activeYtTab = mutableStateOf(YtMusicTab.HOME)
    val ytRecommendations = mutableStateOf<List<Song>>(emptyList())
    val ytRadioMixes = mutableStateOf<List<Pair<String, List<Song>>>>(emptyList())
    val ytMyWaveSongs = mutableStateOf<List<Song>>(emptyList())
    val ytSearchResults = mutableStateOf<List<Song>>(emptyList())
    val isLoadingYtFeed = mutableStateOf(false)
    val isSearchingYt = mutableStateOf(false)
    val downloadingSongIds = mutableStateOf<Set<String>>(emptySet())
    val downloadingSongs = mutableStateOf<Map<String, Song>>(emptyMap())
    val downloadProgress = mutableStateOf<Map<String, Float>>(emptyMap())
    val savedYtSongs = mutableStateOf(loadCleanSavedYtSongs())
    val ytMusicCookie = mutableStateOf(prefs.getYtMusicCookie())
    val googleAccount = mutableStateOf<GoogleAccountProfile?>(loadInitialGoogleProfile())
    private var ytSearchJob: Job? = null

    val isSoundCloudMode = mutableStateOf(false)
    val activeScTab = mutableStateOf(com.lormusic.app.ui.components.ScTab.HOME)
    val scFavoriteIds = mutableStateOf(prefs.getScFavoriteIds())
    val unifiedFavoriteIds = mutableStateOf(prefs.getUnifiedFavoriteIds())
    val savedScSongs = mutableStateOf(prefs.getSavedScSongs())
    val soundCloudProfile = mutableStateOf<SoundCloudUserProfile?>(prefs.getScProfile())
    val soundCloudAuthToken = mutableStateOf<String?>(prefs.getScAuthToken())
    val soundCloudSearchResults = mutableStateOf<List<Song>>(emptyList())
    val soundCloudCharts = mutableStateOf<List<Song>>(emptyList())
    val soundCloudLikes = mutableStateOf<List<Song>>(emptyList())
    val isLoadingSoundCloud = mutableStateOf(false)
    val isSearchingSoundCloud = mutableStateOf(false)
    private var scSearchJob: Job? = null

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

    val classicLibraryDesign = mutableStateOf(false)
    val showClassicRemovedNotice = mutableStateOf(prefs.getClassicLibraryDesign() && !prefs.hasSeenClassicRemovedNotice())
    val selectedArtistProfile = mutableStateOf<String?>(null)
    val currentArtistProfileData = mutableStateOf<com.lormusic.app.data.ArtistProfileData?>(null)
    val isArtistProfileLoading = mutableStateOf(false)

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
                recordSongPlay(withTags)
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
            statusMessage.value = when (appLanguage.value) {
                AppLanguage.EN -> "Failed to play track: ${error.localizedMessage ?: "source error"}"
                AppLanguage.UK -> "Не вдалося відтворити трек: ${error.localizedMessage ?: "помилка джерела"}"
                else -> "Не удалось воспроизвести трек: ${error.localizedMessage ?: "ошибка источника"}"
            }
            isPlaying.value = false
        }
    }

    init {
        exoPlayer.addListener(playerListener)
        exoPlayer.setPlaybackSpeed(playbackSpeed.value)
        startPlaybackServiceIfNeeded()
        checkAndEnforceHalloweenExpiry()
        checkHalloweenAutoTrigger()

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

            val existingScToken = soundCloudAuthToken.value
            if (!existingScToken.isNullOrBlank()) {
                val prof = prefs.getScProfile()
                if (existingScToken == "guest" || prof?.username == "Гость") {
                    soundCloudAuthToken.value = null
                    soundCloudProfile.value = null
                    prefs.saveScAuthToken(null)
                    prefs.saveScProfile(null)
                    SoundCloudRepository.currentAuthToken = null
                    SoundCloudRepository.currentProfile = null
                } else {
                    SoundCloudRepository.currentAuthToken = existingScToken
                    if (prof != null) {
                        SoundCloudRepository.currentProfile = prof
                        soundCloudProfile.value = prof
                    }
                }
            }

            if (autoTagFixEnabled.value) {
                checkBrokenTracksForAutoTagFix()
            }

            delay(2000)
            checkForUpdates(silent = true)
        }

        viewModelScope.launch {
            var counter = 0
            while (true) {
                delay(100)
                if (exoPlayer.isPlaying) {
                    positionMs.value = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val realDur = exoPlayer.duration
                    if (realDur > 0 && realDur != durationMs.value) {
                        durationMs.value = realDur
                    }
                    counter++
                    if (counter >= 10) {
                        counter = 0
                        prefs.addListeningTimeMs(1000L)
                        totalListeningTimeMs.value = prefs.getTotalListeningTimeMs()
                    }
                }
            }
        }
    }

    fun getRecentlyPlayedSongs(): List<Song> {
        if (googleAccount.value == null) {
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

    fun recordSongPlay(song: Song) {
        prefs.recordSongPlay(song.id, song.title, song.artist, song.album)
        topPlayedSong.value = prefs.getTopPlayedSong()
        topAlbum.value = prefs.getTopAlbum()
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
        statusMessage.value = when (appLanguage.value) {
            AppLanguage.EN -> "Tags for «${override.title}» saved"
            AppLanguage.UK -> "Теги треку «${override.title}» збережено"
            else -> "Теги трека «${override.title}» сохранены"
        }
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
        statusMessage.value = when (appLanguage.value) {
            AppLanguage.EN -> "TagFix successfully updated tags and artwork (${proposals.size} tracks)"
            AppLanguage.UK -> "TagFix успішно оновив теги та обкладинки (${proposals.size} треків)"
            else -> "TagFix успешно обновил теги и обложки (${proposals.size} треков)"
        }
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
        val currentStrings = resolveStrings(appLanguage.value)
        statusMessage.value = when (appLanguage.value) {
            AppLanguage.EN -> "Icon changed to «${option.getDisplayName(currentStrings)}»"
            AppLanguage.UK -> "Іконку змінено на «${option.getDisplayName(currentStrings)}»"
            else -> "Иконка изменена на «${option.getDisplayName(currentStrings)}»"
        }
        } catch (_: Exception) {
        val currentStrings = resolveStrings(appLanguage.value)
        statusMessage.value = when (appLanguage.value) {
            AppLanguage.EN -> "Icon selected: «${option.getDisplayName(currentStrings)}»"
            AppLanguage.UK -> "Обрано іконку: «${option.getDisplayName(currentStrings)}»"
            else -> "Выбрана иконка «${option.getDisplayName(currentStrings)}»"
        }
        }
    }

    fun checkAndEnforceHalloweenExpiry() {
        if (AppIconOption.isHalloweenExpired()) {
            if (selectedAppIcon.value == AppIconOption.HALLOWEEN) {
                setAppIconOption(AppIconOption.REGULAR)
                if (!prefs.getHalloweenExpiredNotified()) {
                    prefs.saveHalloweenExpiredNotified(true)
                    val currentStrings = resolveStrings(appLanguage.value)
                    statusMessage.value = currentStrings.halloweenEndedNotice
                }
            }
            if (halloweenCandies.value) {
                setHalloweenCandies(false)
            }
            if (isHalloweenMode.value) {
                setHalloweenMode(false)
            }
        }
    }

    fun setHalloweenMode(enabled: Boolean) {
        isHalloweenMode.value = enabled
        prefs.saveHalloweenMode(enabled)
    }

    fun setHalloweenCandies(enabled: Boolean) {
        halloweenCandies.value = enabled
        prefs.saveHalloweenCandies(enabled)
    }

    fun triggerHalloweenTest() {
        showHalloweenGreeting.value = true
    }

    fun selectHalloweenTreat() {
        showHalloweenGreeting.value = false
        setHalloweenCandies(true)
        setHalloweenMode(true)
        val s = resolveStrings(appLanguage.value)
        statusMessage.value = s.halloweenTreatSelected
    }

    fun selectHalloweenTrick() {
        showHalloweenGreeting.value = false
        triggerHalloweenTrickJumpscare.value = true
    }

    fun dismissHalloweenGreeting() {
        showHalloweenGreeting.value = false
    }

    fun checkHalloweenAutoTrigger() {
        if (AppIconOption.isHalloweenExpired()) {
            checkAndEnforceHalloweenExpiry()
            return
        }
        val cal = java.util.Calendar.getInstance()
        val month = cal.get(java.util.Calendar.MONTH)
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        if (month == java.util.Calendar.OCTOBER && day == 31) {
            if (!isHalloweenMode.value) {
                setHalloweenMode(true)
            }
            val year = cal.get(java.util.Calendar.YEAR)
            val todayKey = "$year-10-31"
            if (!prefs.hasSeenHalloweenGreeting(todayKey)) {
                prefs.saveSeenHalloweenGreeting(todayKey)
                showHalloweenGreeting.value = true
            }
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

    fun enterSoundCloudMode() {
        isSoundCloudMode.value = true
        activeScTab.value = com.lormusic.app.ui.components.ScTab.HOME
        if (soundCloudCharts.value.isEmpty()) {
            loadSoundCloudCharts()
        }
    }

    fun exitSoundCloudMode() {
        isSoundCloudMode.value = false
    }

    fun selectScTab(tab: com.lormusic.app.ui.components.ScTab) {
        activeScTab.value = tab
        if (tab == com.lormusic.app.ui.components.ScTab.CHARTS && soundCloudCharts.value.isEmpty()) {
            loadSoundCloudCharts()
        }
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
        if (googleAccount.value == null) {
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
            val mixLabel = when (appLanguage.value) {
                AppLanguage.EN -> "Mix for You"
                AppLanguage.UK -> "Мікс для тебе"
                else -> "Микс для тебя"
            }
            add(mixLabel to recs.take(10))
                }
                if (radioTracks.isNotEmpty()) {
            val radioLabel = when (appLanguage.value) {
                AppLanguage.EN -> "YouTube Music Radio"
                AppLanguage.UK -> "Радіо YouTube Music"
                else -> "Радио YouTube Music"
            }
            add(radioLabel to radioTracks.take(10))
            val nightLabel = when (appLanguage.value) {
                AppLanguage.EN -> "Night Vibe"
                AppLanguage.UK -> "Нічний вайб"
                else -> "Ночной вайб"
            }
            add(nightLabel to radioTracks.shuffled().take(8))
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
        viewModelScope.launch {
            val seedSongs = getRecentlyPlayedSongs() + allSongs.value.filter { favoriteIds.value.contains(it.id) }
            val wave = if (!moodQuery.isNullOrBlank()) {
                val r1 = YouTubeMusicRepository.searchYtMusic(moodQuery, limit = 35, cookie = cookie)
                val r2 = if (r1.size < 50) YouTubeMusicRepository.searchYtMusic("$moodQuery hits radio", limit = 35, cookie = cookie) else emptyList()
                (r1 + r2).distinctBy { it.id }.take(50)
            } else {
                YouTubeMusicRepository.loadMyWaveTracks(seedSongs, YouTubeMusicRepository.WaveMood.FOR_YOU, cookie = cookie)
            }
            ytMyWaveSongs.value = wave.take(50)
        }
    }

    fun searchYtMusic(query: String) {
        ytSearchJob?.cancel()
        val cookie = ytMusicCookie.value
        if (query.isBlank()) {
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
            downloadingSongs.value = downloadingSongs.value + (song.id to song)
            downloadProgress.value = downloadProgress.value + (song.id to 0f)
            val result = YouTubeMusicRepository.downloadTrackToPhoneWithTags(getApplication(), song) { frac ->
                downloadProgress.value = downloadProgress.value + (song.id to frac)
            }
            downloadingSongIds.value = downloadingSongIds.value - song.id
            downloadingSongs.value = downloadingSongs.value - song.id
            downloadProgress.value = downloadProgress.value - song.id

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
                statusMessage.value = when (appLanguage.value) {
                    AppLanguage.EN -> "Track «${downloaded.title}» downloaded locally with tags and artwork!"
                    AppLanguage.UK -> "Трек «${downloaded.title}» завантажено локально з тегами та обкладинкою!"
                    else -> "Трек «${downloaded.title}» скачан локально со всеми тегами и обложкой!"
                }
            } else {
                statusMessage.value = when (appLanguage.value) {
                    AppLanguage.EN -> "Failed to download track «${song.title}»"
                    AppLanguage.UK -> "Не вдалося завантажити трек «${song.title}»"
                    else -> "Не удалось скачать трек «${song.title}»"
                }
            }
        }
    }

    fun downloadScSongWithTags(song: Song) {
        if (downloadingSongIds.value.contains(song.id)) return
        viewModelScope.launch {
            downloadingSongIds.value = downloadingSongIds.value + song.id
            downloadingSongs.value = downloadingSongs.value + (song.id to song)
            downloadProgress.value = downloadProgress.value + (song.id to 0f)
            val result = SoundCloudRepository.downloadTrackToPhoneWithTags(getApplication(), song) { frac ->
                downloadProgress.value = downloadProgress.value + (song.id to frac)
            }
            downloadingSongIds.value = downloadingSongIds.value - song.id
            downloadingSongs.value = downloadingSongs.value - song.id
            downloadProgress.value = downloadProgress.value - song.id

            if (result != null) {
                val (downloaded, override) = result
                val updatedOverrides = tagOverrides.value.toMutableMap()
                updatedOverrides[override.songId] = override
                prefs.saveTagOverrides(updatedOverrides)
                tagOverrides.value = updatedOverrides

                rawAllSongs = listOf(downloaded) + rawAllSongs.filterNot { it.id == downloaded.id }
                applyTagOverridesToState(updatedOverrides)
                localSongsCount.value = allSongs.value.count { it.isLocal }

                val updatedSaved = listOf(downloaded) + savedScSongs.value.filterNot { it.id == song.id || it.id == downloaded.id }
                savedScSongs.value = updatedSaved
                prefs.saveScSongs(updatedSaved)
                if (autoTagFixEnabled.value) {
                    checkBrokenTracksForAutoTagFix()
                }
                statusMessage.value = when (appLanguage.value) {
                    AppLanguage.EN -> "Track «${downloaded.title}» downloaded locally from SoundCloud with tags and artwork!"
                    AppLanguage.UK -> "Трек «${downloaded.title}» завантажено локально з SoundCloud з тегами та обкладинкою!"
                    else -> "Трек «${downloaded.title}» скачан локально из SoundCloud со всеми тегами и обложкой!"
                }
            } else {
                statusMessage.value = when (appLanguage.value) {
                    AppLanguage.EN -> "Failed to download track «${song.title}» from SoundCloud"
                    AppLanguage.UK -> "Не вдалося завантажити трек «${song.title}» з SoundCloud"
                    else -> "Не удалось скачать трек «${song.title}» из SoundCloud"
                }
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
            displayName = when (appLanguage.value) {
                AppLanguage.EN -> "YouTube Music Account"
                AppLanguage.UK -> "Обліковий запис YouTube Music"
                else -> "Аккаунт YouTube Music"
            },
                email = "YouTube Music Sync"
            )
            googleAccount.value = profile
            prefs.saveGoogleAccount(profile.displayName, profile.email, profile.avatarUrl)

            val unfavorited = prefs.getUnfavoritedSongIds()
            val filteredLiked = syncData.likedSongs.filterNot {
                unfavorited.contains(it.id) || unfavorited.contains(it.id.removePrefix("yt_"))
            }
            val newSaved = (syncData.historySongs + filteredLiked + savedYtSongs.value)
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

            if (filteredLiked.isNotEmpty()) {
                val updatedFavs = ytFavoriteIds.value.toMutableSet()
                filteredLiked.forEach { updatedFavs.add(it.id) }
                ytFavoriteIds.value = updatedFavs
                prefs.saveYtFavoriteIds(updatedFavs)
            }

            if (syncData.homeRecommendations.isNotEmpty() || syncData.likedSongs.isNotEmpty()) {
                val recs = syncData.homeRecommendations.ifEmpty { syncData.likedSongs }
                val radioPool = (syncData.likedSongs + syncData.homeRecommendations).distinctBy { it.id }
                ytRecommendations.value = recs
                ytRadioMixes.value = buildList {
            val mixLabel = when (appLanguage.value) {
                AppLanguage.EN -> "Mix for You"
                AppLanguage.UK -> "Мікс для тебе"
                else -> "Микс для тебя"
            }
            add(mixLabel to recs.take(10))
                    if (radioPool.isNotEmpty()) {
            val radioLabel = when (appLanguage.value) {
                AppLanguage.EN -> "YouTube Music Radio"
                AppLanguage.UK -> "Радіо YouTube Music"
                else -> "Радио YouTube Music"
            }
            add(radioLabel to radioPool.take(10))
            val nightLabel = when (appLanguage.value) {
                AppLanguage.EN -> "Night Vibe"
                AppLanguage.UK -> "Нічний вайб"
                else -> "Ночной вайб"
            }
            add(nightLabel to radioPool.shuffled().take(8))
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
        statusMessage.value = when (appLanguage.value) {
            AppLanguage.EN -> "Signed out of Google / YouTube Music"
            AppLanguage.UK -> "Ви вийшли з облікового запису Google / YouTube Music"
            else -> "Вы вышли из аккаунта Google / YouTube Music"
        }
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

    fun translateCurrentLyrics(targetLang: String) {
        val current = currentLyrics.value
        if (current.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val hasTrans = current.any { it.isTranslation }
            if (hasTrans) {
                currentLyrics.value = current.filter { !it.isTranslation }
                return@launch
            }
            val translated = LyricsRepository.translateLyricLines(current, targetLang)
            currentLyrics.value = translated
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
            statusMessage.value = when (appLanguage.value) {
                AppLanguage.EN -> if (scannedLocal.isEmpty()) "No local audio files found on device" else "Found local tracks: ${scannedLocal.size}"
                AppLanguage.UK -> if (scannedLocal.isEmpty()) "Локальні аудіофайли не знайдено на пристрої" else "Знайдено локальних треків: ${scannedLocal.size}"
                else -> if (scannedLocal.isEmpty()) "Локальные аудиофайлы не найдены на устройстве" else "Найдено локальных треков: ${scannedLocal.size}"
            }
        }
    }

    private fun rebuildPlaylists(songsPool: List<Song>) {
        val songMap = (songsPool + savedYtSongs.value + savedScSongs.value).associateBy { it.id }
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
                    coverColorSeed = sp.coverColorSeed,
                    isUnified = sp.isUnified
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
                    coverColorSeed = pl.coverColorSeed,
                    isUnified = pl.isUnified
                )
            }
        )
    }

    fun getAllPlaylistsWithFavorites(): List<Playlist> {
        val pool = (allSongs.value + savedYtSongs.value + savedScSongs.value).distinctBy { it.id }
        val strings = resolveStrings(appLanguage.value)
        val favSongs = pool.filter { localFavoriteIds.value.contains(it.id) }
        val favPlaylist = Playlist(
            id = "sys_favorites",
            name = strings.favorites,
            songs = favSongs,
            coverColorSeed = 777,
            isSystem = true,
            isUnified = false
        )
        val unifiedFavSongs = pool.filter {
            unifiedFavoriteIds.value.contains(it.id)
        }
        val unifiedFavPlaylist = Playlist(
            id = "sys_unified_favorites",
            name = strings.unifiedFavorites,
            songs = unifiedFavSongs,
            coverColorSeed = 888,
            isSystem = true,
            isUnified = true
        )
        return listOf(favPlaylist, unifiedFavPlaylist) + customPlaylists.value
    }

    fun isFavorite(songId: String): Boolean {
        return when {
            songId.startsWith("yt_") -> ytFavoriteIds.value.contains(songId)
            songId.startsWith("sc_") -> scFavoriteIds.value.contains(songId)
            else -> localFavoriteIds.value.contains(songId)
        }
    }

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
            when (appLanguage.value) {
                AppLanguage.EN -> "Track «${song.title}» hidden from stream"
                AppLanguage.UK -> "Трек «${song.title}» приховано з потоку"
                else -> "Трек «${song.title}» скрыт из потока"
            }
        } else {
            when (appLanguage.value) {
                AppLanguage.EN -> "Track «${song.title}» restored to stream"
                AppLanguage.UK -> "Трек «${song.title}» повернено до потоку"
                else -> "Трек «${song.title}» возвращён в основной поток"
            }
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
            statusMessage.value = when (appLanguage.value) {
                AppLanguage.EN -> "Failed to request file deletion"
                AppLanguage.UK -> "Не вдалося запросити видалення файлу"
                else -> "Не удалось запросить удаление файла"
            }
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && secEx is RecoverableSecurityException) {
                    pendingDeleteSong = song
                    onNeedIntentSender(secEx.userAction.actionIntent.intentSender)
                } else {
            statusMessage.value = when (appLanguage.value) {
                AppLanguage.EN -> "Permission denied to delete file"
                AppLanguage.UK -> "Немає прав для видалення файлу з пристрою"
                else -> "Нет прав для удаления файла с устройства"
            }
                }
            } catch (_: Exception) {
            statusMessage.value = when (appLanguage.value) {
                AppLanguage.EN -> "Error deleting file from device"
                AppLanguage.UK -> "Помилка при видаленні файлу з пристрою"
                else -> "Ошибка при удалении файла с устройства"
            }
            }
        }
    }

    fun onDeviceDeletePermissionResult(granted: Boolean) {
        val song = pendingDeleteSong ?: return
        pendingDeleteSong = null
        if (granted) {
            finalizeSongDeletionFromState(song)
        } else {
        statusMessage.value = when (appLanguage.value) {
            AppLanguage.EN -> "File deletion cancelled"
            AppLanguage.UK -> "Видалення файлу скасовано"
            else -> "Удаление файла отменено"
        }
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
            statusMessage.value = when (appLanguage.value) {
                AppLanguage.EN -> "Track «${song.title}» deleted from device"
                AppLanguage.UK -> "Трек «${song.title}» видалено з пристрою"
                else -> "Трек «${song.title}» удалён с устройства"
            }
    }

    fun toggleFavorite(song: Song) {
        val isYt = song.isRemoteYt || song.id.startsWith("yt_")
        val isSc = song.isRemoteSc || song.id.startsWith("sc_")
        val currentFavs = when {
            isYt -> ytFavoriteIds.value
            isSc -> scFavoriteIds.value
            else -> localFavoriteIds.value
        }
        val updated = currentFavs.toMutableSet()
        val cookie = ytMusicCookie.value
        val added = if (updated.contains(song.id)) {
            updated.remove(song.id)
            prefs.addUnfavoritedSongId(song.id)
            if (isYt) {
                prefs.addUnfavoritedSongId(song.id.removePrefix("yt_"))
                if (!cookie.isNullOrBlank()) {
                    viewModelScope.launch(Dispatchers.IO) {
                        YouTubeMusicRepository.removeLikeFromYtMusic(song.id, cookie)
                    }
                }
            } else if (isSc) {
                soundCloudLikes.value = soundCloudLikes.value.filterNot { it.id == song.id }
            }
            false
        } else {
            updated.add(song.id)
            prefs.removeUnfavoritedSongId(song.id)
            if (isYt) {
                prefs.removeUnfavoritedSongId(song.id.removePrefix("yt_"))
                if (!cookie.isNullOrBlank()) {
                    viewModelScope.launch(Dispatchers.IO) {
                        YouTubeMusicRepository.likeYtMusicVideo(song.id, cookie)
                    }
                }
                val saved = (listOf(song) + savedYtSongs.value).distinctBy { it.id }
                savedYtSongs.value = saved
                prefs.saveYtSongs(saved)
            } else if (isSc) {
                val saved = (listOf(song) + savedScSongs.value).distinctBy { it.id }
                savedScSongs.value = saved
                prefs.saveScSongs(saved)
                soundCloudLikes.value = (listOf(song) + soundCloudLikes.value).distinctBy { it.id }
            }
            true
        }
        when {
            isYt -> {
                ytFavoriteIds.value = updated
                prefs.saveYtFavoriteIds(updated)
            }
            isSc -> {
                scFavoriteIds.value = updated
                prefs.saveScFavoriteIds(updated)
            }
            else -> {
                localFavoriteIds.value = updated
                favoriteIds.value = updated
                prefs.saveLocalFavoriteIds(updated)
            }
        }
        statusMessage.value = when (appLanguage.value) {
            AppLanguage.EN -> if (added) "Added to Favorites" else "Removed from Favorites"
            AppLanguage.UK -> if (added) "Додано до Улюбленого" else "Вилучено з Улюбленого"
            else -> if (added) "Добавлено в «Мне нравится»" else "Удалено из «Мне нравится»"
        }
    }

    fun createPlaylist(name: String, initialSongs: List<Song> = emptyList(), isUnified: Boolean = false) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val newPlaylist = Playlist(
            id = if (isUnified) "pl_uni_${System.currentTimeMillis()}" else "pl_${System.currentTimeMillis()}",
            name = cleanName,
            songs = initialSongs,
            coverColorSeed = cleanName.hashCode(),
            isUnified = isUnified
        )
        val updated = customPlaylists.value + newPlaylist
        customPlaylists.value = updated
        persistPlaylists(updated)
        statusMessage.value = when (appLanguage.value) {
            AppLanguage.EN -> "${if (isUnified) "Unified playlist" else "Playlist"} «$cleanName» created"
            AppLanguage.UK -> "${if (isUnified) "Спільний плейліст" else "Плейліст"} «$cleanName» створено"
            else -> "${if (isUnified) "Общий плейлист" else "Плейлист"} «$cleanName» создан"
        }
    }

    fun deletePlaylist(playlistId: String) {
        val updated = customPlaylists.value.filterNot { it.id == playlistId }
        customPlaylists.value = updated
        persistPlaylists(updated)
        statusMessage.value = when (appLanguage.value) {
            AppLanguage.EN -> "Playlist deleted"
            AppLanguage.UK -> "Плейліст видалено"
            else -> "Плейлист удалён"
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        if (playlistId == "sys_unified_favorites") {
            val current = unifiedFavoriteIds.value.toMutableSet()
            if (current.contains(song.id)) {
                current.remove(song.id)
                unifiedFavoriteIds.value = current
                prefs.saveUnifiedFavoriteIds(current)
                statusMessage.value = when (appLanguage.value) {
                    AppLanguage.EN -> "Removed from Unified Favorites"
                    AppLanguage.UK -> "Вилучено із загального обраного"
                    else -> "Удалено из общего избранного"
                }
            } else {
                current.add(song.id)
                unifiedFavoriteIds.value = current
                prefs.saveUnifiedFavoriteIds(current)
                if (song.isRemoteYt || song.id.startsWith("yt_")) {
                    val saved = (listOf(song) + savedYtSongs.value).distinctBy { it.id }
                    savedYtSongs.value = saved
                    prefs.saveYtSongs(saved)
                } else if (song.isRemoteSc || song.id.startsWith("sc_")) {
                    val saved = (listOf(song) + savedScSongs.value).distinctBy { it.id }
                    savedScSongs.value = saved
                    prefs.saveScSongs(saved)
                }
                statusMessage.value = when (appLanguage.value) {
                    AppLanguage.EN -> "Added to Unified Favorites"
                    AppLanguage.UK -> "Додано до загального обраного"
                    else -> "Добавлено в общее избранное"
                }
            }
            return
        }
        if (playlistId == "sys_favorites") {
            if (!isFavorite(song.id)) toggleFavorite(song)
            return
        }
        if (song.isRemoteYt || song.id.startsWith("yt_")) {
            val saved = (listOf(song) + savedYtSongs.value).distinctBy { it.id }
            savedYtSongs.value = saved
            prefs.saveYtSongs(saved)
        } else if (song.isRemoteSc || song.id.startsWith("sc_")) {
            val saved = (listOf(song) + savedScSongs.value).distinctBy { it.id }
            savedScSongs.value = saved
            prefs.saveScSongs(saved)
        }
        val updated = customPlaylists.value.map { pl ->
            if (pl.id == playlistId) {
                if (pl.songs.any { it.id == song.id }) {
                    statusMessage.value = when (appLanguage.value) {
                        AppLanguage.EN -> "Track is already in playlist «${pl.name}»"
                        AppLanguage.UK -> "Трек уже є в плейлісті «${pl.name}»"
                        else -> "Трек уже есть в плейлисте «${pl.name}»"
                    }
                    pl
                } else {
                    statusMessage.value = when (appLanguage.value) {
                        AppLanguage.EN -> "Added to «${pl.name}»"
                        AppLanguage.UK -> "Додано до «${pl.name}»"
                        else -> "Добавлено в «${pl.name}»"
                    }
                    pl.copy(songs = pl.songs + song)
                }
            } else pl
        }
        customPlaylists.value = updated
        persistPlaylists(updated)
    }

    fun loadSoundCloudCharts() {
        viewModelScope.launch {
            isLoadingSoundCloud.value = true
            val charts = SoundCloudRepository.fetchCharts(30)
            soundCloudCharts.value = charts
            if (!soundCloudAuthToken.value.isNullOrBlank()) {
                soundCloudLikes.value = SoundCloudRepository.fetchUserLikes(30)
            }
            isLoadingSoundCloud.value = false
        }
    }

    fun searchSoundCloud(query: String) {
        scSearchJob?.cancel()
        if (query.isBlank()) {
            soundCloudSearchResults.value = emptyList()
            isSearchingSoundCloud.value = false
            return
        }
        scSearchJob = viewModelScope.launch {
            isSearchingSoundCloud.value = true
            val results = SoundCloudRepository.searchSoundCloud(query, limit = 30)
            soundCloudSearchResults.value = results
            isSearchingSoundCloud.value = false
        }
    }

    fun onSoundCloudAuthSuccess(token: String) {
        val clean = SoundCloudRepository.cleanToken(token)
        if (clean.isBlank() || clean == "guest") return
        viewModelScope.launch {
            soundCloudAuthToken.value = clean
            prefs.saveScAuthToken(clean)
            SoundCloudRepository.currentAuthToken = clean
            var profile = SoundCloudRepository.fetchUserProfile(clean)
            if (profile == null) {
                kotlinx.coroutines.delay(600)
                profile = SoundCloudRepository.fetchUserProfile(clean)
            }
            val finalProfile = profile ?: SoundCloudUserProfile(username = "SoundCloud User", avatarUrl = null)
            soundCloudProfile.value = finalProfile
            prefs.saveScProfile(finalProfile)
            soundCloudLikes.value = SoundCloudRepository.fetchUserLikes(40)
            loadSoundCloudCharts()
            statusMessage.value = when (appLanguage.value) {
                AppLanguage.EN -> "Signed in to SoundCloud as ${finalProfile.username}"
                AppLanguage.UK -> "Успішний вхід у SoundCloud як ${finalProfile.username}"
                else -> "Вы успешно вошли в SoundCloud как ${finalProfile.username}"
            }
        }
    }

    fun signOutSoundCloud() {
        soundCloudAuthToken.value = null
        soundCloudProfile.value = null
        soundCloudLikes.value = emptyList()
        soundCloudCharts.value = emptyList()
        soundCloudSearchResults.value = emptyList()
        prefs.saveScAuthToken(null)
        prefs.saveScProfile(null)
        SoundCloudRepository.currentAuthToken = null
        SoundCloudRepository.currentProfile = null
        runCatching {
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
        }
        statusMessage.value = when (appLanguage.value) {
            AppLanguage.EN -> "Signed out of SoundCloud"
            AppLanguage.UK -> "Ви вийшли з SoundCloud"
            else -> "Вы вышли из SoundCloud"
        }
    }

    fun addSongsToPlaylist(playlistId: String, songs: List<Song>) {
        if (songs.isEmpty()) return
        if (playlistId == "sys_favorites") {
            songs.forEach { if (!isFavorite(it.id)) toggleFavorite(it) }
            return
        }
        val ytTracks = songs.filter { it.isRemoteYt || it.id.startsWith("yt_") }
        if (ytTracks.isNotEmpty()) {
            val saved = (ytTracks + savedYtSongs.value).distinctBy { it.id }
            savedYtSongs.value = saved
            prefs.saveYtSongs(saved)
        }
        val updated = customPlaylists.value.map { pl ->
            if (pl.id == playlistId) {
                val existingIds = pl.songs.map { it.id }.toSet()
                val newSongs = songs.filterNot { existingIds.contains(it.id) }
                statusMessage.value = when (appLanguage.value) {
                    AppLanguage.EN -> "Added ${newSongs.size} tracks to «${pl.name}»"
                    AppLanguage.UK -> "Додано ${newSongs.size} треків до «${pl.name}»"
                    else -> "Добавлено треков: ${newSongs.size} в «${pl.name}»"
                }
                pl.copy(songs = pl.songs + newSongs)
            } else pl
        }
        customPlaylists.value = updated
        persistPlaylists(updated)
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        if (playlistId == "sys_unified_favorites") {
            val current = unifiedFavoriteIds.value.toMutableSet()
            current.remove(songId)
            unifiedFavoriteIds.value = current
            prefs.saveUnifiedFavoriteIds(current)
            return
        }
        if (playlistId == "sys_favorites") {
            (allSongs.value + savedYtSongs.value + savedScSongs.value).find { it.id == songId }?.let { toggleFavorite(it) }
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

    fun setQueueAndPlay(songs: List<Song>, startIndex: Int, shuffle: Boolean = false, context: PlaybackContext? = null) {
        if (songs.isEmpty()) return
        if (context != null) {
            currentPlaybackContext.value = context
        }
        shuffleOn.value = shuffle
        exoPlayer.shuffleModeEnabled = false

        val effectiveQueue = if (shuffle) {
            val clicked = songs.getOrNull(startIndex) ?: songs.first()
            val remaining = songs.filter { it.id != clicked.id }.shuffled()
            listOf(clicked) + remaining
        } else songs
        val targetIdx = if (shuffle) 0 else startIndex.coerceIn(0, effectiveQueue.lastIndex)

        queue.value = effectiveQueue
        val targetSong = effectiveQueue[targetIdx]
        currentSong.value = targetSong
        durationMs.value = targetSong.durationMs
        positionMs.value = 0L
        prefs.saveLastPlayedSongId(targetSong.id)
        recordRecentlyPlayed(targetSong)
        recordSongPlay(targetSong)
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
        statusMessage.value = when (appLanguage.value) {
            AppLanguage.EN -> "«${song.title}» will play next"
            AppLanguage.UK -> "«${song.title}» гратиме наступним"
            else -> "«${song.title}» сыграет следующим"
        }
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
        val current = currentSong.value
        val idx = if (current != null) q.indexOfFirst { it.id == current.id } else exoPlayer.currentMediaItemIndex
        val nextIdx = if (idx >= 0 && idx < q.lastIndex) idx + 1 else 0
        if (nextIdx == 0 && repeatMode.value == RepeatMode.OFF && idx == q.lastIndex) {
            isPlaying.value = false
            positionMs.value = 0L
            seekTo(0L)
        } else {
            if (nextIdx in 0 until exoPlayer.mediaItemCount) {
                exoPlayer.seekToDefaultPosition(nextIdx)
                exoPlayer.play()
            } else {
                setQueueAndPlay(q, nextIdx, shuffle = shuffleOn.value)
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
        val current = currentSong.value
        val idx = if (current != null) q.indexOfFirst { it.id == current.id } else exoPlayer.currentMediaItemIndex
        val prevIdx = if (idx > 0) idx - 1 else (if (repeatMode.value == RepeatMode.ALL) q.lastIndex else 0)
        if (prevIdx in 0 until exoPlayer.mediaItemCount) {
            exoPlayer.seekToDefaultPosition(prevIdx)
            exoPlayer.play()
        } else {
            setQueueAndPlay(q, prevIdx, shuffle = shuffleOn.value)
        }
    }

    fun toggleShuffle() {
        val newValue = !shuffleOn.value
        shuffleOn.value = newValue
        exoPlayer.shuffleModeEnabled = false
        val current = currentSong.value
        val curQueue = queue.value
        if (newValue && current != null && curQueue.size > 1) {
            val remaining = curQueue.filter { it.id != current.id }.shuffled()
            val newQueue = listOf(current) + remaining
            queue.value = newQueue
            val mediaItems = newQueue.map { it.toMediaItem() }
            exoPlayer.setMediaItems(mediaItems, 0, exoPlayer.currentPosition)
        }
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
        statusMessage.value = when (appLanguage.value) {
            AppLanguage.EN -> "Sleep timer turned off"
            AppLanguage.UK -> "Таймер сну вимкнено"
            else -> "Таймер сна выключен"
        }
            return
        }
        val totalSeconds = minutes * 60
        sleepTimerRemainingSec.value = totalSeconds
        statusMessage.value = when (appLanguage.value) {
            AppLanguage.EN -> "Sleep timer set for $minutes min."
            AppLanguage.UK -> "Таймер сну встановлено на $minutes хв."
            else -> "Таймер сна установлен на $minutes мин."
        }
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
                        statusMessage.value = when (appLanguage.value) {
                            AppLanguage.EN -> "Playback stopped by sleep timer"
                            AppLanguage.UK -> "Відтворення зупинено за таймером сну"
                            else -> "Воспроизведение остановлено по таймеру сна"
                        }
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

    fun setPureMaterialYou(enabled: Boolean) {
        pureMaterialYou.value = enabled
        prefs.savePureMaterialYou(enabled)
    }

    fun setShowNavBarLabels(show: Boolean) {
        showNavBarLabels.value = show
        prefs.saveShowNavBarLabels(show)
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

    fun hasSeenBackupNotice(): Boolean = prefs.hasSeenBackupNotice()

    fun dismissBackupNotice() {
        prefs.setHasSeenBackupNotice(true)
    }

    fun exportBackup(): String = prefs.exportBackupJson()

    fun reloadAllPreferences() {
        themeMode.value = prefs.getThemeMode()
        useDynamicColor.value = prefs.getUseDynamicColor()
        useSquigglySlider.value = prefs.getUseSquigglySlider()
        enableBlur.value = prefs.getEnableBlur()
        pureMaterialYou.value = prefs.getPureMaterialYou()
        useLiquidGlass.value = prefs.getUseLiquidGlass()
        classicLibraryDesign.value = prefs.getClassicLibraryDesign()
        useDynamicPlayerColors.value = prefs.getUseDynamicPlayerColors()
        enableBetaUpdates.value = prefs.getEnableBetaUpdates()
        betaSeparateHeaderBubbles.value = prefs.getBetaSeparateHeaderBubbles()
        betaDynamicPlaybackContext.value = prefs.getBetaDynamicPlaybackContext()
        useAlbumArtGradient.value = prefs.getUseAlbumArtGradient()
        useMyWaveCoverGradient.value = prefs.getUseMyWaveCoverGradient()
        autoTagFixEnabled.value = prefs.getAutoTagFixEnabled()
        appLanguage.value = prefs.getAppLanguage()
        searchBarPlacement.value = prefs.getSearchBarPlacement()
        enablePlayerSwipe.value = prefs.getEnablePlayerSwipe()
        hideDemoTracks.value = prefs.getHideDemoTracks()
        selectedAppIcon.value = prefs.getAppIconOption()
        playbackSpeed.value = prefs.getPlaybackSpeed()
        showNavBarLabels.value = prefs.getShowNavBarLabels()
        favoriteIds.value = prefs.getFavoriteIds()
        hiddenSongIds.value = prefs.getHiddenSongIds()
        tagOverrides.value = prefs.getTagOverrides()
        savedYtSongs.value = loadCleanSavedYtSongs()
        rebuildPlaylists(allSongs.value)
        rescanLocalMusic()
    }

    fun importBackup(jsonString: String): Boolean {
        val success = prefs.importBackupJson(jsonString)
        if (success) {
            reloadAllPreferences()
            statusMessage.value = when (appLanguage.value) {
                AppLanguage.EN -> "Settings successfully restored!"
                AppLanguage.UK -> "Налаштування успішно відновлено!"
                else -> "Настройки успешно восстановлены!"
            }
        } else {
            statusMessage.value = when (appLanguage.value) {
                AppLanguage.EN -> "Invalid JSON backup format"
                AppLanguage.UK -> "Неправильний формат бекапу JSON"
                else -> "Неверный формат бэкапа JSON"
            }
        }
        return success
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
        } else if ((id.startsWith("sc_") || isRemoteSc) && !isLocal) {
            "sc://${id.removePrefix("sc_")}.m3u8"
        } else {
            uri
        }
        val mediaItemBuilder = MediaItem.Builder()
            .setMediaId(id)
            .setUri(effectiveUri)
            .setMediaMetadata(metadataBuilder.build())

        if (id.startsWith("sc_") || isRemoteSc || uri.startsWith("sc://") || effectiveUri.contains(".m3u8")) {
            mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
        }

        return mediaItemBuilder.build()
    }

    fun setUseLiquidGlass(enabled: Boolean) {
        useLiquidGlass.value = enabled
        prefs.saveUseLiquidGlass(enabled)
    }

    fun setUseMyWaveCoverGradient(enabled: Boolean) {
        useMyWaveCoverGradient.value = enabled
        prefs.saveUseMyWaveCoverGradient(enabled)
    }

    fun dismissClassicRemovedNotice() {
        showClassicRemovedNotice.value = false
        prefs.setSeenClassicRemovedNotice(true)
        prefs.setClassicLibraryDesign(false)
    }

    fun setClassicLibraryDesign(enabled: Boolean) {
        classicLibraryDesign.value = false
        prefs.setClassicLibraryDesign(false)
    }

    private fun isSingleTrack(song: Song): Boolean {
        val alb = song.album.trim()
        val title = song.title.trim()
        val art = song.artist.trim()
        return alb.isBlank() ||
            alb.equals("Unknown", ignoreCase = true) ||
            alb.equals("YouTube Music", ignoreCase = true) ||
            alb.equals(title, ignoreCase = true) ||
            alb.equals(art, ignoreCase = true) ||
            alb.contains("Single", ignoreCase = true) ||
            alb.contains(" - Single", ignoreCase = true) ||
            alb.contains(" - EP", ignoreCase = true) ||
            alb.contains("(EP)", ignoreCase = true) ||
            alb.equals("EP", ignoreCase = true) ||
            title.contains(" - Single", ignoreCase = true) ||
            title.contains("(Single)", ignoreCase = true)
    }

    fun openArtistProfile(artist: String) {
        val trimmed = artist.trim()
        if (trimmed.isNotBlank()) {
            selectedArtistProfile.value = trimmed
            val localSongs = (allSongs.value + savedYtSongs.value).filter { it.artist.contains(trimmed, ignoreCase = true) }
            val localGrouped = localSongs
                .filter { !isSingleTrack(it) }
                .groupBy { it.album }
                .filter { it.value.size >= 2 || (it.value.size == 1 && !it.key.contains("Single", ignoreCase = true)) }
            val albumSongIds = localGrouped.values.flatten().map { it.id }.toSet()
            val localSingles = localSongs.filter { !albumSongIds.contains(it.id) || isSingleTrack(it) }

            currentArtistProfileData.value = com.lormusic.app.data.ArtistProfileData(
                artistName = trimmed,
                avatarUrl = null,
                topTracks = localSongs.take(20),
                albums = localGrouped,
                singles = localSingles
            )

            isArtistProfileLoading.value = true
            viewModelScope.launch {
                val data = com.lormusic.app.data.YouTubeMusicRepository.fetchArtistProfile(trimmed, cookie = ytMusicCookie.value)
                if (selectedArtistProfile.value == trimmed) {
                    val mergedTopTracks = (data.topTracks + localSongs).distinctBy { it.id }.take(20)
                    val mergedAlbums = mutableMapOf<String, List<Song>>()
                    data.albums.forEach { (name, songs) -> mergedAlbums[name] = songs }
                    localGrouped.forEach { (name, songs) ->
                        val existing = mergedAlbums[name] ?: emptyList()
                        mergedAlbums[name] = (existing + songs).distinctBy { it.id }
                    }
                    val mergedSingles = (data.singles + localSingles).distinctBy { it.title.lowercase() }
                    currentArtistProfileData.value = data.copy(
                        topTracks = if (mergedTopTracks.isNotEmpty()) mergedTopTracks else localSongs.take(20),
                        albums = if (mergedAlbums.isNotEmpty()) mergedAlbums else localGrouped,
                        singles = if (mergedSingles.isNotEmpty()) mergedSingles else localSingles
                    )
                    isArtistProfileLoading.value = false
                }
            }
        }
    }

    fun closeArtistProfile() {
        selectedArtistProfile.value = null
        currentArtistProfileData.value = null
        isArtistProfileLoading.value = false
    }

    fun getArtistSongs(artist: String): List<Song> {
        val profileData = currentArtistProfileData.value
        if (profileData != null && profileData.artistName.equals(artist.trim(), ignoreCase = true)) {
            val fromProfile = profileData.topTracks + profileData.albums.values.flatten() + profileData.singles
            if (fromProfile.isNotEmpty()) return fromProfile.distinctBy { it.id }
        }
        val clean = artist.trim()
        val all = allSongs.value + savedYtSongs.value
        return all.filter { it.artist.contains(clean, ignoreCase = true) }
    }

    fun getArtistTopTracks(artist: String): List<Song> {
        val profileData = currentArtistProfileData.value
        if (profileData != null && profileData.artistName.equals(artist.trim(), ignoreCase = true) && profileData.topTracks.isNotEmpty()) {
            return profileData.topTracks
        }
        return getArtistSongs(artist).sortedByDescending { song ->
            val isFav = if (song.id.startsWith("yt_")) ytFavoriteIds.value.contains(song.id) else localFavoriteIds.value.contains(song.id)
            if (isFav) 100 else 0
        }.take(20)
    }

    fun getArtistAlbums(artist: String): Map<String, List<Song>> {
        val profileData = currentArtistProfileData.value
        if (profileData != null && profileData.artistName.equals(artist.trim(), ignoreCase = true) && profileData.albums.isNotEmpty()) {
            return profileData.albums
        }
        return getArtistSongs(artist)
            .filter { !isSingleTrack(it) }
            .groupBy { it.album }
            .filter { it.value.size >= 2 || (it.value.size == 1 && !it.key.contains("Single", ignoreCase = true)) }
    }

    fun getArtistSingles(artist: String): List<Song> {
        val profileData = currentArtistProfileData.value
        if (profileData != null && profileData.artistName.equals(artist.trim(), ignoreCase = true) && profileData.singles.isNotEmpty()) {
            return profileData.singles
        }
        val albumSongIds = getArtistAlbums(artist).values.flatten().map { it.id }.toSet()
        return getArtistSongs(artist).filter { !albumSongIds.contains(it.id) || isSingleTrack(it) }
    }

    fun setGoogleAccount(displayName: String, email: String, avatarUrl: String? = null) {
        val profile = GoogleAccountProfile(displayName = displayName, email = email, avatarUrl = avatarUrl)
        googleAccount.value = profile
        prefs.saveGoogleAccount(displayName, email, avatarUrl)
    }

    fun setEnableBetaUpdates(enabled: Boolean) {
        enableBetaUpdates.value = enabled
        prefs.saveEnableBetaUpdates(enabled)
        checkForUpdates(silent = true)
    }

    fun setBetaSeparateHeaderBubbles(enabled: Boolean) {
        betaSeparateHeaderBubbles.value = enabled
        prefs.saveBetaSeparateHeaderBubbles(enabled)
    }

    fun setBetaDynamicPlaybackContext(enabled: Boolean) {
        betaDynamicPlaybackContext.value = enabled
        prefs.saveBetaDynamicPlaybackContext(enabled)
    }

    fun setPlaybackContext(context: PlaybackContext?) {
        currentPlaybackContext.value = context
    }

    fun setUseAlbumArtGradient(enabled: Boolean) {
        useAlbumArtGradient.value = enabled
        prefs.saveUseAlbumArtGradient(enabled)
    }

    fun setUseDynamicPlayerColors(enabled: Boolean) {
        useDynamicPlayerColors.value = enabled
        prefs.saveUseDynamicPlayerColors(enabled)
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
            val result = updateManager.checkForUpdates(BuildConfig.VERSION_NAME, includeBeta = enableBetaUpdates.value)
            updateState.value = result
            if (result is UpdateState.Available) {
                availableUpdateInfo.value = result.info
                showUpdateDialog.value = true
            } else if (!silent) {
                if (result is UpdateState.UpToDate) {
                    val currentStrings = resolveStrings(appLanguage.value)
                    statusMessage.value = currentStrings.latestVersionInstalled
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
                updateState.value = UpdateState.Error(
                    when (appLanguage.value) {
                        AppLanguage.EN -> "Failed to download update"
                        AppLanguage.UK -> "Не вдалося завантажити оновлення"
                        else -> "Не удалось скачать обновление"
                    }
                )
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
