package com.lormusic.app.data

import android.content.Context
import com.lormusic.app.model.*
import org.json.JSONArray
import org.json.JSONObject

data class StoredPlaylistData(
    val id: String,
    val name: String,
    val songIds: List<String>,
    val coverColorSeed: Int,
    val isUnified: Boolean = false
)

class PreferencesRepository(context: Context) {

    private val prefs = context.getSharedPreferences("lormusic_prefs", Context.MODE_PRIVATE)

    private val backupSchema = mapOf(
        KEY_LOCAL_FAVORITES to "StringSet",
        KEY_FAVORITES to "StringSet",
        KEY_YT_FAVORITES to "StringSet",
        KEY_HIDDEN_SONGS to "StringSet",
        KEY_ENABLE_BLUR to "Boolean",
        KEY_SEARCH_PLACEMENT to "String",
        KEY_ENABLE_PLAYER_SWIPE to "Boolean",
        KEY_HIDE_DEMO_TRACKS to "Boolean",
        KEY_THEME_MODE to "String",
        KEY_APP_ICON to "String",
        KEY_DYNAMIC_COLOR to "Boolean",
        KEY_SQUIGGLY_SLIDER to "Boolean",
        KEY_SORT_OPTION to "String",
        KEY_LIBRARY_FILTER to "String",
        KEY_PLAYBACK_SPEED to "Float",
        KEY_LAST_SONG_ID to "String",
        KEY_RECENTLY_PLAYED to "String",
        KEY_GOOGLE_NAME to "String",
        KEY_GOOGLE_EMAIL to "String",
        KEY_GOOGLE_AVATAR to "String",
        KEY_CLASSIC_LIBRARY to "Boolean",
        KEY_CLASSIC_REMOVED_NOTICE_SEEN to "Boolean",
        KEY_TAG_OVERRIDES to "String",
        KEY_SAVED_YT_SONGS to "String",
        KEY_SC_FAVORITES to "StringSet",
        KEY_UNIFIED_FAVORITES to "StringSet",
        KEY_SAVED_SC_SONGS to "String",
        KEY_SC_USER_PROFILE to "String",
        KEY_CUSTOM_PLAYLISTS to "String",
        KEY_LIQUID_GLASS to "Boolean",
        KEY_BETA_UPDATES to "Boolean",
        KEY_BETA_SEPARATE_HEADER_BUBBLES to "Boolean",
        KEY_BETA_DYNAMIC_PLAYBACK_CONTEXT to "Boolean",
        KEY_BETA_AUTO_HIDE_NAVBAR to "Boolean",
        KEY_BETA_CATEGORIZED_SETTINGS to "Boolean",
        KEY_BETA_HIDE_DIVIDERS to "Boolean",
        KEY_ONBOARDING_COMPLETED to "Boolean",
        KEY_EQUALIZER_ENABLED to "Boolean",
        KEY_EQUALIZER_PRESET to "Int",
        KEY_EQUALIZER_BANDS to "String",
        KEY_EQUALIZER_BASS to "Int",
        KEY_EQUALIZER_VIRTUALIZER to "Int",
        KEY_ALBUM_ART_GRADIENT to "Boolean",
        KEY_MY_WAVE_COVER_GRADIENT to "Boolean",
        KEY_AUTO_TAGFIX to "Boolean",
        KEY_RECENTS_VIEW_MODE to "String",
        KEY_APP_LANGUAGE to "String",
        KEY_DYNAMIC_PLAYER_COLORS to "Boolean",
        KEY_PURE_MATERIAL_YOU to "Boolean",
        KEY_SHOW_NAVBAR_LABELS to "Boolean",
        KEY_UNFAVORITED_SONGS to "StringSet",
        KEY_HAS_SEEN_BACKUP_NOTICE_1631 to "Boolean",
        KEY_SONG_PLAY_COUNTS to "String",
        KEY_SONG_META_MAP to "String",
        KEY_ALBUM_PLAY_COUNTS to "String",
        KEY_TOTAL_LISTENING_MS to "Long",
        KEY_BETA_EXPANDING_SEARCH to "Boolean",
    )

    init {
        // Old imports inferred primitive storage types from JSON numeric magnitude.
        // Repair each incompatible field independently; never clear the preference file.
        val editor = prefs.edit()
        var changed = false
        prefs.all.forEach { (key, value) ->
            val type = backupSchema[key] ?: return@forEach
            when (type) {
                "Long" -> {
                    val number = when (value) {
                        is Long -> value
                        is Int -> value.toLong()
                        is String -> value.toLongOrNull()
                        else -> null
                    }?.coerceAtLeast(0L)
                    if (value !is Long || value != number) {
                        editor.putLong(key, number ?: 0L); changed = true
                    }
                }
                "Int" -> if (value !is Int) {
                    if (value is Long && value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) editor.putInt(key, value.toInt())
                    else editor.remove(key)
                    changed = true
                }
                "Float" -> {
                    val number = (value as? Number)?.toFloat()?.takeIf { it.isFinite() }?.coerceIn(0.1f, 5f)
                    if (value !is Float || value != number) {
                        if (number != null) editor.putFloat(key, number) else editor.remove(key)
                        changed = true
                    }
                }
                else -> {
                    val valid = when (type) {
                        "Boolean" -> value is Boolean
                        "String" -> value is String
                        "StringSet" -> value is Set<*> && value.all { it is String }
                        else -> true
                    }
                    if (!valid) { editor.remove(key); changed = true }
                }
            }
        }
        if (changed) editor.commit()
    }

