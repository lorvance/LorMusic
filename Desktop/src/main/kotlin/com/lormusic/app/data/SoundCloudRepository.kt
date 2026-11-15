package com.lormusic.app.data

import com.lormusic.app.model.Song
import com.lormusic.app.model.SoundCloudUserProfile
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object SoundCloudRepository {

    private const val DEFAULT_CLIENT_ID = "3GKD969NJWD1eB6gvQJN0GnueYbcWszv"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    @Volatile private var activeClientId: String = DEFAULT_CLIENT_ID

    private val clientRefreshMutex = Mutex()
    private val transcodingsCache = ConcurrentHashMap<String, String>()
    private val trackPermalinkMap = ConcurrentHashMap<String, String>()

    var currentAuthToken: String? = null
    var currentProfile: SoundCloudUserProfile? = null

    fun cleanToken(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.trim()
            .removePrefix("OAuth ")
            .removePrefix("oauth ")
            .removePrefix("Bearer ")
            .removePrefix("bearer ")
            .trim('"', '\'', ' ', '\\')
    }

    private fun getClientId(): String = activeClientId

    private fun openConnection(
        urlStr: String,
        includeAuth: Boolean = false,
        connectTimeout: Int = 10000,
        readTimeout: Int = 12000,
    ): HttpURLConnection {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.setRequestProperty("Accept", "application/json, text/plain, */*")
        if (includeAuth) {
            val token = cleanToken(currentAuthToken)
            if (token.isNotBlank()) {
                conn.setRequestProperty("Authorization", "OAuth $token")
            }
        }
        conn.connectTimeout = connectTimeout
        conn.readTimeout = readTimeout
        return conn
    }

    private suspend fun refreshClientIdIfNeeded(): String =
        clientRefreshMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val conn =
                        URL("https://soundcloud.com/discover").openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", USER_AGENT)
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    val html = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()

                    val pattern =
                        Pattern.compile(
                            "https://[a-zA-Z0-9.-]+\\.sndcdn\\.com/assets/[a-zA-Z0-9.-]+\\.js"
                        )
                    val matcher = pattern.matcher(html)
                    val scripts = mutableListOf<String>()
                    while (matcher.find()) {
                        scripts.add(matcher.group())
                    }

                    val idPattern = Pattern.compile("client_id[:=]\"?([a-zA-Z0-9]{32})\"?")
                    for (scriptUrl in scripts.reversed()) {
                        try {
                            val sConn = URL(scriptUrl).openConnection() as HttpURLConnection
                            sConn.setRequestProperty("User-Agent", USER_AGENT)
                            sConn.connectTimeout = 6000
                            sConn.readTimeout = 6000
                            val js = sConn.inputStream.bufferedReader().use { it.readText() }
                            sConn.disconnect()

                            val idMatcher = idPattern.matcher(js)
                            if (idMatcher.find()) {
                                val found = idMatcher.group(1)
                                if (!found.isNullOrBlank()) {
                                    activeClientId = found
                                    return@withContext found
                                }
                            }
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
                activeClientId
            }
        }

    private fun parseSongFromJson(obj: JSONObject): Song? {
        val trackId = obj.optLong("id", -1L)
        if (trackId <= 0L) return null

        val title = obj.optString("title", "Без названия").trim()
        val userObj = obj.optJSONObject("user")
        val artist = userObj?.optString("username", "SoundCloud")?.trim() ?: "SoundCloud"
        val durationMs = obj.optLong("duration", 180_000L).coerceAtLeast(10_000L)
        val rawArt =
            obj.optString("artwork_url", "").ifBlank { userObj?.optString("avatar_url", "") ?: "" }
        val artworkUrl =
            if (rawArt.isNotBlank()) {
                rawArt.replace("-large", "-t500x500")
            } else null

        val mediaObj = obj.optJSONObject("media")
        val transcodings = mediaObj?.optJSONArray("transcodings")
        if (transcodings != null && transcodings.length() > 0) {
            var selectedUrl: String? = null
            for (i in 0 until transcodings.length()) {
                val tc = transcodings.optJSONObject(i) ?: continue
                val format = tc.optJSONObject("format")
                val protocol = format?.optString("protocol", "") ?: ""
                val tcUrl = tc.optString("url", "")
                if (protocol == "progressive" && tcUrl.isNotBlank()) {
                    selectedUrl = tcUrl
                    break
                }
            }
            if (selectedUrl == null) {
                selectedUrl = transcodings.optJSONObject(0)?.optString("url", "")
            }
            if (!selectedUrl.isNullOrBlank()) {
                transcodingsCache[trackId.toString()] = selectedUrl
            }
        }

        val permalink = obj.optString("permalink_url", "").trim()
        if (permalink.isNotBlank()) {
            trackPermalinkMap["sc_$trackId"] = permalink
            trackPermalinkMap[trackId.toString()] = permalink
        }

        return Song(
            id = "sc_$trackId",
            title = title,
            artist = artist,
            album = "SoundCloud",
            durationMs = durationMs,
            uri = "sc://$trackId",
            coverColorSeed = trackId.hashCode(),
            albumArtUri = artworkUrl,
            isLocal = false,
            folderName = "SoundCloud",
            dateAddedSec = System.currentTimeMillis() / 1000L,
            genre = obj.optString("genre", ""),
            year = "",
            isRemoteSc = true,
        )
    }

    suspend fun searchSoundCloud(query: String, limit: Int = 25): List<Song> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            val songs = mutableListOf<Song>()
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")

            for (attempt in 0..1) {
                try {
                    val cid = getClientId()
                    val url =
                        "https://api-v2.soundcloud.com/search/tracks?q=$encoded&client_id=$cid&limit=$limit&offset=0"
                    val conn = openConnection(url)
                    val code = conn.responseCode

                    if (code == 401 && attempt == 0) {
                        conn.disconnect()
                        refreshClientIdIfNeeded()
                        continue
                    }

                    if (code in 200..299) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        conn.disconnect()
                        val json = JSONObject(body)
                        val collection = json.optJSONArray("collection") ?: JSONArray()
                        for (i in 0 until collection.length()) {
                            val item = collection.optJSONObject(i) ?: continue
                            val track = item.optJSONObject("track") ?: item
                            parseSongFromJson(track)?.let {
                                if (it.durationMs < 600_000L) {
                                    songs.add(it)
                                }
                            }
                        }
                        return@withContext songs
                    } else {
                        conn.disconnect()
                    }
                } catch (_: Exception) {}
            }
            songs
        }

    suspend fun fetchCharts(limit: Int = 30): List<Song> =
        withContext(Dispatchers.IO) {
            val queries =
                listOf("phonk", "trending", "remix", "hip hop", "bass", "electronic", "rock")
            val combined = mutableListOf<Song>()
            val seenIds = mutableSetOf<String>()
            for (q in queries) {
                val results = searchSoundCloud(q, limit = 15)
                for (song in results) {
                    if (seenIds.add(song.id)) {
                        combined.add(song)
                        if (combined.size >= limit) return@withContext combined
                    }
                }
            }
            combined
        }

    suspend fun fetchTrending(limit: Int = 30): List<Song> =
        withContext(Dispatchers.IO) {
            val queries = listOf("phonk", "dancehall", "trance", "reggae", "electronic")
            val combined = mutableListOf<Song>()
            val seenIds = mutableSetOf<String>()
            for (q in queries) {
                val results = searchSoundCloud(q, limit = 15)
                for (song in results) {
                    if (seenIds.add(song.id)) {
                        combined.add(song)
                        if (combined.size >= limit) return@withContext combined
                    }
                }
            }
            combined
        }

    suspend fun fetchUserLikes(limit: Int = 30): List<Song> =
        withContext(Dispatchers.IO) {
            val token = currentAuthToken ?: return@withContext emptyList()
            val songs = mutableListOf<Song>()
            try {
                val cid = getClientId()
                val url =
                    "https://api-v2.soundcloud.com/me/likes/tracks?client_id=$cid&limit=$limit"
                val conn = openConnection(url, includeAuth = true)
                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    val json = JSONObject(body)
                    val collection = json.optJSONArray("collection") ?: JSONArray()
                    for (i in 0 until collection.length()) {
                        val item = collection.optJSONObject(i) ?: continue
                        val track = item.optJSONObject("track") ?: item
                        parseSongFromJson(track)?.let {
                            if (it.durationMs < 600_000L) songs.add(it)
                        }
                    }
                    return@withContext songs
                }
                conn.disconnect()
            } catch (_: Exception) {}
            songs
        }

    suspend fun fetchUserStream(limit: Int = 30): List<Song> =
        withContext(Dispatchers.IO) {
            val token = currentAuthToken ?: return@withContext emptyList()
            val songs = mutableListOf<Song>()
            try {
                val cid = getClientId()
                val url = "https://api-v2.soundcloud.com/stream?client_id=$cid&limit=$limit"
                val conn = openConnection(url, includeAuth = true)
                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    val json = JSONObject(body)
                    val collection = json.optJSONArray("collection") ?: JSONArray()
                    for (i in 0 until collection.length()) {
                        val item = collection.optJSONObject(i) ?: continue
                        val track = item.optJSONObject("track") ?: item
                        parseSongFromJson(track)?.let {
                            if (it.durationMs < 600_000L) songs.add(it)
                        }
                    }
                    return@withContext songs
                }
                conn.disconnect()
            } catch (_: Exception) {}
            songs
        }

    suspend fun fetchUserProfile(authToken: String): SoundCloudUserProfile? =
        withContext(Dispatchers.IO) {
            val clean = cleanToken(authToken)
            if (clean.isBlank() || clean == "guest") return@withContext null
            for (attempt in 0..1) {
                try {
                    val cid = if (attempt == 0) getClientId() else refreshClientIdIfNeeded()
                    val url =
                        if (cid.isNotBlank()) {
                            "https://api-v2.soundcloud.com/me?client_id=$cid"
                        } else {
                            "https://api-v2.soundcloud.com/me"
                        }
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", USER_AGENT)
                    conn.setRequestProperty("Authorization", "OAuth $clean")
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000

                    if (conn.responseCode in 200..299) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        conn.disconnect()
                        val obj = JSONObject(body)
                        val username =
                            obj.optString("username", "").ifBlank {
                                obj.optString("full_name", "").ifBlank { "SoundCloud User" }
                            }
                        val rawAvatar =
                            obj.optString("avatar_url", "").ifBlank {
                                obj.optString("avatar_url_template", "")
                            }
                        val avatar =
                            if (rawAvatar.isNotBlank()) rawAvatar.replace("-large", "-t500x500")
                            else null
                        val permalink =
                            obj.optString("permalink_url", "").takeIf { it.isNotBlank() }
                        val trackCount = obj.optInt("track_count", 0)
                        val likesCount =
                            obj.optInt("public_favorites_count", obj.optInt("likes_count", 0))
                        val followersCount = obj.optInt("followers_count", 0)
                        val followingsCount = obj.optInt("followings_count", 0)

                        val profile =
                            SoundCloudUserProfile(
                                username = username,
                                avatarUrl = avatar,
                                permalinkUrl = permalink,
                                trackCount = trackCount,
                                likesCount = likesCount,
                                followersCount = followersCount,
                                followingsCount = followingsCount,
                            )
                        currentProfile = profile
                        currentAuthToken = clean
                        return@withContext profile
                    }
                    conn.disconnect()
                } catch (_: Exception) {}
            }
            null
        }

    fun resolveStreamUriForExoPlayer(uri: java.net.URI): java.net.URI {
        val scheme = uri.scheme ?: return uri
        if (scheme != "sc") return uri

        val trackId =
            uri.schemeSpecificPart
                .removePrefix("//")
                .substringBefore(".")
                .substringBefore("?")
                .trim()
        if (trackId.isBlank()) return uri

        try {
            val cid = getClientId()
            val candidateTranscodings = mutableListOf<String>()

            val cached = transcodingsCache[trackId]
            if (!cached.isNullOrBlank()) {
                candidateTranscodings.add(cached)
            }

            val trackMetaUrl = "https://api-v2.soundcloud.com/tracks/$trackId?client_id=$cid"
            val conn = openConnection(trackMetaUrl, includeAuth = false)
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val obj = JSONObject(body)
                val media = obj.optJSONObject("media")
                val transcodings = media?.optJSONArray("transcodings")
                if (transcodings != null) {
                    for (i in 0 until transcodings.length()) {
                        val tc = transcodings.optJSONObject(i) ?: continue
                        val format = tc.optJSONObject("format")
                        val protocol = format?.optString("protocol", "") ?: ""
                        val tcUrl = tc.optString("url", "")
                        if (tcUrl.isNotBlank() && !candidateTranscodings.contains(tcUrl)) {
                            if (protocol == "progressive") {
                                candidateTranscodings.add(0, tcUrl)
                            } else {
                                candidateTranscodings.add(tcUrl)
                            }
                        }
                    }
                }
            } else {
                conn.disconnect()
            }

            for (tcUrl in candidateTranscodings) {
                val sep = if (tcUrl.contains("?")) "&" else "?"
                val finalStreamMetaUrl = "$tcUrl${sep}client_id=$cid"
                val sConn = openConnection(finalStreamMetaUrl, includeAuth = false)
                if (sConn.responseCode in 200..299) {
                    val sBody = sConn.inputStream.bufferedReader().use { it.readText() }
                    sConn.disconnect()
                    val streamObj = JSONObject(sBody)
                    val directUrl = streamObj.optString("url", "")
                    if (directUrl.isNotBlank()) {
                        transcodingsCache[trackId] = tcUrl
                        return java.net.URI.create(directUrl)
                    }
                } else {
                    sConn.disconnect()
                }
            }
        } catch (_: Exception) {}

        return uri
    }

    fun getShareUrl(song: Song): String? {
        if (!song.isRemoteSc && !song.id.startsWith("sc_") && !song.uri.startsWith("sc://"))
            return null
        val cached = trackPermalinkMap[song.id] ?: trackPermalinkMap[song.id.removePrefix("sc_")]
        if (!cached.isNullOrBlank()) return cached
        val cleanArtist = song.artist.trim()
        val cleanTitle = song.title.trim()
        return try {
            val q = URLEncoder.encode("$cleanArtist $cleanTitle", "UTF-8")
            "https://soundcloud.com/search?q=$q"
        } catch (_: Exception) {
            "https://soundcloud.com"
        }
    }
}
