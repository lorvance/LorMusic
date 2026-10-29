package com.lormusic.app.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.lormusic.app.model.GoogleAccountProfile
import com.lormusic.app.model.Song
import com.lormusic.app.model.SongTagOverride
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

data class YtAccountSyncResult(
    val profile: GoogleAccountProfile?,
    val historySongs: List<Song>,
    val likedSongs: List<Song>,
    val homeRecommendations: List<Song>
)

data class ArtistProfileData(
    val artistName: String,
    val avatarUrl: String?,
    val topTracks: List<Song>,
    val albums: Map<String, List<Song>>,
    val singles: List<Song>
)

object YouTubeMusicRepository {

    private const val YTM_ORIGIN = "https://music.youtube.com"
    private const val YT_ORIGIN = "https://www.youtube.com"
    private const val YTM_BROWSE_URL = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
    private const val YTM_SEARCH_URL = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
    private const val YTM_ACCOUNT_URL = "https://music.youtube.com/youtubei/v1/account/account_menu?prettyPrint=false"
    private const val YT_VISITOR_URL = "https://www.youtube.com/youtubei/v1/visitor_id?prettyPrint=false"
    private const val YT_PLAYER_URL = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"

    const val ANDROID_USER_AGENT = "com.google.android.youtube/20.10.35 (Linux; U; Android 14) gzip"
    private const val ANDROID_CLIENT_VERSION = "20.10.35"

    private data class CachedAudioStream(
        val url: String,
        val durationMs: Long,
        val createdAtMs: Long = System.currentTimeMillis()
    )

    private val streamCache = ConcurrentHashMap<String, CachedAudioStream>()

    @Volatile
    private var cachedVisitorData: String? = null

    @Volatile
    private var visitorDataFetchedAtMs: Long = 0L

    @Volatile
    var currentAuthCookie: String? = null

    enum class WaveMood(val label: String, val queryHint: String) {
        FOR_YOU("Под твой вкус", "инди рок пост панк альтернатива"),
        ENERGETIC("Бодрое", "драйв рок электроника фонк"),
        CHILL("Спокойное", "лоу фай атмосфера инди чилл"),
        DISCOVERY("Незнакомое", "новые инди хиты 2025 2026")
    }

    fun hasValidAuthCookie(cookie: String?): Boolean {
        if (cookie.isNullOrBlank()) return false
        return extractSapisid(cookie) != null
    }

    private fun extractSapisid(cookie: String): String? {
        val parts = cookie.split(";").map { it.trim() }
        for (part in parts) {
            if (part.startsWith("SAPISID=")) {
                return part.removePrefix("SAPISID=").trim().takeIf { it.isNotEmpty() }
            }
            if (part.startsWith("__Secure-3PAPISID=")) {
                return part.removePrefix("__Secure-3PAPISID=").trim().takeIf { it.isNotEmpty() }
            }
        }
        return null
    }

    private fun buildSapisidHashHeader(cookie: String, origin: String = YTM_ORIGIN): String? {
        val sapisid = extractSapisid(cookie) ?: return null
        val timestampSec = System.currentTimeMillis() / 1000L
        val input = "$timestampSec $sapisid $origin"
        val digest = MessageDigest.getInstance("SHA-1").digest(input.toByteArray(StandardCharsets.UTF_8))
        val sha1Hex = digest.joinToString("") { "%02x".format(it) }
        return "SAPISIDHASH ${timestampSec}_$sha1Hex"
    }

    private fun buildWebRemixContextJson(): JSONObject {
        return JSONObject().apply {
            put(
                "context",
                JSONObject().apply {
                    put(
                        "client",
                        JSONObject().apply {
                            put("clientName", "WEB_REMIX")
                            put("clientVersion", "1.20250219.01.00")
                            put("hl", "ru")
                            put("gl", "RU")
                        }
                    )
                }
            )
        }
    }

