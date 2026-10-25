package com.lormusic.app.data

import android.content.Context
import com.lormusic.app.model.*
import org.json.JSONArray
import org.json.JSONObject

data class StoredPlaylistData(
    val id: String,
    val name: String,
    val songIds: List<String>,
    val coverColorSeed: Int
)

class PreferencesRepository(context: Context) {

    private val prefs = context.getSharedPreferences("lormusic_prefs", Context.MODE_PRIVATE)

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
                        coverColorSeed = obj.optInt("coverColorSeed", i + 10)
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

    fun getUseAlbumArtGradient(): Boolean {
        return prefs.getBoolean(KEY_ALBUM_ART_GRADIENT, false)
    }

    fun saveUseAlbumArtGradient(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALBUM_ART_GRADIENT, enabled).apply()
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
        val current = prefs.getLong(KEY_TOTAL_LISTENING_MS, 0L)
        prefs.edit().putLong(KEY_TOTAL_LISTENING_MS, current + ms).apply()
    }

    fun getTotalListeningTimeMs(): Long {
        return prefs.getLong(KEY_TOTAL_LISTENING_MS, 0L)
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
        val root = JSONObject()
        root.put("app", "LorMusic")
        root.put("version", "1.6.4")
        root.put("timestamp", System.currentTimeMillis())
        val data = JSONObject()
        prefs.all.forEach { (key, value) ->
            when (value) {
                is Boolean -> data.put(key, value)
                is Int -> data.put(key, value)
                is Long -> data.put(key, value)
                is Float -> data.put(key, value.toDouble())
                is String -> data.put(key, value)
                is Set<*> -> {
                    val arr = JSONArray()
                    value.forEach { if (it is String) arr.put(it) }
                    data.put(key, arr)
                }
            }
        }
        root.put("preferences", data)
        return root.toString(2)
    }

    fun importBackupJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val data = if (root.has("preferences")) root.getJSONObject("preferences") else root
            val editor = prefs.edit()
            val keys = data.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (data.isNull(key)) continue
                val value = data.get(key)
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> {
                        if (key == KEY_PLAYBACK_SPEED) {
                            editor.putFloat(key, value.toFloat())
                        } else {
                            editor.putLong(key, value.toLong())
                        }
                    }
                    is String -> editor.putString(key, value)
                    is JSONArray -> {
                        val set = mutableSetOf<String>()
                        for (i in 0 until value.length()) {
                            set.add(value.getString(i))
                        }
                        editor.putStringSet(key, set)
                    }
                }
            }
            editor.apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
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
        private const val KEY_YT_MUSIC_COOKIE = "yt_music_cookie_raw"
        private const val KEY_TAG_OVERRIDES = "song_tag_overrides_json"
        private const val KEY_SAVED_YT_SONGS = "saved_yt_music_songs_json"
        private const val KEY_CUSTOM_PLAYLISTS = "custom_playlists_json"
        private const val KEY_LIQUID_GLASS = "use_liquid_glass"
        private const val KEY_BETA_UPDATES = "enable_beta_updates"
        private const val KEY_ALBUM_ART_GRADIENT = "use_album_art_gradient"
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
    }
}
