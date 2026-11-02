package com.lormusic.app.data

import android.content.Context
import android.util.Base64
import android.util.LruCache
import com.lormusic.app.model.LyricLine
import com.lormusic.app.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

object LyricsRepository {

    private val memoryCache = LruCache<String, List<LyricLine>>(120)

    private val lrcLineRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?](.*)""")
    private val inlineTimestampRegex = Regex("""<\d{1,2}:\d{2}(?:[.:]\d{1,3})?>""")
    private val bracketsCleanupRegex = Regex("""\s*[\(\[][^)\]]*(?:official|video|audio|lyrics|lyric|visualizer|mv|hd|hq|4k|remaster|prod\.|feat\.|ft\.)[^)\]]*[\)\]]""", RegexOption.IGNORE_CASE)

    private val lyricsPlusServers = listOf(
        "https://lyricsplus.prjktla.my.id",
        "https://lyricsplus.atomix.one",
        "https://lyricsplus.binimum.org",
        "https://lyricsplus.prjktla.workers.dev",
        "https://lyricsplus-seven.vercel.app",
        "https://lyrics-plus-backend.vercel.app"
    )

    suspend fun getLyricsForSong(context: Context, song: Song): List<LyricLine> = withContext(Dispatchers.IO) {
        if (!song.customLyrics.isNullOrBlank()) {
            val parsed = parseRawToLines(song.customLyrics)
            if (parsed.isNotEmpty()) return@withContext parsed
        }

        val cacheKey = "${song.artist.lowercase().trim()}|${song.title.lowercase().trim()}|${song.durationMs}"
        memoryCache.get(cacheKey)?.let { cached ->
            if (cached.isNotEmpty()) return@withContext cached
        }

        val localLrc = findSidecarLrcFile(song)
        if (!localLrc.isNullOrBlank()) {
            val parsed = parseRawToLines(localLrc)
            if (parsed.isNotEmpty()) {
                memoryCache.put(cacheKey, parsed)
                return@withContext parsed
            }
        }

        val remoteLines = fetchMultiSourceLyrics(song)
        if (remoteLines.isNotEmpty()) {
            memoryCache.put(cacheKey, remoteLines)
            return@withContext remoteLines
        }

        emptyList()
    }

    fun clearCacheForSong(song: Song) {
        val cacheKey = "${song.artist.lowercase().trim()}|${song.title.lowercase().trim()}|${song.durationMs}"
        memoryCache.remove(cacheKey)
    }

    private fun findSidecarLrcFile(song: Song): String? {
        val path = song.filePath ?: return null
        return runCatching {
            val audioFile = File(path)
            if (!audioFile.exists()) return null
            val baseName = audioFile.nameWithoutExtension
            val parent = audioFile.parentFile ?: return null
            val lrcFile = File(parent, "$baseName.lrc")
            if (lrcFile.exists() && lrcFile.canRead()) {
                lrcFile.readText()
            } else {
                null
            }
        }.getOrNull()
    }

    private fun cleanQueryTitle(raw: String): String {
        return raw
            .replace(bracketsCleanupRegex, "")
            .replace(Regex("""\s*\((?:feat\.?|ft\.?)[^)]*\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\[(?:feat\.?|ft\.?)[^\]]*]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+(?:feat\.?|ft\.?)\s+.*$""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun cleanQueryArtist(raw: String): String {
        val lower = raw.lowercase()
        if (lower.contains("unknown") || lower.contains("неизвестн") || lower == "<unknown>") {
            return ""
        }
        return raw
            .replace(Regex("""\s*-\s*Topic$""", RegexOption.IGNORE_CASE), "")
            .split(Regex("""[,;&/]|(?:\s+(?:feat\.?|ft\.?|x|&)\s+)""", RegexOption.IGNORE_CASE))
            .firstOrNull()
            ?.trim() ?: raw.trim()
    }

    private fun extractYtVideoId(song: Song): String? {
        if (song.uri.startsWith("yt://")) {
            return song.uri.removePrefix("yt://").trim().takeIf { it.length == 11 }
        }
        if (song.id.startsWith("yt_v_")) {
            return song.id.removePrefix("yt_v_").trim().takeIf { it.length == 11 }
        }
        if (song.id.startsWith("yt_")) {
            return song.id.removePrefix("yt_").trim().takeIf { it.length == 11 }
        }
        return null
    }

    private suspend fun fetchMultiSourceLyrics(song: Song): List<LyricLine> = coroutineScope {
        val rawTitle = song.title.trim()
        val rawArtist = song.artist.trim()
        val cleanTitle = cleanQueryTitle(rawTitle)
        val cleanArtist = cleanQueryArtist(rawArtist)
        val durationSec = (song.durationMs / 1000L).toInt().coerceAtLeast(0)
        val videoId = extractYtVideoId(song)

        val candidates = mutableListOf<Pair<String, String>>()
        if (rawTitle.isNotBlank()) candidates.add(rawTitle to rawArtist)
        if (cleanTitle.isNotBlank() && (cleanTitle != rawTitle || cleanArtist != rawArtist)) {
            candidates.add(cleanTitle to cleanArtist)
        }
        if (rawTitle.contains(" - ")) {
            val parts = rawTitle.split(" - ", limit = 2)
            val pArtist = cleanQueryArtist(parts[0])
            val pTitle = cleanQueryTitle(parts[1])
            if (pTitle.isNotBlank()) {
                candidates.add(pTitle to pArtist)
            }
        }

        val mainTitle = cleanTitle.ifBlank { rawTitle }
        val mainArtist = cleanArtist.ifBlank { rawArtist }
        val plainFallbacks = mutableListOf<List<LyricLine>>()

        if (videoId != null) {
            val simp = querySimpMusicByVideoId(videoId)
            if (simp.isNotEmpty()) {
                if (simp.any { it.timeStampMs > 0L }) return@coroutineScope simp
                plainFallbacks.add(simp)
            }
        }

        for ((t, a) in candidates.distinct()) {
            val lrclibDirect = queryLrclibGet(t, a, song.album, durationSec)
            if (lrclibDirect.isNotEmpty()) {
                if (lrclibDirect.any { it.timeStampMs > 0L }) return@coroutineScope lrclibDirect
                plainFallbacks.add(lrclibDirect)
            }
        }

        for ((t, a) in candidates.distinct()) {
            val lrclibSearch = queryLrclibSearch(t, a, durationSec)
            if (lrclibSearch.isNotEmpty()) {
                if (lrclibSearch.any { it.timeStampMs > 0L }) return@coroutineScope lrclibSearch
                plainFallbacks.add(lrclibSearch)
            }
        }

        val parallelSyncedResults = listOf(
            async { queryBetterLyrics(mainTitle, mainArtist, song.album, durationSec) },
            async { queryLyricsPlus(mainTitle, mainArtist, song.album, durationSec) },
            async { queryPaxSenix(mainTitle, mainArtist, song.album, durationSec) },
            async { queryNetEase(mainTitle, mainArtist, durationSec) },
            async { queryKuGou(mainTitle, mainArtist, durationSec) },
            async { queryLrcRed(mainTitle, mainArtist) }
        ).awaitAll()

        for (res in parallelSyncedResults) {
            if (res.isNotEmpty() && res.any { it.timeStampMs > 0L }) {
                return@coroutineScope res
            }
            if (res.isNotEmpty()) {
                plainFallbacks.add(res)
            }
        }

        val megalobiz = queryMegalobiz(mainTitle, mainArtist)
        if (megalobiz.isNotEmpty() && megalobiz.any { it.timeStampMs > 0L }) {
            return@coroutineScope megalobiz
        }

        if (plainFallbacks.isNotEmpty()) {
            return@coroutineScope plainFallbacks.first()
        }

        val ovh = queryLyricsOvh(mainTitle, mainArtist)
        if (ovh.isNotEmpty()) return@coroutineScope ovh

        val genius = queryGenius(mainTitle, mainArtist)
        if (genius.isNotEmpty()) return@coroutineScope genius

        emptyList()
    }

    private fun querySimpMusicByVideoId(videoId: String): List<LyricLine> {
        return runCatching {
            val url = URL("https://api-lyrics.simpmusic.org/v1/$videoId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
                setRequestProperty("User-Agent", "SimpMusicLyrics/1.0")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode != 200) return emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            if (!json.optBoolean("success", false)) return emptyList()
            val arr = json.optJSONArray("data") ?: return emptyList()
            var bestPlain: List<LyricLine> = emptyList()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val synced = item.optString("syncedLyrics", "").takeIf { it != "null" && it.isNotBlank() }
                if (!synced.isNullOrBlank()) {
                    val parsed = parseRawToLines(synced)
                    if (parsed.isNotEmpty()) return parsed
                }
                val rich = item.optString("richSyncLyrics", "").takeIf { it != "null" && it.isNotBlank() }
                if (!rich.isNullOrBlank()) {
                    val parsed = parseRawToLines(rich)
                    if (parsed.isNotEmpty()) return parsed
                }
                val plain = item.optString("plainLyrics", "").takeIf { it != "null" && it.isNotBlank() }
                if (!plain.isNullOrBlank() && bestPlain.isEmpty()) {
                    bestPlain = parseRawToLines(plain)
                }
            }
            bestPlain
        }.getOrDefault(emptyList())
    }

    private fun queryLrclibGet(title: String, artist: String, album: String, durationSec: Int): List<LyricLine> {
        if (title.isBlank() || artist.isBlank()) return emptyList()
        return runCatching {
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val encArtist = URLEncoder.encode(artist, "UTF-8")
            val encAlbum = URLEncoder.encode(album, "UTF-8")
            val durParam = if (durationSec > 0) "&duration=$durationSec" else ""
            val url = URL("https://lrclib.net/api/get?track_name=$encTitle&artist_name=$encArtist&album_name=$encAlbum$durParam")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "LorMusic/1.7.1 (https://github.com/Lorvance/LorMusic)")
            }
            if (conn.responseCode != 200) return emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val synced = json.optString("syncedLyrics", "").takeIf { it != "null" && it.isNotBlank() }
            val plain = json.optString("plainLyrics", "").takeIf { it != "null" && it.isNotBlank() }
            when {
                !synced.isNullOrBlank() -> parseRawToLines(synced)
                !plain.isNullOrBlank() -> parseRawToLines(plain)
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
    }

    private fun queryLrclibSearch(title: String, artist: String, durationSec: Int): List<LyricLine> {
        if (title.isBlank()) return emptyList()
        return runCatching {
            val q = if (artist.isNotBlank()) "$artist $title" else title
            val encQ = URLEncoder.encode(q, "UTF-8")
            val url = URL("https://lrclib.net/api/search?q=$encQ")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5500
                readTimeout = 5500
                setRequestProperty("User-Agent", "LorMusic/1.7.1 (https://github.com/Lorvance/LorMusic)")
            }
            if (conn.responseCode != 200) return emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(body)
            if (arr.length() == 0) return emptyList()

            var bestSynced: String? = null
            var bestPlain: String? = null
            var bestScore = Int.MAX_VALUE

            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val synced = item.optString("syncedLyrics", "").takeIf { it != "null" && it.isNotBlank() }
                val plain = item.optString("plainLyrics", "").takeIf { it != "null" && it.isNotBlank() }
                val itemDur = item.optDouble("duration", 0.0).toInt()
                val diff = if (durationSec > 0 && itemDur > 0) abs(itemDur - durationSec) else 10

                if (!synced.isNullOrBlank() && diff < bestScore) {
                    bestScore = diff
                    bestSynced = synced
                }
                if (bestPlain == null && !plain.isNullOrBlank()) {
                    bestPlain = plain
                }
            }

            when {
                !bestSynced.isNullOrBlank() -> parseRawToLines(bestSynced)
                !bestPlain.isNullOrBlank() -> parseRawToLines(bestPlain)
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
    }

    private fun queryBetterLyrics(title: String, artist: String, album: String, durationSec: Int): List<LyricLine> {
        if (title.isBlank() || artist.isBlank()) return emptyList()
        for (endpoint in listOf("/getLyrics", "/qq/getLyrics")) {
            val res = runCatching {
                val sb = StringBuilder("https://lyrics-api.boidu.dev$endpoint?s=${URLEncoder.encode(title, "UTF-8")}&a=${URLEncoder.encode(artist, "UTF-8")}")
                if (durationSec > 0) sb.append("&d=$durationSec")
                if (album.isNotBlank() && !album.equals("YouTube Music", ignoreCase = true)) {
                    sb.append("&al=${URLEncoder.encode(album, "UTF-8")}")
                }
                val conn = (URL(sb.toString()).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    setRequestProperty("Accept", "application/json")
                }
                if (conn.responseCode != 200) return@runCatching emptyList<LyricLine>()
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val ttml = json.optString("ttml", "").takeIf { it != "null" && it.isNotBlank() }
                val lrc = json.optString("lrc", "").takeIf { it != "null" && it.isNotBlank() }
                    ?: json.optString("lyrics", "").takeIf { it != "null" && it.isNotBlank() }
                when {
                    !ttml.isNullOrBlank() -> {
                        if (ttml.trimStart().startsWith("<")) parseTtmlToLines(ttml) else parseRawToLines(ttml)
                    }
                    !lrc.isNullOrBlank() -> {
                        if (lrc.trimStart().startsWith("<")) parseTtmlToLines(lrc) else parseRawToLines(lrc)
                    }
                    else -> emptyList()
                }
            }.getOrDefault(emptyList())
            if (res.isNotEmpty()) return res
        }
        return emptyList()
    }

    private fun queryLyricsPlus(title: String, artist: String, album: String, durationSec: Int): List<LyricLine> {
        if (title.isBlank() || artist.isBlank()) return emptyList()
        for (base in lyricsPlusServers) {
            val res = runCatching {
                val sb = StringBuilder("$base/v2/lyrics/get?title=${URLEncoder.encode(title, "UTF-8")}&artist=${URLEncoder.encode(artist, "UTF-8")}")
                if (durationSec > 0) sb.append("&duration=$durationSec")
                if (album.isNotBlank() && !album.equals("YouTube Music", ignoreCase = true)) {
                    sb.append("&album=${URLEncoder.encode(album, "UTF-8")}")
                }
                sb.append("&source=apple,lyricsplus,musixmatch,spotify,musixmatch-word")
                val conn = (URL(sb.toString()).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4500
                    readTimeout = 4500
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    setRequestProperty("Accept", "application/json")
                }
                if (conn.responseCode != 200) return@runCatching emptyList<LyricLine>()
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val lyricsArr = json.optJSONArray("lyrics") ?: return@runCatching emptyList<LyricLine>()
                val out = mutableListOf<LyricLine>()
                for (i in 0 until lyricsArr.length()) {
                    val obj = lyricsArr.optJSONObject(i) ?: continue
                    val timeMs = obj.optLong("time", -1L)
                    val text = obj.optString("text", "").trim()
                    if (timeMs >= 0L && text.isNotBlank()) {
                        out.add(LyricLine(timeStampMs = timeMs, content = text))
                    }
                }
                out.sortedBy { it.timeStampMs }
            }.getOrDefault(emptyList())
            if (res.isNotEmpty()) return res
        }
        return emptyList()
    }

    private fun queryPaxSenix(title: String, artist: String, album: String, durationSec: Int): List<LyricLine> {
        if (title.isBlank() || artist.isBlank()) return emptyList()
        val encT = URLEncoder.encode(title, "UTF-8")
        val encA = URLEncoder.encode(artist, "UTF-8")
        val durParam = if (durationSec > 0) "&d=$durationSec" else ""
        val urls = listOf(
            "https://lyrics.paxsenix.org/apple-music/lyrics?t=$encT&a=$encA$durParam",
            "https://lyrics.paxsenix.org/musixmatch/lyrics?t=$encT&a=$encA$durParam",
            "https://lyrics.paxsenix.org/musixmatch/lyrics-search?t=$encT&a=$encA$durParam"
        )
        for (u in urls) {
            val res = runCatching {
                val conn = (URL(u).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4500
                    readTimeout = 4500
                    setRequestProperty("User-Agent", "LorMusic/1.7.1")
                    setRequestProperty("Accept", "application/json")
                }
                if (conn.responseCode != 200) return@runCatching emptyList<LyricLine>()
                val body = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                if (body.startsWith("{")) {
                    val json = JSONObject(body)
                    val lrc = json.optString("lrc", "").takeIf { it != "null" && it.isNotBlank() }
                        ?: json.optString("syncedLyrics", "").takeIf { it != "null" && it.isNotBlank() }
                        ?: json.optString("lyrics", "").takeIf { it != "null" && it.isNotBlank() }
                        ?: json.optString("plain", "").takeIf { it != "null" && it.isNotBlank() }
                    if (!lrc.isNullOrBlank()) return@runCatching parseRawToLines(lrc)
                }
                emptyList<LyricLine>()
            }.getOrDefault(emptyList())
            if (res.isNotEmpty()) return res
        }
        return emptyList()
    }

    private fun queryNetEase(title: String, artist: String, durationSec: Int): List<LyricLine> {
        if (title.isBlank()) return emptyList()
        return runCatching {
            val q = if (artist.isNotBlank()) "$title $artist" else title
            val searchUrl = URL("https://music.163.com/api/search/get")
            val postBody = "s=${URLEncoder.encode(q, "UTF-8")}&type=1&offset=0&limit=5"
            val searchConn = (searchUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Referer", "https://music.163.com")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }
            searchConn.outputStream.use { it.write(postBody.toByteArray(Charsets.UTF_8)) }
            if (searchConn.responseCode != 200) return emptyList()
            val searchResp = JSONObject(searchConn.inputStream.bufferedReader().use { it.readText() })
            val songs = searchResp.optJSONObject("result")?.optJSONArray("songs") ?: return emptyList()
            if (songs.length() == 0) return emptyList()

            var chosenSongId = songs.getJSONObject(0).optLong("id", 0L)
            for (i in 0 until songs.length()) {
                val sObj = songs.getJSONObject(i)
                val sDurSec = (sObj.optLong("duration", 0L) / 1000L).toInt()
                if (durationSec > 0 && abs(sDurSec - durationSec) <= 6) {
                    chosenSongId = sObj.optLong("id", chosenSongId)
                    break
                }
            }
            if (chosenSongId == 0L) return emptyList()

            val lyricUrl = URL("https://music.163.com/api/song/lyric?id=$chosenSongId&lv=1&kv=1&tv=-1")
            val lyricConn = (lyricUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Referer", "https://music.163.com")
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (lyricConn.responseCode != 200) return emptyList()
            val lyricJson = JSONObject(lyricConn.inputStream.bufferedReader().use { it.readText() })
            val lrcText = lyricJson.optJSONObject("lrc")?.optString("lyric", "") ?: ""
            if (lrcText.isNotBlank()) parseRawToLines(lrcText) else emptyList()
        }.getOrDefault(emptyList())
    }

    private fun queryKuGou(title: String, artist: String, durationSec: Int): List<LyricLine> {
        if (title.isBlank()) return emptyList()
        return runCatching {
            val keyword = if (artist.isNotBlank()) "$artist - $title" else title
            val encKeyword = URLEncoder.encode(keyword, "UTF-8")
            val searchUrl = URL("https://mobileservice.kugou.com/api/v3/search/song?version=9108&plat=0&pagesize=8&showtype=0&keyword=$encKeyword")
            val searchConn = (searchUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (searchConn.responseCode != 200) return emptyList()
            val searchJson = JSONObject(searchConn.inputStream.bufferedReader().use { it.readText() })
            val infoArr = searchJson.optJSONObject("data")?.optJSONArray("info") ?: return emptyList()
            if (infoArr.length() == 0) return emptyList()

            var bestHash = ""
            var bestDurMs = if (durationSec > 0) durationSec * 1000L else 180000L
            for (i in 0 until infoArr.length()) {
                val item = infoArr.getJSONObject(i)
                val hash = item.optString("hash", "")
                val itemDurSec = item.optInt("duration", 0)
                if (hash.isNotBlank()) {
                    if (bestHash.isEmpty()) {
                        bestHash = hash
                        if (itemDurSec > 0) bestDurMs = itemDurSec * 1000L
                    }
                    if (durationSec > 0 && itemDurSec > 0 && abs(itemDurSec - durationSec) <= 8) {
                        bestHash = hash
                        bestDurMs = itemDurSec * 1000L
                        break
                    }
                }
            }
            if (bestHash.isBlank()) return emptyList()

            val candidatesUrl = URL("https://lyrics.kugou.com/search?ver=1&man=yes&client=pc&keyword=$encKeyword&duration=$bestDurMs&hash=$bestHash")
            val candConn = (candidatesUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (candConn.responseCode != 200) return emptyList()
            val candJson = JSONObject(candConn.inputStream.bufferedReader().use { it.readText() })
            val candidates = candJson.optJSONArray("candidates") ?: return emptyList()
            if (candidates.length() == 0) return emptyList()

            val firstCand = candidates.getJSONObject(0)
            val candId = firstCand.optString("id", "")
            val accessKey = firstCand.optString("accesskey", "")
            if (candId.isBlank() || accessKey.isBlank()) return emptyList()

            val dlUrl = URL("https://lyrics.kugou.com/download?ver=1&client=pc&id=$candId&accesskey=$accessKey&fmt=lrc&charset=utf8")
            val dlConn = (dlUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (dlConn.responseCode != 200) return emptyList()
            val dlJson = JSONObject(dlConn.inputStream.bufferedReader().use { it.readText() })
            val b64 = dlJson.optString("content", "")
            if (b64.isBlank()) return emptyList()
            val decoded = String(Base64.decode(b64, Base64.DEFAULT), Charsets.UTF_8)
            parseRawToLines(decoded)
        }.getOrDefault(emptyList())
    }

    private fun queryLrcRed(title: String, artist: String): List<LyricLine> {
        if (title.isBlank() || artist.isBlank()) return emptyList()
        return runCatching {
            val q = URLEncoder.encode("$title $artist", "UTF-8")
            val searchConn = (URL("https://lrc.red/search.json?q=$q").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
                setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                setRequestProperty("Referer", "https://lrc.red/")
            }
            if (searchConn.responseCode != 200) return emptyList()
            val searchBody = searchConn.inputStream.bufferedReader().use { it.readText() }.trim()
            val items = if (searchBody.startsWith("[")) {
                JSONArray(searchBody)
            } else {
                val obj = JSONObject(searchBody)
                obj.optJSONArray("results") ?: obj.optJSONArray("data") ?: obj.optJSONArray("tracks") ?: return emptyList()
            }
            if (items.length() == 0) return emptyList()
            val first = items.optJSONObject(0) ?: return emptyList()
            val isrc = first.optString("isrc", "").ifBlank { first.optString("id", "") }
            if (isrc.isBlank()) return emptyList()

            val ttmlConn = (URL("https://lrc.red/s/$isrc.ttml").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Referer", "https://lrc.red/")
            }
            if (ttmlConn.responseCode != 200) return emptyList()
            val ttml = ttmlConn.inputStream.bufferedReader().use { it.readText() }
            if (ttml.contains("<p")) parseTtmlToLines(ttml) else parseRawToLines(ttml)
        }.getOrDefault(emptyList())
    }

    private fun queryMegalobiz(title: String, artist: String): List<LyricLine> {
        if (title.isBlank()) return emptyList()
        return runCatching {
            val q = URLEncoder.encode(if (artist.isNotBlank()) "$artist $title" else title, "UTF-8")
            val searchConn = (URL("https://www.megalobiz.com/search/all?qry=$q").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36")
            }
            if (searchConn.responseCode != 200) return emptyList()
            val html = searchConn.inputStream.bufferedReader().use { it.readText() }
            val linkRegex = Regex("""href="(/lrc/maker/[^"]+)"""")
            val match = linkRegex.find(html) ?: return emptyList()
            val pagePath = match.groupValues[1]

            val pageConn = (URL("https://www.megalobiz.com$pagePath").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36")
            }
            if (pageConn.responseCode != 200) return emptyList()
            val pageHtml = pageConn.inputStream.bufferedReader().use { it.readText() }
            val spanRegex = Regex("""<span id="lrc_\d+_lyrics"[^>]*>(.*?)</span>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            val spanMatch = spanRegex.find(pageHtml) ?: return emptyList()
            val rawLrc = decodeHtmlEntities(spanMatch.groupValues[1].replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n").replace(Regex("<[^>]+>"), ""))
            parseRawToLines(rawLrc)
        }.getOrDefault(emptyList())
    }

    private fun queryLyricsOvh(title: String, artist: String): List<LyricLine> {
        if (title.isBlank() || artist.isBlank()) return emptyList()
        return runCatching {
            val encArtist = URLEncoder.encode(artist, "UTF-8").replace("+", "%20")
            val encTitle = URLEncoder.encode(title, "UTF-8").replace("+", "%20")
            val url = URL("https://api.lyrics.ovh/v1/$encArtist/$encTitle")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
                setRequestProperty("User-Agent", "LorMusic/1.7.1")
            }
            if (conn.responseCode != 200) return emptyList()
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val lyrics = json.optString("lyrics", "").trim()
            if (lyrics.isNotBlank()) parseRawToLines(lyrics) else emptyList()
        }.getOrDefault(emptyList())
    }

    private fun queryGenius(title: String, artist: String): List<LyricLine> {
        if (title.isBlank()) return emptyList()
        return runCatching {
            val q = URLEncoder.encode(if (artist.isNotBlank()) "$artist $title" else title, "UTF-8")
            val searchConn = (URL("https://genius.com/api/search/multi?q=$q").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36")
                setRequestProperty("Accept", "application/json")
            }
            if (searchConn.responseCode != 200) return emptyList()
            val searchJson = JSONObject(searchConn.inputStream.bufferedReader().use { it.readText() })
            val sections = searchJson.optJSONObject("response")?.optJSONArray("sections") ?: return emptyList()
            var songPath: String? = null
            for (s in 0 until sections.length()) {
                val sec = sections.getJSONObject(s)
                val type = sec.optString("type", "")
                if (type == "song" || type == "top_hit") {
                    val hits = sec.optJSONArray("hits") ?: continue
                    for (h in 0 until hits.length()) {
                        val hit = hits.getJSONObject(h)
                        if (hit.optString("type") == "song") {
                            val res = hit.optJSONObject("result") ?: continue
                            val path = res.optString("path", "")
                            if (path.isNotBlank() && !path.endsWith("-annotated")) {
                                songPath = path
                                break
                            }
                        }
                    }
                }
                if (songPath != null) break
            }
            val finalPath = songPath ?: return emptyList()
            val pageConn = (URL("https://genius.com$finalPath").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36")
            }
            if (pageConn.responseCode != 200) return emptyList()
            val html = pageConn.inputStream.bufferedReader().use { it.readText() }
            val containerRegex = Regex("""<div[^>]*data-lyrics-container="true"[^>]*>(.*?)</div>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            val matches = containerRegex.findAll(html).toList()
            if (matches.isEmpty()) return emptyList()
            val rawBuilder = StringBuilder()
            for (m in matches) {
                val block = m.groupValues[1]
                    .replace(Regex("""<div[^>]*data-exclude-from-selection="true"[^>]*>.*?</div>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
                    .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
                    .replace(Regex("""<[^>]+>"""), "")
                rawBuilder.append(decodeHtmlEntities(block)).append("\n")
            }
            val plainText = rawBuilder.toString().lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("[") }
                .joinToString("\n")
            parseRawToLines(plainText)
        }.getOrDefault(emptyList())
    }

    private fun decodeHtmlEntities(input: String): String {
        return input
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
    }

    private fun parseTtmlTimeMs(raw: String): Long? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        if (s.endsWith("ms")) {
            return s.removeSuffix("ms").toDoubleOrNull()?.toLong()
        }
        if (s.endsWith("s")) {
            val sec = s.removeSuffix("s").toDoubleOrNull() ?: return null
            return (sec * 1000.0).toLong()
        }
        val parts = s.split(":")
        return when (parts.size) {
            3 -> {
                val h = parts[0].toLongOrNull() ?: return null
                val m = parts[1].toLongOrNull() ?: return null
                val sec = parts[2].toDoubleOrNull() ?: return null
                ((h * 3600 + m * 60) * 1000L) + (sec * 1000.0).toLong()
            }
            2 -> {
                val m = parts[0].toLongOrNull() ?: return null
                val sec = parts[1].toDoubleOrNull() ?: return null
                (m * 60 * 1000L) + (sec * 1000.0).toLong()
            }
            else -> {
                val sec = s.toDoubleOrNull() ?: return null
                (sec * 1000.0).toLong()
            }
        }
    }

    fun parseTtmlToLines(ttml: String): List<LyricLine> {
        val pRegex = Regex("""<p\b([^>]*)>(.*?)</p>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val beginRegex = Regex("""begin\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val out = mutableListOf<LyricLine>()
        for (match in pRegex.findAll(ttml)) {
            val attrs = match.groupValues[1]
            val inner = match.groupValues[2]
            val beginStr = beginRegex.find(attrs)?.groupValues?.get(1) ?: continue
            val timeMs = parseTtmlTimeMs(beginStr) ?: continue
            val cleanedInner = inner
                .replace(Regex("""<span[^>]*ttm:role\s*=\s*["']x-translation["'][^>]*>.*?</span>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
                .replace(Regex("""<[^>]+>"""), "")
            val text = decodeHtmlEntities(cleanedInner).replace(Regex("""\s+"""), " ").trim()
            if (text.isNotBlank()) {
                out.add(LyricLine(timeStampMs = timeMs, content = text))
            }
        }
        return out.sortedBy { it.timeStampMs }
    }

    fun parseRawToLines(raw: String): List<LyricLine> {
        val rawLines = raw.lines()
        val synced = mutableListOf<LyricLine>()
        val plain = mutableListOf<String>()

        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val match = lrcLineRegex.matchEntire(trimmed)
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val fracStr = match.groupValues[3]
                val ms = when (fracStr.length) {
                    1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                    2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                    3 -> fracStr.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val totalMs = min * 60_000L + sec * 1_000L + ms
                val text = match.groupValues[4].replace(inlineTimestampRegex, "").trim()
                if (text.isNotEmpty()) {
                    val isMeta = text.startsWith("作词") || text.startsWith("作曲") || text.startsWith("编曲") ||
                        text.startsWith("制作人") || text.startsWith("混音") || text.startsWith("母带")
                    if (!isMeta) {
                        val isInst = text == "♪" || text == "..." || text.equals("[Instrumental]", ignoreCase = true)
                        synced.add(
                            LyricLine(
                                timeStampMs = totalMs,
                                content = text,
                                isInstrumental = isInst
                            )
                        )
                    }
                }
            } else if (!trimmed.startsWith("[ti:") &&
                !trimmed.startsWith("[ar:") &&
                !trimmed.startsWith("[al:") &&
                !trimmed.startsWith("[by:") &&
                !trimmed.startsWith("[offset:") &&
                !trimmed.startsWith("[length:") &&
                !trimmed.startsWith("[id:") &&
                !trimmed.startsWith("[hash:") &&
                !trimmed.startsWith("[total:")
            ) {
                plain.add(trimmed)
            }
        }

        if (synced.isNotEmpty()) {
            return synced.sortedBy { it.timeStampMs }
        }

        if (plain.isEmpty()) return emptyList()
        return plain.map { lineText ->
            LyricLine(
                timeStampMs = 0L,
                content = lineText
            )
        }
    }

    suspend fun translateLyricLines(lines: List<LyricLine>, targetLang: String): List<LyricLine> = withContext(Dispatchers.IO) {
        if (lines.isEmpty()) return@withContext lines
        try {
            val baseLines = lines.filter { !it.isTranslation }
            val result = mutableListOf<LyricLine>()
            val chunkSize = 25
            for (chunk in baseLines.chunked(chunkSize)) {
                val combined = chunk.joinToString("\n") { it.content }
                val encoded = URLEncoder.encode(combined, "UTF-8")
                val url = URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encoded")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 7000
                    readTimeout = 7000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                val translatedLines = if (conn.responseCode in 200..299) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(resp)
                    val sentences = jsonArray.optJSONArray(0)
                    val sb = StringBuilder()
                    if (sentences != null) {
                        for (i in 0 until sentences.length()) {
                            val item = sentences.optJSONArray(i)
                            if (item != null) sb.append(item.optString(0))
                        }
                    }
                    sb.toString().split("\n")
                } else {
                    emptyList()
                }
                for (i in chunk.indices) {
                    val orig = chunk[i]
                    result.add(orig)
                    val tr = translatedLines.getOrNull(i)?.trim()
                    if (!tr.isNullOrBlank() && !tr.equals(orig.content.trim(), ignoreCase = true)) {
                        result.add(
                            LyricLine(
                                timeStampMs = orig.timeStampMs,
                                content = tr,
                                isTranslation = true
                            )
                        )
                    }
                }
            }
            result
        } catch (_: Exception) {
            lines
        }
    }
}