    private fun postInnertubeJson(
        endpointUrl: String,
        body: JSONObject,
        cookie: String?
    ): JSONObject? {
        return runCatching {
            val conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 10000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Origin", YTM_ORIGIN)
                setRequestProperty("X-Origin", YTM_ORIGIN)
                setRequestProperty("Referer", "$YTM_ORIGIN/")
                setRequestProperty("X-Goog-AuthUser", "0")
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                )
                if (!cookie.isNullOrBlank()) {
                    setRequestProperty("Cookie", cookie)
                    buildSapisidHashHeader(cookie, YTM_ORIGIN)?.let { auth ->
                        setRequestProperty("Authorization", auth)
                    }
                }
            }
            conn.outputStream.use { out ->
                out.write(body.toString().toByteArray(StandardCharsets.UTF_8))
            }
            if (conn.responseCode in 200..299) {
                val respText = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(respText)
            } else {
                null
            }
        }.getOrNull()
    }

    private fun getOrFetchVisitorData(): String? {
        val now = System.currentTimeMillis()
        val existing = cachedVisitorData
        if (!existing.isNullOrBlank() && (now - visitorDataFetchedAtMs) < 3600_000L) {
            return existing
        }
        return runCatching {
            val body = JSONObject().apply {
                put(
                    "context",
                    JSONObject().apply {
                        put(
                            "client",
                            JSONObject().apply {
                                put("clientName", "ANDROID")
                                put("clientVersion", ANDROID_CLIENT_VERSION)
                                put("androidSdkVersion", 34)
                                put("osName", "Android")
                                put("osVersion", "14")
                                put("hl", "en")
                                put("gl", "US")
                            }
                        )
                    }
                )
            }
            val conn = (URL(YT_VISITOR_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", ANDROID_USER_AGENT)
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val visitor = json.optJSONObject("responseContext")?.optString("visitorData", "")
                if (!visitor.isNullOrBlank()) {
                    cachedVisitorData = visitor
                    visitorDataFetchedAtMs = now
                    visitor
                } else null
            } else null
        }.getOrNull()
    }

    suspend fun fetchAuthenticatedAccountProfile(cookie: String): GoogleAccountProfile? = withContext(Dispatchers.IO) {
        currentAuthCookie = cookie
        val body = buildWebRemixContextJson()
        val json = postInnertubeJson(YTM_ACCOUNT_URL, body, cookie) ?: return@withContext null
        runCatching {
            val header = json.optJSONArray("actions")
                ?.optJSONObject(0)
                ?.optJSONObject("openPopupAction")
                ?.optJSONObject("popup")
                ?.optJSONObject("multiPageMenuRenderer")
                ?.optJSONObject("header")
                ?.optJSONObject("activeAccountHeaderRenderer")
                ?: return@runCatching null

            val name = extractRunsText(header.optJSONObject("accountName")).ifBlank { "Google Пользователь" }
            val email = extractRunsText(header.optJSONObject("email"))
                .ifBlank { extractRunsText(header.optJSONObject("channelHandle")) }
                .ifBlank { "Синхронизировано с YouTube Music" }

            val photoThumbs = header.optJSONObject("accountPhoto")?.optJSONArray("thumbnails")
            val rawAvatar = if (photoThumbs != null && photoThumbs.length() > 0) {
                photoThumbs.optJSONObject(photoThumbs.length() - 1)?.optString("url")
            } else null
            val avatarUrl = rawAvatar?.let {
                if (it.startsWith("//")) "https:$it" else it
            }

            GoogleAccountProfile(displayName = name, email = email, avatarUrl = avatarUrl)
        }.getOrNull()
    }

    suspend fun syncAuthenticatedAccount(cookie: String): YtAccountSyncResult = withContext(Dispatchers.IO) {
        currentAuthCookie = cookie
        coroutineScope {
            val visitorJob = async { getOrFetchVisitorData() }
            val profileDeferred = async { fetchAuthenticatedAccountProfile(cookie) }
            val historyDeferred = async { fetchBrowseTracks("FEmusic_history", cookie, limit = 18) }
            val likedDeferred = async { fetchBrowseTracks("FEmusic_liked_videos", cookie, limit = 24) }
            val homeDeferred = async { fetchBrowseTracks("FEmusic_home", cookie, limit = 20) }

            visitorJob.await()
            YtAccountSyncResult(
                profile = profileDeferred.await(),
                historySongs = historyDeferred.await(),
                likedSongs = likedDeferred.await(),
                homeRecommendations = homeDeferred.await()
            )
        }
    }

    suspend fun removeLikeFromYtMusic(videoId: String, cookie: String?): Boolean = withContext(Dispatchers.IO) {
        if (cookie.isNullOrBlank()) return@withContext false
        val cleanId = videoId.removePrefix("yt_")
        val body = buildWebRemixContextJson().apply {
            put("target", JSONObject().apply {
                put("videoId", cleanId)
            })
        }
        val res = postInnertubeJson("$YTM_ORIGIN/youtubei/v1/like/removelike?prettyPrint=false", body, cookie)
        res != null
    }

    suspend fun likeYtMusicVideo(videoId: String, cookie: String?): Boolean = withContext(Dispatchers.IO) {
        if (cookie.isNullOrBlank()) return@withContext false
        val cleanId = videoId.removePrefix("yt_")
        val body = buildWebRemixContextJson().apply {
            put("target", JSONObject().apply {
                put("videoId", cleanId)
            })
        }
        val res = postInnertubeJson("$YTM_ORIGIN/youtubei/v1/like/like?prettyPrint=false", body, cookie)
        res != null
    }

    private fun fetchBrowseTracks(browseId: String, cookie: String?, limit: Int = 18): List<Song> {
        val body = buildWebRemixContextJson().apply {
            put("browseId", browseId)
        }
        val json = postInnertubeJson(YTM_BROWSE_URL, body, cookie) ?: return emptyList()
        val found = mutableListOf<Song>()
        collectMusicItemsRecursive(json, found, limit)
        return found.distinctBy { "${it.title.lowercase()}_${it.artist.lowercase()}" }.take(limit)
    }

    private fun collectMusicItemsRecursive(node: Any?, out: MutableList<Song>, maxCount: Int) {
        if (node == null || out.size >= maxCount) return
        when (node) {
            is JSONObject -> {
                val listItem = node.optJSONObject("musicResponsiveListItemRenderer")
                if (listItem != null) {
                    parseResponsiveListItem(listItem)?.let { out.add(it) }
                }
                val twoRow = node.optJSONObject("musicTwoRowItemRenderer")
                if (twoRow != null) {
                    parseTwoRowItem(twoRow)?.let { out.add(it) }
                }
                val keys = node.keys()
                while (keys.hasNext() && out.size < maxCount) {
                    val k = keys.next()
                    collectMusicItemsRecursive(node.opt(k), out, maxCount)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    if (out.size >= maxCount) break
                    collectMusicItemsRecursive(node.opt(i), out, maxCount)
                }
            }
        }
    }

    private fun parseDurationTextMs(text: String): Long? {
        val clean = text.trim()
        if (!clean.matches(Regex("""^\d{1,2}:\d{2}(:\d{2})?$"""))) return null
        val parts = clean.split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            2 -> (parts[0] * 60L + parts[1]) * 1000L
            3 -> (parts[0] * 3600L + parts[1] * 60L + parts[2]) * 1000L
            else -> null
        }
    }

    fun isGenericTrackTypeLabel(text: String): Boolean {
        val clean = text.trim().lowercase()
        return clean in setOf(
            "композиция", "песня", "трек", "сингл", "видео", "видеоклип", "клип", "аудио",
            "song", "track", "single", "video", "music video", "audio"
        ) || clean.contains("просмотр") || clean.contains("views") ||
           clean.contains("подписчик") || clean.contains("subscribers")
    }

    private fun parseResponsiveListItem(item: JSONObject): Song? {
        val videoId = item.optJSONObject("playlistItemData")?.optString("videoId", "")
            ?.ifBlank {
                item.optJSONObject("overlay")
                    ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("musicPlayButtonRenderer")
                    ?.optJSONObject("playNavigationEndpoint")
                    ?.optJSONObject("watchEndpoint")
                    ?.optString("videoId", "")
            } ?: ""

        if (videoId.isBlank()) return null

        val flexCols = item.optJSONArray("flexColumns") ?: return null
        var title = flexCols.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.let { extractRunsText(it) }
            .orEmpty()

        if (title.isBlank()) return null

        var parsedDurationMs: Long? = null

        val fixedCols = item.optJSONArray("fixedColumns")
        if (fixedCols != null && fixedCols.length() > 0) {
            val fixedText = fixedCols.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                ?.optJSONObject("text")
                ?.let { extractRunsText(it) }
                .orEmpty()
            parsedDurationMs = parseDurationTextMs(fixedText)
        }

        val subtitleRuns = flexCols.optJSONObject(1)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")

        var artist = "YouTube Music"
        var album = "YouTube Music"
        if (subtitleRuns != null && subtitleRuns.length() > 0) {
            val parts = mutableListOf<String>()
            for (i in 0 until subtitleRuns.length()) {
                val t = subtitleRuns.optJSONObject(i)?.optString("text", "")?.trim().orEmpty()
                if (t.isEmpty() || t == "•" || t == " • ") continue
                val maybeDur = parseDurationTextMs(t)
                if (maybeDur != null) {
                    parsedDurationMs = maybeDur
                    continue
                }
                if (!isGenericTrackTypeLabel(t)) {
                    parts.add(t)
                }
            }
            val validArtistCandidate = parts.firstOrNull { !isGenericTrackTypeLabel(it) }
            if (validArtistCandidate != null) {
                artist = validArtistCandidate
                val remainingParts = parts.filterNot { it == validArtistCandidate }
                if (remainingParts.isNotEmpty()) album = remainingParts[0]
            }
        }

        if (isGenericTrackTypeLabel(artist) || artist == "YouTube Music") {
            val (cleanedTitle, cleanedArtist) = normalizeTrackTitleAndArtist(title, artist)
            if (cleanedArtist != "Неизвестный исполнитель" && !isGenericTrackTypeLabel(cleanedArtist)) {
                artist = cleanedArtist
                title = cleanedTitle
            }
        }

        val thumbUrl = item.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.let { arr ->
                arr.optJSONObject(arr.length() - 1)?.optString("url", "")
            }
            ?.replace("w120-h120", "w544-h544")
            ?.replace("w60-h60", "w544-h544")

        return Song(
            id = "yt_v_$videoId",
            title = title,
            artist = artist,
            album = album,
            durationMs = parsedDurationMs ?: 195_000L,
            uri = "yt://$videoId",
            coverColorSeed = videoId.hashCode(),
            albumArtUri = thumbUrl?.takeIf { it.isNotBlank() },
            isLocal = false,
            folderName = "YouTube Music",
            dateAddedSec = System.currentTimeMillis() / 1000L,
            genre = "YouTube Music",
            year = "2025",
            isRemoteYt = true
        )
    }

    private fun parseTwoRowItem(item: JSONObject): Song? {
        val videoId = item.optJSONObject("navigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId", "")
            .orEmpty()
        if (videoId.isBlank()) return null

        val title = extractRunsText(item.optJSONObject("title"))
        if (title.isBlank()) return null

        val subtitle = extractRunsText(item.optJSONObject("subtitle"))
        var artist = subtitle.split("•")
            .map { it.trim() }
            .firstOrNull {
                it.isNotEmpty() &&
                    !isGenericTrackTypeLabel(it) &&
                    parseDurationTextMs(it) == null
            }
            ?: "YouTube Music"

        if (isGenericTrackTypeLabel(artist) || artist == "YouTube Music") {
            val (cleanedTitle, cleanedArtist) = normalizeTrackTitleAndArtist(title, artist)
            if (cleanedArtist != "Неизвестный исполнитель" && !isGenericTrackTypeLabel(cleanedArtist)) {
                artist = cleanedArtist
            }
        }

        val thumbUrl = item.optJSONObject("thumbnailRenderer")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.let { arr ->
                arr.optJSONObject(arr.length() - 1)?.optString("url", "")
            }
            ?.replace("w120-h120", "w544-h544")
            ?.replace("w60-h60", "w544-h544")

        return Song(
            id = "yt_v_$videoId",
            title = title,
            artist = artist,
            album = "YouTube Music",
            durationMs = 195_000L,
            uri = "yt://$videoId",
            coverColorSeed = videoId.hashCode(),
            albumArtUri = thumbUrl?.takeIf { it.isNotBlank() },
            isLocal = false,
            folderName = "YouTube Music",
            dateAddedSec = System.currentTimeMillis() / 1000L,
            genre = "YouTube Music",
            year = "2025",
            isRemoteYt = true
        )
    }

    private fun extractRunsText(textObj: JSONObject?): String {
        if (textObj == null) return ""
        val simple = textObj.optString("simpleText", "")
        if (simple.isNotBlank()) return simple
        val runs = textObj.optJSONArray("runs") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text", "").orEmpty())
        }
        return sb.toString().trim()
    }

    fun extractVideoId(songOrUri: String): String? {
        if (songOrUri.startsWith("yt://")) {
            return songOrUri.removePrefix("yt://").trim().takeIf { it.isNotEmpty() }
        }
        if (songOrUri.startsWith("yt_v_")) {
            return songOrUri.removePrefix("yt_v_").trim().takeIf { it.isNotEmpty() }
        }
        return null
    }

    fun resolveStreamUriForExoPlayer(uri: Uri): Uri {
        val raw = uri.toString()
        if (raw.startsWith("yt://")) {
            val videoId = raw.removePrefix("yt://").trim()
            val resolved = fetchDirectInnertubeAudioStream(videoId)
            if (resolved != null && resolved.first.isNotBlank()) {
                return Uri.parse(resolved.first)
            }
        }
        return uri
    }

    fun resolveSongStream(song: Song): Song? {
        val videoId = extractVideoId(song.uri) ?: extractVideoId(song.id)
        if (!videoId.isNullOrBlank()) {
            val streamPair = fetchDirectInnertubeAudioStream(videoId)
            if (streamPair != null) {
                val (streamUrl, durMs) = streamPair
                return song.copy(
                    uri = streamUrl,
                    durationMs = if (durMs > 0L) durMs else song.durationMs
                )
            }
            return song
        }
        if (song.uri.startsWith("http://") || song.uri.startsWith("https://")) {
            return song
        }
        return null
    }

    fun fetchDirectInnertubeAudioStream(videoId: String): Pair<String, Long>? {
        val cached = streamCache[videoId]
        val now = System.currentTimeMillis()
        if (cached != null && (now - cached.createdAtMs) < 2 * 3600 * 1000L) {
            return cached.url to cached.durationMs
        }

        val visitorData = getOrFetchVisitorData()

        requestAndroidProgressiveStream(videoId, visitorData, includeAuthCookie = false)?.let { result ->
            streamCache[videoId] = CachedAudioStream(result.first, result.second)
            return result
        }

        requestIosProgressiveStream(videoId)?.let { result ->
            streamCache[videoId] = CachedAudioStream(result.first, result.second)
            return result
        }

        if (!currentAuthCookie.isNullOrBlank()) {
            requestAndroidProgressiveStream(videoId, visitorData, includeAuthCookie = true)?.let { result ->
                streamCache[videoId] = CachedAudioStream(result.first, result.second)
                return result
            }
        }

        requestWebRemixStream(videoId)?.let { result ->
            streamCache[videoId] = CachedAudioStream(result.first, result.second)
            return result
        }

        requestMirrorAudioStream(videoId)?.let { result ->
            streamCache[videoId] = CachedAudioStream(result.first, result.second)
            return result
        }

        return null
    }

    private fun requestIosProgressiveStream(videoId: String): Pair<String, Long>? {
        return runCatching {
            val clientJson = JSONObject().apply {
                put("clientName", "IOS")
                put("clientVersion", "19.45.4")
                put("deviceMake", "Apple")
                put("deviceModel", "iPhone16,2")
                put("osName", "iOS")
                put("osVersion", "18.1.0.22B83")
                put("hl", "en")
                put("gl", "US")
            }
            val body = JSONObject().apply {
                put("videoId", videoId)
                put("contentCheckOk", true)
                put("racyCheckOk", true)
                put("context", JSONObject().apply { put("client", clientJson) })
            }
            val conn = (URL(YT_PLAYER_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 18_1 like Mac OS X; en_US)")
                setRequestProperty("X-YouTube-Client-Name", "5")
                setRequestProperty("X-YouTube-Client-Version", "19.45.4")
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            if (conn.responseCode != 200) return null

            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            extractAudioStreamFromJson(json)
        }.getOrNull()
    }

    private fun requestWebRemixStream(videoId: String): Pair<String, Long>? {
        return runCatching {
            val body = buildWebRemixContextJson().apply {
                put("videoId", videoId)
                put("contentCheckOk", true)
                put("racyCheckOk", true)
            }
            val conn = (URL("https://music.youtube.com/youtubei/v1/player?prettyPrint=false").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                setRequestProperty("Referer", "https://music.youtube.com/")
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            if (conn.responseCode != 200) return null

            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            extractAudioStreamFromJson(json)
        }.getOrNull()
    }

    private fun requestMirrorAudioStream(videoId: String): Pair<String, Long>? {
        val mirrors = listOf(
            "https://pipedapi.kavin.rocks/streams/$videoId",
            "https://api.piped.privacydev.net/streams/$videoId",
            "https://invidious.nerdvpn.de/api/v1/videos/$videoId"
        )
        for (mUrl in mirrors) {
            val res = runCatching {
                val conn = (URL(mUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7000
                    readTimeout = 7000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) LorMusic/1.7.0")
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    var audioUrl: String? = null
                    var durMs: Long = 0L

                    val audioStreams = json.optJSONArray("audioStreams")
                    if (audioStreams != null && audioStreams.length() > 0) {
                        for (i in 0 until audioStreams.length()) {
                            val st = audioStreams.getJSONObject(i)
                            val u = st.optString("url")
                            if (u.isNotBlank()) {
                                audioUrl = u
                                val bit = st.optInt("bitrate", 0)
                                if (bit >= 128000) break
                            }
                        }
                    } else {
                        val adaptive = json.optJSONArray("adaptiveFormats")
                        if (adaptive != null) {
                            for (i in 0 until adaptive.length()) {
                                val fmt = adaptive.getJSONObject(i)
                                val mime = fmt.optString("type", "")
                                val u = fmt.optString("url", "")
                                if (u.isNotBlank() && mime.startsWith("audio/")) {
                                    audioUrl = u
                                    durMs = fmt.optString("approxDurationMs", "0").toLongOrNull() ?: 0L
                                    break
                                }
                            }
                        }
                    }
                    if (!audioUrl.isNullOrBlank()) {
                        audioUrl to durMs
                    } else null
                } else null
            }.getOrNull()
            if (res != null) return res
        }
        return null
    }

    private fun extractAudioStreamFromJson(json: JSONObject): Pair<String, Long>? {
        val streamingData = json.optJSONObject("streamingData") ?: return null
        val videoLengthSec = json.optJSONObject("videoDetails")
            ?.optString("lengthSeconds", "0")
            ?.toLongOrNull() ?: 0L

        val formats = streamingData.optJSONArray("formats")
        if (formats != null) {
            for (i in 0 until formats.length()) {
                val fmt = formats.getJSONObject(i)
                val url = fmt.optString("url", "")
                if (url.isNotBlank()) {
                    val durMs = fmt.optString("approxDurationMs", "0").toLongOrNull()
                        ?.takeIf { it > 0L }
                        ?: (videoLengthSec * 1000L)
                    return url to durMs
                }
            }
        }

        val adaptive = streamingData.optJSONArray("adaptiveFormats")
        if (adaptive != null) {
            var bestMp4: Pair<String, Long>? = null
            var bestWebm: Pair<String, Long>? = null

            for (i in 0 until adaptive.length()) {
                val fmt = adaptive.getJSONObject(i)
                val mime = fmt.optString("mimeType", "")
                val url = fmt.optString("url", "")
                val contentLen = fmt.optString("contentLength", "").toLongOrNull()
                if (url.isNotBlank() && mime.startsWith("audio/")) {
                    val durMs = fmt.optString("approxDurationMs", "0").toLongOrNull()
                        ?.takeIf { it > 0L }
                        ?: (videoLengthSec * 1000L)
                    val rangedUrl = if (contentLen != null && contentLen > 0L && !url.contains("range=")) {
                        "$url&range=0-${contentLen - 1}"
                    } else {
                        url
                    }
                    if (mime.startsWith("audio/mp4") && bestMp4 == null) {
                        bestMp4 = rangedUrl to durMs
                    } else if (bestWebm == null) {
                        bestWebm = rangedUrl to durMs
                    }
                }
            }
            if (bestMp4 != null) return bestMp4
            if (bestWebm != null) return bestWebm
        }
        return null
    }

    private fun requestAndroidProgressiveStream(
        videoId: String,
        visitorData: String?,
        includeAuthCookie: Boolean
    ): Pair<String, Long>? {
        return runCatching {
            val clientJson = JSONObject().apply {
                put("clientName", "ANDROID")
                put("clientVersion", ANDROID_CLIENT_VERSION)
                put("androidSdkVersion", 34)
                put("osName", "Android")
                put("osVersion", "14")
                put("hl", "en")
                put("gl", "US")
                if (!visitorData.isNullOrBlank()) {
                    put("visitorData", visitorData)
                }
            }
            val body = JSONObject().apply {
                put("videoId", videoId)
                put("contentCheckOk", true)
                put("racyCheckOk", true)
                put("context", JSONObject().apply { put("client", clientJson) })
            }
            val conn = (URL(YT_PLAYER_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", ANDROID_USER_AGENT)
                if (!visitorData.isNullOrBlank()) {
                    setRequestProperty("X-Goog-Visitor-Id", visitorData)
                }
                val cookie = currentAuthCookie
                if (includeAuthCookie && !cookie.isNullOrBlank()) {
                    setRequestProperty("Cookie", cookie)
                    buildSapisidHashHeader(cookie, YT_ORIGIN)?.let { auth ->
                        setRequestProperty("Authorization", auth)
                    }
                }
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            if (conn.responseCode != 200) return null

            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            extractAudioStreamFromJson(json)
        }.getOrNull()
    }

    suspend fun searchYtMusic(
        query: String,
        limit: Int = 20,
        cookie: String? = null
    ): List<Song> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext emptyList()
        if (!cookie.isNullOrBlank()) currentAuthCookie = cookie

        runCatching {
            val body = buildWebRemixContextJson().apply {
                put("query", clean)
                put("params", "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
            }
            val json = postInnertubeJson(YTM_SEARCH_URL, body, cookie)
            val list = mutableListOf<Song>()
            if (json != null) {
                collectMusicItemsRecursive(json, list, maxCount = limit)
            }
            list.distinctBy { "${it.title.lowercase()}_${it.artist.lowercase()}" }.take(limit)
        }.getOrDefault(emptyList())
    }

    suspend fun loadPersonalizedHomeFeed(
        seedSongs: List<Song>,
        cookie: String? = null
    ): Pair<List<Song>, List<Song>> = withContext(Dispatchers.IO) {
        if (!cookie.isNullOrBlank()) currentAuthCookie = cookie

        coroutineScope {
            val hasAuth = hasValidAuthCookie(cookie)
            val homeDef = if (hasAuth) async { fetchBrowseTracks("FEmusic_home", cookie, limit = 18) } else null
            val likedDef = if (hasAuth) async { fetchBrowseTracks("FEmusic_liked_videos", cookie, limit = 16) } else null
            val homeTracks = homeDef?.await() ?: emptyList()
            val likedOrHistory = likedDef?.await() ?: emptyList()

            if (homeTracks.isNotEmpty() || likedOrHistory.isNotEmpty()) {
                val recs = homeTracks.ifEmpty { likedOrHistory }
                val radio = likedOrHistory.ifEmpty { homeTracks.shuffled() }
                return@coroutineScope recs to radio
            }

            val topArtists = seedSongs
                .map { it.artist.trim() }
                .filter {
                    it.isNotBlank() &&
                        !it.equals("Неизвестный исполнитель", true) &&
                        !it.equals("<unknown>", true) &&
                        !it.equals("LorMusic Synth", true)
                }
                .distinct()
                .take(3)

            val recQuery = if (topArtists.isNotEmpty()) {
                topArtists.joinToString(" ")
            } else {
                "Дайте танк Где Фантом Перемотка Молчат Дома"
            }

            val recommendations = searchYtMusic(recQuery, limit = 14, cookie = cookie)
            val radioTracks = searchYtMusic("Пост панк инди рок хиты", limit = 12, cookie = cookie)
            recommendations to radioTracks
        }
    }

    suspend fun loadMyWaveTracks(
        seedSongs: List<Song>,
        mood: WaveMood,
        cookie: String? = null
    ): List<Song> = withContext(Dispatchers.IO) {
        if (!cookie.isNullOrBlank()) currentAuthCookie = cookie

        val userArtists = seedSongs
            .map { it.artist.trim() }
            .filter {
                it.isNotBlank() &&
                    !it.equals("Неизвестный исполнитель", true) &&
                    !it.equals("<unknown>", true) &&
                    !it.equals("LorMusic Synth", true)
            }
            .distinct()
            .shuffled()

        val selectedArtists = userArtists.take(4)

        val dynamicGenres = listOf(
            "Indie Pop Hits",
            "Synthwave Retrowave",
            "Phonk Drift Mix",
            "Lo-Fi Beats Chill",
            "Deep House Vocal",
            "Alternative Rock Radio",
            "Hip Hop Rap New",
            "Pop New Wave Hits"
        ).shuffled()

        val results = mutableListOf<Song>()

        if (mood == WaveMood.FOR_YOU) {
            val accountHome = fetchBrowseTracks("FEmusic_home", cookie, limit = 30)
            results.addAll(accountHome)
        }

        val mainQuery = when (mood) {
            WaveMood.FOR_YOU -> if (selectedArtists.isNotEmpty()) {
                "${selectedArtists.take(2).joinToString(" ")} mix"
            } else {
                "${dynamicGenres[0]} 2024"
            }
            else -> "${mood.queryHint} ${dynamicGenres[0]}"
        }
        val mainResults = searchYtMusic(mainQuery, limit = 35, cookie = cookie)
        results.addAll(mainResults)

        if (results.distinctBy { it.id }.size < 50) {
            val secondQuery = when (mood) {
                WaveMood.FOR_YOU -> if (selectedArtists.size >= 3) {
                    "${selectedArtists.drop(2).joinToString(" ")} mix"
                } else {
                    "${dynamicGenres.getOrElse(1) { "Music Mix" }} Hits"
                }
                else -> "${mood.queryHint} hits popular radio"
            }
            val extraResults = searchYtMusic(secondQuery, limit = 35, cookie = cookie)
            results.addAll(extraResults)
        }

        val combined = results.distinctBy { it.id }.shuffled()
        if (combined.isNotEmpty()) combined.take(50) else emptyList()
    }

    suspend fun fetchArtistProfile(
        artistName: String,
        cookie: String? = null
    ): ArtistProfileData = withContext(Dispatchers.IO) {
        val clean = artistName.trim()
        if (clean.isBlank()) {
            return@withContext ArtistProfileData(
                artistName = artistName,
                avatarUrl = null,
                topTracks = emptyList(),
                albums = emptyMap(),
                singles = emptyList()
            )
        }

        val topTracksRaw = searchYtMusic(clean, limit = 30, cookie = cookie)
        val sortedTopTracks = topTracksRaw
            .sortedByDescending {
                if (it.artist.contains(clean, ignoreCase = true)) 10 else 0
            }
            .take(20)

        val isSinglePredicate: (Song) -> Boolean = { song ->
            val alb = song.album.trim()
            val title = song.title.trim()
            alb.isBlank() ||
                alb.equals("Unknown", ignoreCase = true) ||
                alb.equals("YouTube Music", ignoreCase = true) ||
                alb.equals(clean, ignoreCase = true) ||
                alb.equals(title, ignoreCase = true) ||
                alb.contains("Single", ignoreCase = true) ||
                alb.contains(" - Single", ignoreCase = true) ||
                alb.contains(" - EP", ignoreCase = true) ||
                alb.contains("(EP)", ignoreCase = true) ||
                alb.equals("EP", ignoreCase = true) ||
                title.contains(" - Single", ignoreCase = true) ||
                title.contains("(Single)", ignoreCase = true)
        }

        val albumsRaw = searchYtMusic("$clean album", limit = 25, cookie = cookie)
        val allArtistTracks = (topTracksRaw + albumsRaw).distinctBy { it.id }

        val groupedAlbums = allArtistTracks
            .filter { !isSinglePredicate(it) }
            .groupBy { it.album }
            .filter { it.value.size >= 2 || (it.value.size == 1 && !it.key.contains("Single", ignoreCase = true) && !it.key.equals(it.value.first().title, ignoreCase = true)) }

        val albumTrackIds = groupedAlbums.values.flatten().map { it.id }.toSet()

        val singlesRaw = searchYtMusic("$clean single", limit = 20, cookie = cookie)
        val singlesList = (singlesRaw + allArtistTracks)
            .filter { !albumTrackIds.contains(it.id) || isSinglePredicate(it) }
            .distinctBy { it.title.lowercase() }
            .take(20)

        val avatar = TagFixEngine.fetchArtistAvatar(clean)
            ?: sortedTopTracks.firstOrNull { !it.albumArtUri.isNullOrBlank() }?.albumArtUri

        ArtistProfileData(
            artistName = clean,
            avatarUrl = avatar,
            topTracks = sortedTopTracks,
            albums = groupedAlbums,
            singles = singlesList
        )
    }

    suspend fun fetchAlbumTracks(
        artist: String,
        album: String,
        cookie: String? = null
    ): List<Song> = withContext(Dispatchers.IO) {
        val cleanArtist = artist.trim()
        val cleanAlbum = album.trim()
        if (cleanAlbum.isBlank()) return@withContext emptyList()
        val results = searchYtMusic("$cleanArtist $cleanAlbum", limit = 30, cookie = cookie)
        results.filter { song ->
            song.album.equals(cleanAlbum, ignoreCase = true) ||
                song.album.contains(cleanAlbum, ignoreCase = true) ||
                (song.artist.contains(cleanArtist, ignoreCase = true) && !song.title.contains(" - Single", ignoreCase = true))
        }
    }

    fun getShareUrl(song: Song): String? {
        val videoId = extractVideoId(song.id) ?: extractVideoId(song.uri)
        if (!videoId.isNullOrBlank()) {
            return "https://music.youtube.com/watch?v=$videoId"
        }
        if (song.isRemoteYt || song.id.startsWith("yt_") || song.uri.startsWith("yt://")) {
            val query = Uri.encode("${song.artist} ${song.title}".trim())
            return "https://music.youtube.com/search?q=$query"
        }
        return null
    }

    fun shareSongLink(context: Context, song: Song) {
        val shareUrl = getShareUrl(song) ?: return
        val shareText = "${song.artist} — ${song.title}\n$shareUrl"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${song.artist} — ${song.title}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(sendIntent, "Поделиться песней").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(chooser)
        }
    }

    suspend fun downloadTrackToPhoneWithTags(
        context: Context,
        song: Song,
        onProgress: ((Float) -> Unit)? = null
    ): Pair<Song, SongTagOverride>? = withContext(Dispatchers.IO) {
        runCatching {
            val videoId = extractVideoId(song.id) ?: extractVideoId(song.uri)
            val streamPair = if (!videoId.isNullOrBlank()) {
                fetchDirectInnertubeAudioStream(videoId)
            } else null

            val streamUrl = streamPair?.first ?: song.uri
            val realDurationMs = streamPair?.second?.takeIf { it > 0L } ?: song.durationMs

            val (cleanTitle, cleanArtist) = normalizeTrackTitleAndArtist(song.title, song.artist)
            val cleanAlbum = song.album.takeIf {
                it.isNotBlank() && !it.equals("LorMusic", ignoreCase = true) && !it.equals("Музыка", ignoreCase = true)
            } ?: "YouTube Music"
            val cleanGenre = song.genre.ifBlank { "YouTube Music" }
            val cleanYear = song.year.ifBlank { "2025" }

            val tempInput = File(context.cacheDir, "yt_dl_${System.currentTimeMillis()}.mp4")
            val tempAudioOut = File(context.cacheDir, "yt_audio_${System.currentTimeMillis()}.m4a")

            try {
                val audioConn = (URL(streamUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 25000
                    setRequestProperty("User-Agent", ANDROID_USER_AGENT)
                }
                val contentLength = audioConn.contentLengthLong
                audioConn.inputStream.use { input ->
                    FileOutputStream(tempInput).use { output ->
                        val buffer = ByteArray(16384)
                        var bytesRead: Int
                        var totalBytes = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                            if (contentLength > 0L) {
                                val progress = (totalBytes.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                                onProgress?.invoke(progress)
                            }
                        }
                    }
                }

                val remuxedOk = extractPureAudioM4a(tempInput, tempAudioOut)
                val finalSourceFile = if (remuxedOk && tempAudioOut.exists() && tempAudioOut.length() > 1024L) {
                    tempAudioOut
                } else {
                    tempInput
                }

                val remoteArtUrl = song.albumArtUri?.takeIf { it.startsWith("http") }
                    ?: videoId?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }
                val coverBytes = remoteArtUrl?.let { downloadBinaryBytes(it) }

                runCatching {
                    injectM4aMetadataAtoms(
                        m4aFile = finalSourceFile,
                        title = cleanTitle,
                        artist = cleanArtist,
                        album = cleanAlbum,
                        genre = cleanGenre,
                        year = cleanYear,
                        coverBytes = coverBytes
                    )
                }

                val cleanFileName = "$cleanArtist - $cleanTitle"
                    .replace(Regex("""[\\/:*?"<>|]"""), "_")
                    .take(90) + ".m4a"

                var savedUriString: String? = null
                var savedFilePath: String? = null
                var mediaStoreNumericId: Long? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, cleanFileName)
                        put(MediaStore.Audio.Media.TITLE, cleanTitle)
                        put(MediaStore.Audio.Media.ARTIST, cleanArtist)
                        put(MediaStore.Audio.Media.ALBUM, cleanAlbum)
                        put(MediaStore.Audio.Media.YEAR, cleanYear.toIntOrNull() ?: 2025)
                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                        put(MediaStore.Audio.Media.DURATION, realDurationMs)
                        put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/LorMusic")
                        put(MediaStore.Audio.Media.IS_MUSIC, 1)
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }
                    val itemUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    if (itemUri != null) {
                        resolver.openOutputStream(itemUri)?.use { out ->
                            finalSourceFile.inputStream().use { inp -> inp.copyTo(out) }
                        }

                        val doneValues = ContentValues().apply {
                            put(MediaStore.Audio.Media.IS_PENDING, 0)
                            put(MediaStore.Audio.Media.TITLE, cleanTitle)
                            put(MediaStore.Audio.Media.ARTIST, cleanArtist)
                            put(MediaStore.Audio.Media.ALBUM, cleanAlbum)
                            put(MediaStore.Audio.Media.DURATION, realDurationMs)
                        }
                        resolver.update(itemUri, doneValues, null, null)
                        savedUriString = itemUri.toString()
                        savedFilePath = "/storage/emulated/0/Music/LorMusic/$cleanFileName"
                        mediaStoreNumericId = runCatching { ContentUris.parseId(itemUri) }.getOrNull()
                    }
                }

                if (savedUriString == null) {
                    val musicDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "LorMusic").apply { mkdirs() }
                    val outFile = File(musicDir, cleanFileName)
                    finalSourceFile.copyTo(outFile, overwrite = true)
                    savedUriString = Uri.fromFile(outFile).toString()
                    savedFilePath = outFile.absolutePath
                }

                val localId = if (mediaStoreNumericId != null && mediaStoreNumericId > 0L) {
                    "local_$mediaStoreNumericId"
                } else {
                    "dl_${System.currentTimeMillis()}"
                }

                val finalCover = if (coverBytes != null && coverBytes.isNotEmpty()) {
                    runCatching {
                        val coversDir = File(context.filesDir, "tagfix_covers").apply { mkdirs() }
                        val safeKey = localId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                        val coverFile = File(coversDir, "cover_${safeKey}.jpg")
                        FileOutputStream(coverFile).use { it.write(coverBytes) }
                        Uri.fromFile(coverFile).toString()
                    }.getOrNull() ?: remoteArtUrl
                } else {
                    remoteArtUrl?.let { TagFixEngine.cacheRemoteCoverToDisk(context, localId, it) } ?: song.albumArtUri
                }

                val downloadedSong = song.copy(
                    id = localId,
                    title = cleanTitle,
                    artist = cleanArtist,
                    album = cleanAlbum,
                    genre = cleanGenre,
                    year = cleanYear,
                    uri = savedUriString!!,
                    durationMs = realDurationMs,
                    albumArtUri = finalCover,
                    isLocal = true,
                    isRemoteYt = false,
                    filePath = savedFilePath,
                    folderName = "LorMusic",
                    dateAddedSec = System.currentTimeMillis() / 1000L
                )

                val override = SongTagOverride(
                    songId = localId,
                    title = cleanTitle,
                    artist = cleanArtist,
                    album = cleanAlbum,
                    genre = cleanGenre,
                    year = cleanYear,
                    trackNumber = "1",
                    albumArtUri = finalCover
                )

                downloadedSong to override
            } finally {
                runCatching { tempInput.delete() }
                runCatching { tempAudioOut.delete() }
            }
        }.getOrNull()
    }

    fun normalizeTrackTitleAndArtist(rawTitle: String, rawArtist: String): Pair<String, String> {
        var title = rawTitle
            .replace(Regex("""\s*\((?:Official\s*(?:Audio|Video|Music\s*Video|Lyric\s*Video)|Lyrics?|Visualizer|HD|4K)\)\s*""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\s*\[(?:Official\s*(?:Audio|Video|Music\s*Video|Lyric\s*Video)|Lyrics?|Visualizer|HD|4K)]\s*""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        var artist = rawArtist
            .replace(Regex("""\s*-\s*Topic$""", RegexOption.IGNORE_CASE), "")
            .trim()

        val separators = listOf(" — ", " – ", " - ")
        for (sep in separators) {
            if (title.contains(sep)) {
                val parts = title.split(sep, limit = 2)
                val left = parts[0].trim()
                val right = parts[1].trim()
                if (left.isNotEmpty() && right.isNotEmpty()) {
                    val artistIsGeneric = artist.isBlank() ||
                        isGenericTrackTypeLabel(artist) ||
                        artist.equals("Неизвестный исполнитель", ignoreCase = true) ||
                        artist.equals("Композиция", ignoreCase = true) ||
                        artist.equals("Видеоклип", ignoreCase = true) ||
                        artist.equals("YouTube Music", ignoreCase = true)
                    if (artistIsGeneric || left.equals(artist, ignoreCase = true)) {
                        artist = left
                        title = right
                        break
                    }
                }
            }
        }
        if (artist.isBlank() || isGenericTrackTypeLabel(artist) || artist.equals("Композиция", ignoreCase = true)) {
            artist = "Неизвестный исполнитель"
        }
        if (title.isBlank()) {
            title = rawTitle.trim().ifBlank { "Без названия" }
        }
        return title to artist
    }

    fun downloadBinaryBytes(url: String): ByteArray? {
        return runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 12000
                setRequestProperty("User-Agent", ANDROID_USER_AGENT)
            }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.use { it.readBytes() }
        }.getOrNull()
    }

    private fun injectM4aMetadataAtoms(
        m4aFile: File,
        title: String,
        artist: String,
        album: String,
        genre: String,
        year: String,
        coverBytes: ByteArray?
    ) {
        val fileBytes = m4aFile.readBytes()
        if (fileBytes.size < 32) return

        val ilstPayload = ByteArrayOutputStream()
        writeItunesTextAtom(ilstPayload, "\u00A9nam", title)
        writeItunesTextAtom(ilstPayload, "\u00A9ART", artist)
        writeItunesTextAtom(ilstPayload, "aART", artist)
        writeItunesTextAtom(ilstPayload, "\u00A9alb", album)
        if (genre.isNotBlank()) writeItunesTextAtom(ilstPayload, "\u00A9gen", genre)
        if (year.isNotBlank()) writeItunesTextAtom(ilstPayload, "\u00A9day", year)
        if (coverBytes != null && coverBytes.isNotEmpty()) {
            val isPng = coverBytes.size > 4 &&
                coverBytes[0] == 0x89.toByte() &&
                coverBytes[1] == 0x50.toByte() &&
                coverBytes[2] == 0x4E.toByte() &&
                coverBytes[3] == 0x47.toByte()
            writeItunesDataAtom(ilstPayload, "covr", if (isPng) 14 else 13, coverBytes)
        }
        val ilstAtom = wrapMp4Atom("ilst", ilstPayload.toByteArray())

        val hdlrPayload = ByteBuffer.allocate(25).apply {
            putInt(0)
            putInt(0)
            put("mdir".toByteArray(StandardCharsets.ISO_8859_1))
            put("appl".toByteArray(StandardCharsets.ISO_8859_1))
            putInt(0)
            putInt(0)
            put(0.toByte())
        }.array()
        val hdlrAtom = wrapMp4Atom("hdlr", hdlrPayload)

        val metaPayload = ByteArrayOutputStream().apply {
            write(byteArrayOf(0, 0, 0, 0))
            write(hdlrAtom)
            write(ilstAtom)
        }.toByteArray()
        val metaAtom = wrapMp4Atom("meta", metaPayload)
        val udtaAtom = wrapMp4Atom("udta", metaAtom)

        var offset = 0
        var moovOffset = -1
        var moovSize = 0
        var mdatOffset = -1

        while (offset + 8 <= fileBytes.size) {
            val size = ByteBuffer.wrap(fileBytes, offset, 4).int
            val type = String(fileBytes, offset + 4, 4, StandardCharsets.ISO_8859_1)
            val actualSize = if (size == 1 && offset + 16 <= fileBytes.size) {
                ByteBuffer.wrap(fileBytes, offset + 8, 8).long.toInt()
            } else if (size <= 0) {
                fileBytes.size - offset
            } else {
                size
            }
            if (actualSize < 8 || offset + actualSize > fileBytes.size) break
            if (type == "moov") {
                moovOffset = offset
                moovSize = actualSize
            } else if (type == "mdat") {
                mdatOffset = offset
            }
            offset += actualSize
        }

        if (moovOffset < 0 || moovSize < 8) return

        val newMoovChildren = ByteArrayOutputStream()
        var childPos = moovOffset + 8
        val moovEnd = moovOffset + moovSize
        while (childPos + 8 <= moovEnd) {
            val cSize = ByteBuffer.wrap(fileBytes, childPos, 4).int
            val cType = String(fileBytes, childPos + 4, 4, StandardCharsets.ISO_8859_1)
            if (cSize < 8 || childPos + cSize > moovEnd) break
            if (cType != "udta") {
                newMoovChildren.write(fileBytes, childPos, cSize)
            }
            childPos += cSize
        }
        newMoovChildren.write(udtaAtom)
        val newMoovBytes = wrapMp4Atom("moov", newMoovChildren.toByteArray())

        val delta = newMoovBytes.size - moovSize
        if (mdatOffset > moovOffset && delta != 0) {
            adjustStcoOffsetsInMoov(newMoovBytes, 8, newMoovBytes.size, delta)
        }

        val out = ByteArrayOutputStream(fileBytes.size + delta.coerceAtLeast(0))
        if (moovOffset > 0) {
            out.write(fileBytes, 0, moovOffset)
        }
        out.write(newMoovBytes)
        val afterMoov = moovOffset + moovSize
        if (afterMoov < fileBytes.size) {
            out.write(fileBytes, afterMoov, fileBytes.size - afterMoov)
        }
        m4aFile.writeBytes(out.toByteArray())
    }

    private fun adjustStcoOffsetsInMoov(buf: ByteArray, start: Int, end: Int, delta: Int) {
        val containerTypes = setOf("trak", "mdia", "minf", "stbl")
        var pos = start
        while (pos + 8 <= end) {
            val size = ByteBuffer.wrap(buf, pos, 4).int
            val type = String(buf, pos + 4, 4, StandardCharsets.ISO_8859_1)
            if (size < 8 || pos + size > end) break
            if (type in containerTypes) {
                adjustStcoOffsetsInMoov(buf, pos + 8, pos + size, delta)
            } else if (type == "stco" && size >= 16) {
                val entryCount = ByteBuffer.wrap(buf, pos + 12, 4).int
                var entryPos = pos + 16
                for (i in 0 until entryCount) {
                    if (entryPos + 4 > pos + size) break
                    val oldOffset = ByteBuffer.wrap(buf, entryPos, 4).int
                    ByteBuffer.wrap(buf, entryPos, 4).putInt(oldOffset + delta)
                    entryPos += 4
                }
            }
            pos += size
        }
    }

    private fun writeItunesTextAtom(out: ByteArrayOutputStream, atomType: String, value: String) {
        val utf8 = value.toByteArray(StandardCharsets.UTF_8)
        writeItunesDataAtom(out, atomType, 1, utf8)
    }

    private fun writeItunesDataAtom(out: ByteArrayOutputStream, atomType: String, dataType: Int, payload: ByteArray) {
        val dataContent = ByteBuffer.allocate(8 + payload.size).apply {
            putInt(dataType)
            putInt(0)
            put(payload)
        }.array()
        val dataAtom = wrapMp4Atom("data", dataContent)
        val itemAtom = wrapMp4Atom(atomType, dataAtom)
        out.write(itemAtom)
    }

    private fun wrapMp4Atom(type: String, payload: ByteArray): ByteArray {
        val typeBytes = type.toByteArray(StandardCharsets.ISO_8859_1)
        val totalSize = 8 + payload.size
        return ByteBuffer.allocate(totalSize).apply {
            putInt(totalSize)
            put(typeBytes, 0, 4)
            put(payload)
        }.array()
    }

    private fun extractPureAudioM4a(inputFile: File, outputFile: File): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            extractor.setDataSource(inputFile.absolutePath)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }
            if (audioTrackIndex < 0 || audioFormat == null) return false

            extractor.selectTrack(audioTrackIndex)
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val dstTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val buffer = ByteBuffer.allocate(256 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(dstTrack, buffer, bufferInfo)
                extractor.advance()
            }
            muxer.stop()
            true
        } catch (_: Exception) {
            false
        } finally {
            runCatching { extractor.release() }
            runCatching { muxer?.release() }
        }
    }
}