    fun getBetaExpandingSearch(): Boolean = prefs.getBoolean(KEY_BETA_EXPANDING_SEARCH, false)
    fun saveBetaExpandingSearch(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BETA_EXPANDING_SEARCH, enabled).apply()
    }

    fun getLocalFavoriteIds(): Set<String> {
        val stored = prefs.getStringSet(KEY_LOCAL_FAVORITES, null)
        if (stored != null) return stored.toSet()
        val legacy = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        val local = legacy.filterNot { it.startsWith("yt_") }.toSet()
        saveLocalFavoriteIds(local)
        return local
    }

    fun saveLocalFavoriteIds(ids: Set<String>) {
        val filtered = ids.filterNot { it.startsWith("yt_") }.toSet()
        prefs.edit().putStringSet(KEY_LOCAL_FAVORITES, filtered).apply()
    }

    fun getYtFavoriteIds(): Set<String> {
        val stored = prefs.getStringSet(KEY_YT_FAVORITES, null)
        if (stored != null) return stored.toSet()
        val legacy = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        val yt = legacy.filter { it.startsWith("yt_") }.toSet()
        saveYtFavoriteIds(yt)
        return yt
    }

    fun saveYtFavoriteIds(ids: Set<String>) {
        val filtered = ids.filter { it.startsWith("yt_") }.toSet()
        prefs.edit().putStringSet(KEY_YT_FAVORITES, filtered).apply()
    }

    fun getFavoriteIds(): Set<String> = getLocalFavoriteIds()

    fun saveFavoriteIds(ids: Set<String>) = saveLocalFavoriteIds(ids)

    fun getHiddenSongIds(): Set<String> {
        return prefs.getStringSet(KEY_HIDDEN_SONGS, emptySet())?.toSet() ?: emptySet()
    }

    fun saveHiddenSongIds(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_HIDDEN_SONGS, ids).apply()
    }

    fun getEnableBlur(): Boolean {
        return prefs.getBoolean(KEY_ENABLE_BLUR, true)
    }

    fun saveEnableBlur(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_BLUR, enabled).apply()
    }

    fun getSearchBarPlacement(): SearchBarPlacement {
        val raw = prefs.getString(KEY_SEARCH_PLACEMENT, SearchBarPlacement.LOCAL_TOP_YT_BOTTOM.name)
            ?: SearchBarPlacement.LOCAL_TOP_YT_BOTTOM.name
        return SearchBarPlacement.entries.find { it.name == raw } ?: SearchBarPlacement.LOCAL_TOP_YT_BOTTOM
    }

    fun saveSearchBarPlacement(placement: SearchBarPlacement) {
        prefs.edit().putString(KEY_SEARCH_PLACEMENT, placement.name).apply()
    }

    fun getEnablePlayerSwipe(): Boolean {
        return prefs.getBoolean(KEY_ENABLE_PLAYER_SWIPE, true)
    }

    fun saveEnablePlayerSwipe(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_PLAYER_SWIPE, enabled).apply()
    }

    fun getHideDemoTracks(): Boolean {
        return prefs.getBoolean(KEY_HIDE_DEMO_TRACKS, false)
    }

    fun saveHideDemoTracks(hide: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_DEMO_TRACKS, hide).apply()
    }

    fun getThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return ThemeMode.entries.find { it.name == raw } ?: ThemeMode.SYSTEM
    }

    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getAppIconOption(): AppIconOption {
        val raw = prefs.getString(KEY_APP_ICON, AppIconOption.REGULAR.name) ?: AppIconOption.REGULAR.name
        return AppIconOption.entries.find { it.name == raw } ?: AppIconOption.REGULAR
    }

    fun saveAppIconOption(option: AppIconOption) {
        prefs.edit().putString(KEY_APP_ICON, option.name).apply()
    }

    fun getUseDynamicColor(): Boolean {
        return prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
    }

    fun saveUseDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
    }

    fun getUseSquigglySlider(): Boolean {
        return prefs.getBoolean(KEY_SQUIGGLY_SLIDER, true)
    }

    fun saveUseSquigglySlider(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SQUIGGLY_SLIDER, enabled).apply()
    }

    fun getSortOption(): SortOption {
        val raw = prefs.getString(KEY_SORT_OPTION, SortOption.TITLE_ASC.name) ?: SortOption.TITLE_ASC.name
        return SortOption.entries.find { it.name == raw } ?: SortOption.TITLE_ASC
    }

    fun saveSortOption(option: SortOption) {
        prefs.edit().putString(KEY_SORT_OPTION, option.name).apply()
    }

    fun getLibraryFilter(): LibraryFilter {
        val raw = prefs.getString(KEY_LIBRARY_FILTER, LibraryFilter.ALL.name) ?: LibraryFilter.ALL.name
        return LibraryFilter.entries.find { it.name == raw } ?: LibraryFilter.ALL
    }

    fun saveLibraryFilter(filter: LibraryFilter) {
        prefs.edit().putString(KEY_LIBRARY_FILTER, filter.name).apply()
    }

    fun getPlaybackSpeed(): Float {
        return prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f)
    }

    fun savePlaybackSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_PLAYBACK_SPEED, speed).apply()
    }

    fun getLastPlayedSongId(): String? {
        return prefs.getString(KEY_LAST_SONG_ID, null)
    }

    fun saveLastPlayedSongId(songId: String) {
        prefs.edit().putString(KEY_LAST_SONG_ID, songId).apply()
    }

    fun getRecentlyPlayedIds(): List<String> {
        val raw = prefs.getString(KEY_RECENTLY_PLAYED, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val id = arr.optString(i)
                    if (id.isNotBlank()) add(id)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addRecentlyPlayedId(songId: String) {
        val current = getRecentlyPlayedIds().toMutableList()
        current.remove(songId)
        current.add(0, songId)
        val trimmed = current.take(40)
        val arr = JSONArray()
        trimmed.forEach { arr.put(it) }
        prefs.edit().putString(KEY_RECENTLY_PLAYED, arr.toString()).apply()
    }

    fun getGoogleAccountName(): String? = prefs.getString(KEY_GOOGLE_NAME, null)
    fun getGoogleAccountEmail(): String? = prefs.getString(KEY_GOOGLE_EMAIL, null)
    fun getGoogleAccountAvatar(): String? = prefs.getString(KEY_GOOGLE_AVATAR, null)
    fun getYtMusicCookie(): String? = prefs.getString(KEY_YT_MUSIC_COOKIE, null)

    fun saveGoogleAccount(name: String?, email: String?, avatarUrl: String? = null) {
        prefs.edit()
            .putString(KEY_GOOGLE_NAME, name)
            .putString(KEY_GOOGLE_EMAIL, email)
            .putString(KEY_GOOGLE_AVATAR, avatarUrl)
            .apply()
    }

    fun getClassicLibraryDesign(): Boolean = prefs.getBoolean(KEY_CLASSIC_LIBRARY, false)

    fun setClassicLibraryDesign(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_CLASSIC_LIBRARY, enabled)
            .apply()
    }

    fun hasSeenClassicRemovedNotice(): Boolean = prefs.getBoolean(KEY_CLASSIC_REMOVED_NOTICE_SEEN, false)

    fun setSeenClassicRemovedNotice(seen: Boolean = true) {
        prefs.edit().putBoolean(KEY_CLASSIC_REMOVED_NOTICE_SEEN, seen).apply()
    }

    fun saveYtMusicCookie(cookie: String?) {
        prefs.edit()
            .putString(KEY_YT_MUSIC_COOKIE, cookie)
            .apply()
    }

    fun getTagOverrides(): Map<String, SongTagOverride> {
        val json = prefs.getString(KEY_TAG_OVERRIDES, null) ?: return emptyMap()
        return try {
            val arr = JSONArray(json)
            val map = mutableMapOf<String, SongTagOverride>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val songId = obj.getString("songId")
                map[songId] = SongTagOverride(
                    songId = songId,
                    title = obj.optString("title", ""),
                    artist = obj.optString("artist", ""),
                    album = obj.optString("album", ""),
                    genre = obj.optString("genre", ""),
                    year = obj.optString("year", ""),
                    trackNumber = obj.optString("trackNumber", ""),
                    albumArtUri = obj.optString("albumArtUri", "").takeIf { it.isNotBlank() },
                    customLyrics = obj.optString("customLyrics", "").takeIf { it.isNotBlank() }
                )
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveTagOverrides(overrides: Map<String, SongTagOverride>) {
        val arr = JSONArray()
        overrides.values.forEach { ov ->
            val obj = JSONObject()
            obj.put("songId", ov.songId)
            obj.put("title", ov.title)
            obj.put("artist", ov.artist)
            obj.put("album", ov.album)
            obj.put("genre", ov.genre)
            obj.put("year", ov.year)
            obj.put("trackNumber", ov.trackNumber)
            obj.put("albumArtUri", ov.albumArtUri ?: "")
            obj.put("customLyrics", ov.customLyrics ?: "")
            arr.put(obj)
        }
        prefs.edit().putString(KEY_TAG_OVERRIDES, arr.toString()).apply()
    }

    fun getSavedYtSongs(): List<Song> {
        val json = prefs.getString(KEY_SAVED_YT_SONGS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        Song(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            artist = obj.getString("artist"),
                            album = obj.optString("album", "YouTube Music"),
                            durationMs = obj.optLong("durationMs", 180_000L),
                            uri = obj.getString("uri"),
                            coverColorSeed = obj.optInt("coverColorSeed", i),
                            albumArtUri = obj.optString("albumArtUri", "").takeIf { it.isNotBlank() },
                            isLocal = false,
                            folderName = "YouTube Music",
                            dateAddedSec = obj.optLong("dateAddedSec", System.currentTimeMillis() / 1000L),
                            genre = obj.optString("genre", ""),
                            year = obj.optString("year", ""),
                            isRemoteYt = true
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveYtSongs(songs: List<Song>) {
        val arr = JSONArray()
        songs.take(120).forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("title", s.title)
            obj.put("artist", s.artist)
            obj.put("album", s.album)
            obj.put("durationMs", s.durationMs)
            obj.put("uri", s.uri)
            obj.put("coverColorSeed", s.coverColorSeed)
            obj.put("albumArtUri", s.albumArtUri ?: "")
            obj.put("dateAddedSec", s.dateAddedSec)
            obj.put("genre", s.genre)
            obj.put("year", s.year)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_YT_SONGS, arr.toString()).apply()
    }

    fun getScFavoriteIds(): Set<String> {
        return prefs.getStringSet(KEY_SC_FAVORITES, emptySet())?.toSet() ?: emptySet()
    }

    fun saveScFavoriteIds(ids: Set<String>) {
        val filtered = ids.filter { it.startsWith("sc_") }.toSet()
        prefs.edit().putStringSet(KEY_SC_FAVORITES, filtered).apply()
    }

    fun getUnifiedFavoriteIds(): Set<String> {
        return prefs.getStringSet(KEY_UNIFIED_FAVORITES, emptySet())?.toSet() ?: emptySet()
    }

    fun saveUnifiedFavoriteIds(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_UNIFIED_FAVORITES, ids).apply()
    }

    fun getSavedScSongs(): List<Song> {
        val json = prefs.getString(KEY_SAVED_SC_SONGS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        Song(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            artist = obj.getString("artist"),
                            album = obj.optString("album", "SoundCloud"),
                            durationMs = obj.optLong("durationMs", 180_000L),
                            uri = obj.getString("uri"),
                            coverColorSeed = obj.optInt("coverColorSeed", i),
                            albumArtUri = obj.optString("albumArtUri", "").takeIf { it.isNotBlank() },
                            isLocal = false,
                            folderName = "SoundCloud",
                            dateAddedSec = obj.optLong("dateAddedSec", System.currentTimeMillis() / 1000L),
                            genre = obj.optString("genre", ""),
                            year = obj.optString("year", ""),
                            isRemoteSc = true
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveScSongs(songs: List<Song>) {
        val arr = JSONArray()
        songs.take(120).forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("title", s.title)
            obj.put("artist", s.artist)
            obj.put("album", s.album)
            obj.put("durationMs", s.durationMs)
            obj.put("uri", s.uri)
            obj.put("coverColorSeed", s.coverColorSeed)
            obj.put("albumArtUri", s.albumArtUri ?: "")
            obj.put("dateAddedSec", s.dateAddedSec)
            obj.put("genre", s.genre)
            obj.put("year", s.year)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_SC_SONGS, arr.toString()).apply()
    }

    fun getScProfile(): SoundCloudUserProfile? {
        val json = prefs.getString(KEY_SC_USER_PROFILE, null) ?: return null
        return try {
            val obj = JSONObject(json)
            SoundCloudUserProfile(
                username = obj.getString("username"),
                avatarUrl = obj.optString("avatarUrl", "").takeIf { it.isNotBlank() },
                permalinkUrl = obj.optString("permalinkUrl", "").takeIf { it.isNotBlank() },
                trackCount = obj.optInt("trackCount", 0),
                likesCount = obj.optInt("likesCount", 0),
                followersCount = obj.optInt("followersCount", 0),
                followingsCount = obj.optInt("followingsCount", 0)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveScProfile(profile: SoundCloudUserProfile?) {
        if (profile == null) {
            prefs.edit().remove(KEY_SC_USER_PROFILE).apply()
        } else {
            val obj = JSONObject()
            obj.put("username", profile.username)
            obj.put("avatarUrl", profile.avatarUrl ?: "")
            obj.put("permalinkUrl", profile.permalinkUrl ?: "")
            obj.put("trackCount", profile.trackCount)
            obj.put("likesCount", profile.likesCount)
            obj.put("followersCount", profile.followersCount)
            obj.put("followingsCount", profile.followingsCount)
            prefs.edit().putString(KEY_SC_USER_PROFILE, obj.toString()).apply()
        }
    }

    fun getScAuthToken(): String? {
        return prefs.getString(KEY_SC_AUTH_TOKEN, null)
    }

    fun saveScAuthToken(token: String?) {
        if (token.isNullOrBlank()) {
            prefs.edit().remove(KEY_SC_AUTH_TOKEN).apply()
        } else {
            prefs.edit().putString(KEY_SC_AUTH_TOKEN, token).apply()
        }
    }

    fun getCustomPlaylists(): List<StoredPlaylistData>? {
        val json = prefs.getString(KEY_CUSTOM_PLAYLISTS, null) ?: return null
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<StoredPlaylistData>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val songIdsArr = obj.optJSONArray("songIds") ?: JSONArray()
                val songIds = mutableListOf<String>()
                for (j in 0 until songIdsArr.length()) {
                    songIds.add(songIdsArr.getString(j))
                }
                list.add(
                    StoredPlaylistData(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        songIds = songIds,
                        coverColorSeed = obj.optInt("coverColorSeed", i + 10),
                        isUnified = obj.optBoolean("isUnified", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            null
        }
    }

    fun saveCustomPlaylists(playlists: List<StoredPlaylistData>) {
        val arr = JSONArray()
        playlists.forEach { pl ->
            val obj = JSONObject()
            obj.put("id", pl.id)
            obj.put("name", pl.name)
            obj.put("coverColorSeed", pl.coverColorSeed)
            obj.put("isUnified", pl.isUnified)
            val ids = JSONArray()
            pl.songIds.forEach { ids.put(it) }
            obj.put("songIds", ids)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_PLAYLISTS, arr.toString()).apply()
    }

    fun getUseLiquidGlass(): Boolean {
        return prefs.getBoolean(KEY_LIQUID_GLASS, false)
    }

    fun saveUseLiquidGlass(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LIQUID_GLASS, enabled).apply()
    }

    fun getEnableBetaUpdates(): Boolean {
        return prefs.getBoolean(KEY_BETA_UPDATES, false)
    }

    fun saveEnableBetaUpdates(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BETA_UPDATES, enabled).apply()
    }

    fun getBetaSeparateHeaderBubbles(): Boolean {
        return prefs.getBoolean(KEY_BETA_SEPARATE_HEADER_BUBBLES, false)
    }

    fun saveBetaSeparateHeaderBubbles(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BETA_SEPARATE_HEADER_BUBBLES, enabled).apply()
    }

    fun getBetaDynamicPlaybackContext(): Boolean {
        return prefs.getBoolean(KEY_BETA_DYNAMIC_PLAYBACK_CONTEXT, true)
    }

    fun saveBetaDynamicPlaybackContext(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BETA_DYNAMIC_PLAYBACK_CONTEXT, enabled).apply()
    }

    fun getBetaAutoHideNavBar(): Boolean {
        return prefs.getBoolean(KEY_BETA_AUTO_HIDE_NAVBAR, false)
    }

    fun saveBetaAutoHideNavBar(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BETA_AUTO_HIDE_NAVBAR, enabled).apply()
    }

    fun getBetaCategorizedSettings(): Boolean {
        return prefs.getBoolean(KEY_BETA_CATEGORIZED_SETTINGS, true)
    }

    fun saveBetaCategorizedSettings(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BETA_CATEGORIZED_SETTINGS, enabled).apply()
    }

    fun getBetaHideDividers(): Boolean {
        return prefs.getBoolean(KEY_BETA_HIDE_DIVIDERS, false)
    }

    fun saveBetaHideDividers(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BETA_HIDE_DIVIDERS, enabled).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun getEqualizerEnabled(): Boolean {
        return prefs.getBoolean(KEY_EQUALIZER_ENABLED, false)
    }

    fun saveEqualizerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EQUALIZER_ENABLED, enabled).apply()
    }

    fun getEqualizerPreset(): Int {
        return prefs.getInt(KEY_EQUALIZER_PRESET, 0)
    }

    fun saveEqualizerPreset(preset: Int) {
        prefs.edit().putInt(KEY_EQUALIZER_PRESET, preset).apply()
    }

    fun getEqualizerBands(): List<Int> {
        val raw = prefs.getString(KEY_EQUALIZER_BANDS, null) ?: return listOf(0, 0, 0, 0, 0)
        return try {
            raw.split(",").mapNotNull { it.trim().toIntOrNull() }.ifEmpty { listOf(0, 0, 0, 0, 0) }
        } catch (_: Exception) {
            listOf(0, 0, 0, 0, 0)
        }
    }

    fun saveEqualizerBands(bands: List<Int>) {
        prefs.edit().putString(KEY_EQUALIZER_BANDS, bands.joinToString(",")).apply()
    }

    fun getEqualizerBass(): Int {
        return prefs.getInt(KEY_EQUALIZER_BASS, 0)
    }

    fun saveEqualizerBass(bass: Int) {
        prefs.edit().putInt(KEY_EQUALIZER_BASS, bass.coerceIn(0, 1000)).apply()
    }

    fun getEqualizerVirtualizer(): Int {
        return prefs.getInt(KEY_EQUALIZER_VIRTUALIZER, 0)
    }

    fun saveEqualizerVirtualizer(virtualizer: Int) {
        prefs.edit().putInt(KEY_EQUALIZER_VIRTUALIZER, virtualizer.coerceIn(0, 1000)).apply()
    }

    fun getUseAlbumArtGradient(): Boolean {
        return prefs.getBoolean(KEY_ALBUM_ART_GRADIENT, true)
    }

    fun saveUseAlbumArtGradient(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALBUM_ART_GRADIENT, enabled).apply()
    }

    fun getUseMyWaveCoverGradient(): Boolean {
        return prefs.getBoolean(KEY_MY_WAVE_COVER_GRADIENT, true)
    }

    fun saveUseMyWaveCoverGradient(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MY_WAVE_COVER_GRADIENT, enabled).apply()
    }

    fun getAutoTagFixEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_TAGFIX, false)
    }

    fun getAutoTagFix(): Boolean = getAutoTagFixEnabled()

    fun saveAutoTagFixEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_TAGFIX, enabled).apply()
    }

    fun saveAutoTagFix(enabled: Boolean) = saveAutoTagFixEnabled(enabled)

    fun getRecentsViewMode(): String {
        return prefs.getString(KEY_RECENTS_VIEW_MODE, "grid") ?: "grid"
    }

    fun saveRecentsViewMode(mode: String) {
        prefs.edit().putString(KEY_RECENTS_VIEW_MODE, mode).apply()
    }

    fun getAppLanguage(): com.lormusic.app.model.AppLanguage {
        val code = prefs.getString(KEY_APP_LANGUAGE, null)
        return com.lormusic.app.model.AppLanguage.fromCode(code)
    }

    fun saveAppLanguage(lang: com.lormusic.app.model.AppLanguage) {
        prefs.edit().putString(KEY_APP_LANGUAGE, lang.code).apply()
    }

    fun getUseDynamicPlayerColors(): Boolean = prefs.getBoolean(KEY_DYNAMIC_PLAYER_COLORS, true)

    fun saveUseDynamicPlayerColors(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_PLAYER_COLORS, enabled).apply()
    }

    fun getPureMaterialYou(): Boolean = prefs.getBoolean(KEY_PURE_MATERIAL_YOU, false)

    fun savePureMaterialYou(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PURE_MATERIAL_YOU, enabled).apply()
    }

    fun getShowNavBarLabels(): Boolean = prefs.getBoolean(KEY_SHOW_NAVBAR_LABELS, false)

    fun saveShowNavBarLabels(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_NAVBAR_LABELS, show).apply()
    }

    fun getUnfavoritedSongIds(): Set<String> {
        return prefs.getStringSet(KEY_UNFAVORITED_SONGS, emptySet()) ?: emptySet()
    }

    fun addUnfavoritedSongId(id: String) {
        val current = getUnfavoritedSongIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet(KEY_UNFAVORITED_SONGS, current).apply()
    }

    fun removeUnfavoritedSongId(id: String) {
        val current = getUnfavoritedSongIds().toMutableSet()
        if (current.remove(id)) {
            prefs.edit().putStringSet(KEY_UNFAVORITED_SONGS, current).apply()
        }
    }

    fun hasSeenBackupNotice(): Boolean {
        return prefs.getBoolean(KEY_HAS_SEEN_BACKUP_NOTICE_1631, false)
    }

    fun setHasSeenBackupNotice(seen: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_SEEN_BACKUP_NOTICE_1631, seen).apply()
    }

    fun recordSongPlay(songId: String, title: String, artist: String, album: String) {
        try {
            val countsJson = prefs.getString(KEY_SONG_PLAY_COUNTS, "{}") ?: "{}"
            val countsObj = JSONObject(countsJson)
            val currentCount = countsObj.optInt(songId, 0)
            countsObj.put(songId, currentCount + 1)

            val titlesJson = prefs.getString(KEY_SONG_META_MAP, "{}") ?: "{}"
            val titlesObj = JSONObject(titlesJson)
            titlesObj.put(songId, "$title///$artist")

            val albumsJson = prefs.getString(KEY_ALBUM_PLAY_COUNTS, "{}") ?: "{}"
            val albumsObj = JSONObject(albumsJson)
            val cleanAlbum = album.trim().ifBlank { "Unknown" }
            if (cleanAlbum != "Unknown" && cleanAlbum != "YouTube Music") {
                val albumCount = albumsObj.optInt(cleanAlbum, 0)
                albumsObj.put(cleanAlbum, albumCount + 1)
            }

            prefs.edit()
                .putString(KEY_SONG_PLAY_COUNTS, countsObj.toString())
                .putString(KEY_SONG_META_MAP, titlesObj.toString())
                .putString(KEY_ALBUM_PLAY_COUNTS, albumsObj.toString())
                .apply()
        } catch (_: Exception) {}
    }

    fun addListeningTimeMs(ms: Long) {
        if (ms <= 0) return
        val current = getTotalListeningTimeMs()
        prefs.edit().putLong(KEY_TOTAL_LISTENING_MS, current + ms.coerceAtMost(Long.MAX_VALUE - current)).apply()
    }

    fun getTotalListeningTimeMs(): Long {
        val stored = prefs.all[KEY_TOTAL_LISTENING_MS] ?: return 0L
        val value = when (stored) {
            is Long -> stored
            is Int -> stored.toLong()
            is String -> stored.toLongOrNull() ?: 0L
            else -> 0L
        }.coerceAtLeast(0L)
        if (stored !is Long || stored != value) prefs.edit().putLong(KEY_TOTAL_LISTENING_MS, value).commit()
        return value
    }

    fun getTopPlayedSong(): Triple<String, String, Int>? {
        return try {
            val countsJson = prefs.getString(KEY_SONG_PLAY_COUNTS, "{}") ?: "{}"
            val countsObj = JSONObject(countsJson)
            val titlesJson = prefs.getString(KEY_SONG_META_MAP, "{}") ?: "{}"
            val titlesObj = JSONObject(titlesJson)

            var maxId: String? = null
            var maxCount = 0
            val keys = countsObj.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val count = countsObj.optInt(id, 0)
                if (count > maxCount) {
                    maxCount = count
                    maxId = id
                }
            }
            if (maxId != null && maxCount > 0) {
                val meta = titlesObj.optString(maxId, "")
                val parts = meta.split("///")
                val title = parts.getOrNull(0) ?: maxId
                val artist = parts.getOrNull(1) ?: ""
                Triple(title, artist, maxCount)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getTopAlbum(): Pair<String, Int>? {
        return try {
            val albumsJson = prefs.getString(KEY_ALBUM_PLAY_COUNTS, "{}") ?: "{}"
            val albumsObj = JSONObject(albumsJson)
            var maxAlbum: String? = null
            var maxCount = 0
            val keys = albumsObj.keys()
            while (keys.hasNext()) {
                val alb = keys.next()
                val count = albumsObj.optInt(alb, 0)
                if (count > maxCount) {
                    maxCount = count
                    maxAlbum = alb
                }
            }
            if (maxAlbum != null && maxCount > 0) {
                Pair(maxAlbum, maxCount)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun exportBackupJson(): String {
        getTotalListeningTimeMs() // Repair legacy numeric storage before exporting.
        val values = prefs.all.filterKeys { it in backupSchema }
        val root = JSONObject().put("app", "LorMusic")
            .put("formatVersion", 2)
            .put("appVersion", com.lormusic.app.BuildConfig.VERSION_NAME)
            .put("versionCode", com.lormusic.app.BuildConfig.VERSION_CODE)
            .put("createdAt", java.time.OffsetDateTime.now().toString())
        val data = JSONObject()
        values.forEach { (key, value) ->
            data.put(key, if (value is Set<*>) JSONArray(value.toList()) else value)
        }
        root.put("settings", data)
        val json = root.toString(2)
        validateBackup(json)
        return json
    }

    /** Validates the entire document without changing any preferences. */
    fun validateBackup(json: String): String {
        val root = JSONObject(json)
        prepareBackup(root)
        return "LorMusic " + root.optString("appVersion", root.optString("version", "legacy")) +
            "\n" + root.optString("createdAt", root.optString("timestamp", "")) +
            "\nФормат: " + root.optInt("formatVersion", 1)
    }

    private fun prepareBackup(root: JSONObject): Map<String, Any> {
        require(!root.has("app") || root.get("app") == "LorMusic") { "Это не резервная копия LorMusic" }
        val version = root.opt("formatVersion") ?: 1
        require(version is Int && version in 1..2) { "Неподдерживаемая версия формата" }
        val data = when {
            version == 2 -> {
                require(root.getString("app") == "LorMusic") { "Отсутствует название приложения" }
                root.getJSONObject("settings")
            }
            root.has("preferences") -> root.getJSONObject("preferences")
            else -> root // Compatibility with old plain preferences exports.
        }
        val result = linkedMapOf<String, Any>()
        data.keys().forEach { key ->
            val type = backupSchema[key] ?: return@forEach // Includes exclusion of credentials.
            val value = data.get(key)
            val converted: Any = when (type) {
                "Boolean" -> { require(value is Boolean) { "Неверный тип: $key" }; value }
                "String" -> { require(value is String) { "Неверный тип: $key" }; value }
                "StringSet" -> {
                    require(value is JSONArray) { "Неверный тип: $key" }
                    (0 until value.length()).map { i ->
                        val item = value.get(i)
                        require(item is String) { "Неверный элемент: $key" }
                        item
                    }.toSet()
                }
                "Int", "Long" -> {
                    require(value is Number) { "Неверное число: $key" }
                    val number = try { java.math.BigDecimal(value.toString()).longValueExact() }
                        catch (_: ArithmeticException) { throw IllegalArgumentException("Число вне диапазона: $key") }
                    if (type == "Int") {
                        require(number in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "Число вне диапазона: $key" }
                        number.toInt()
                    } else {
                        require(number >= 0) { "Отрицательное время прослушивания" }
                        number
                    }
                }
                "Float" -> {
                    require(value is Number) { "Неверное число: $key" }
                    val number = value.toFloat()
                    require(number.isFinite() && number in 0.1f..5f) { "Недопустимая скорость воспроизведения" }
                    number
                }
                else -> error("Неизвестный тип")
            }
            // Collections stored as JSON strings must also be valid before committing.
            if (converted is String) {
                when (key) {
                    KEY_TAG_OVERRIDES, KEY_SAVED_YT_SONGS, KEY_SAVED_SC_SONGS, KEY_CUSTOM_PLAYLISTS, KEY_RECENTLY_PLAYED -> JSONArray(converted)
                    KEY_SONG_PLAY_COUNTS, KEY_SONG_META_MAP, KEY_ALBUM_PLAY_COUNTS, KEY_SC_USER_PROFILE -> JSONObject(converted)
                }
            }
            result[key] = converted
        }
        require(result.isNotEmpty()) { "В файле нет поддерживаемых настроек LorMusic" }
        return result
    }

    fun importBackupJson(jsonString: String): Boolean = try {
        val values = prepareBackup(JSONObject(jsonString))
        val previous = prefs.all.filterKeys { it in values }
        fun applyValues(data: Map<String, Any?>, removed: Set<String> = emptySet()): Boolean {
        val editor = prefs.edit()
        removed.forEach { editor.remove(it) }
        data.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    editor.putStringSet(key, value as Set<String>)
                }
            }
        }
        return editor.commit()
        }
        if (applyValues(values)) true else {
            applyValues(previous, values.keys - previous.keys)
            false
        }
    } catch (_: Exception) { false }

    companion object {
        private const val KEY_BETA_EXPANDING_SEARCH = "beta_expanding_search"
        private const val KEY_FAVORITES = "favorite_song_ids"
        private const val KEY_HIDDEN_SONGS = "hidden_song_ids"
        private const val KEY_ENABLE_BLUR = "enable_backdrop_blur"
        private const val KEY_SEARCH_PLACEMENT = "search_bar_placement"
        private const val KEY_ENABLE_PLAYER_SWIPE = "enable_player_swipe_gesture"
        private const val KEY_HIDE_DEMO_TRACKS = "hide_demo_tracks"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_APP_ICON = "app_icon_option"
        private const val KEY_DYNAMIC_COLOR = "use_dynamic_color"
        private const val KEY_SQUIGGLY_SLIDER = "use_squiggly_slider"
        private const val KEY_SORT_OPTION = "library_sort_option"
        private const val KEY_LIBRARY_FILTER = "library_filter"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
        private const val KEY_LAST_SONG_ID = "last_played_song_id"
        private const val KEY_RECENTLY_PLAYED = "recently_played_song_ids"
        private const val KEY_GOOGLE_NAME = "google_account_name"
        private const val KEY_GOOGLE_EMAIL = "google_account_email"
        private const val KEY_GOOGLE_AVATAR = "google_account_avatar"
        private const val KEY_CLASSIC_LIBRARY = "classic_library_design"
        private const val KEY_CLASSIC_REMOVED_NOTICE_SEEN = "classic_removed_notice_seen"
        private const val KEY_YT_MUSIC_COOKIE = "yt_music_cookie_raw"
        private const val KEY_TAG_OVERRIDES = "song_tag_overrides_json"
        private const val KEY_SAVED_YT_SONGS = "saved_yt_music_songs_json"
        private const val KEY_CUSTOM_PLAYLISTS = "custom_playlists_json"
        private const val KEY_LIQUID_GLASS = "use_liquid_glass"
        private const val KEY_BETA_UPDATES = "enable_beta_updates"
        private const val KEY_ALBUM_ART_GRADIENT = "use_album_art_gradient_v2"
        private const val KEY_MY_WAVE_COVER_GRADIENT = "use_my_wave_cover_gradient"
        private const val KEY_AUTO_TAGFIX = "auto_tagfix_enabled"
        private const val KEY_RECENTS_VIEW_MODE = "yt_recents_view_mode"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_DYNAMIC_PLAYER_COLORS = "use_dynamic_player_colors"
        private const val KEY_PURE_MATERIAL_YOU = "pure_material_you"
        private const val KEY_UNFAVORITED_SONGS = "unfavorited_song_ids"
        private const val KEY_SHOW_NAVBAR_LABELS = "show_navbar_labels"
        private const val KEY_HAS_SEEN_BACKUP_NOTICE_1631 = "has_seen_backup_notice_1631"
        private const val KEY_LOCAL_FAVORITES = "local_favorite_song_ids"
        private const val KEY_YT_FAVORITES = "yt_favorite_song_ids"
        private const val KEY_SONG_PLAY_COUNTS = "song_play_counts_json"
        private const val KEY_SONG_META_MAP = "song_meta_map_json"
        private const val KEY_ALBUM_PLAY_COUNTS = "album_play_counts_json"
        private const val KEY_TOTAL_LISTENING_MS = "total_listening_ms"
        private const val KEY_SC_FAVORITES = "sc_favorites_song_ids"
        private const val KEY_SAVED_SC_SONGS = "saved_sc_songs_json"
        private const val KEY_SC_USER_PROFILE = "sc_user_profile_json"
        private const val KEY_SC_AUTH_TOKEN = "sc_auth_token_raw"
        private const val KEY_UNIFIED_FAVORITES = "unified_favorite_ids"
        private const val KEY_BETA_SEPARATE_HEADER_BUBBLES = "beta_separate_header_bubbles"
        private const val KEY_BETA_DYNAMIC_PLAYBACK_CONTEXT = "beta_dynamic_playback_context"
        private const val KEY_BETA_AUTO_HIDE_NAVBAR = "beta_auto_hide_navbar"
        private const val KEY_BETA_CATEGORIZED_SETTINGS = "beta_categorized_settings_v2"
        private const val KEY_BETA_HIDE_DIVIDERS = "beta_hide_dividers"
        private const val KEY_EQUALIZER_ENABLED = "equalizer_enabled"
        private const val KEY_EQUALIZER_PRESET = "equalizer_preset"
        private const val KEY_EQUALIZER_BANDS = "equalizer_bands_csv"
        private const val KEY_EQUALIZER_BASS = "equalizer_bass_boost"
        private const val KEY_EQUALIZER_VIRTUALIZER = "equalizer_virtualizer"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed_v1"
    }
}
