package com.lormusic.app.data

import android.content.Context
import com.lormusic.app.model.LyricLine
import com.lormusic.app.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.Charset

object LyricsRepository {

    private val demoLrcMap = mapOf(
        "demo_1" to """
            [00:00.00]
            [00:03.50] City lights are blurring on the windshield
            [00:07.50] Neon reflections on the wet asphalt
            [00:11.00]
            [00:14.50] Cruising through the quiet midnight avenues
            [00:18.50] Chasing the horizon till the morning glow
        """.trimIndent(),
        "demo_2" to """
            [00:00.00]
            [00:03.50] Towers made of crystal and electric dreams
            [00:07.50] Shadows dancing underneath the violet sky
            [00:11.50]
            [00:15.00] Lost inside the rhythm of the Glass City
        """.trimIndent(),
        "demo_3" to """
            [00:00.00]
            [00:04.00] When the sun goes down behind the hills
            [00:08.50] Golden embers linger in the air
            [00:13.00]
            [00:17.50] Floating in a gentle afterglow
        """.trimIndent()
    )

    suspend fun getLyricsForSong(context: Context, song: Song): List<LyricLine> = withContext(Dispatchers.IO) {

        if (!song.customLyrics.isNullOrBlank()) {
            val rawCustom = song.customLyrics.trim()
            val parsedLrc = parseLrcString(rawCustom)
            if (parsedLrc.isNotEmpty()) return@withContext parsedLrc
            val plainLines = rawCustom.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (plainLines.isNotEmpty()) {
                val totalMs = song.durationMs.coerceAtLeast(30_000L)
                val stepMs = (totalMs / (plainLines.size + 1)).coerceAtLeast(2000L)
                return@withContext plainLines.mapIndexed { idx, line ->
                    LyricLine(timeStampMs = (idx * stepMs) + 1500L, content = line)
                }
            }
        }

        demoLrcMap[song.id]?.let { lrc ->
            val parsed = parseLrcString(lrc)
            if (parsed.isNotEmpty()) return@withContext parsed
        }

        song.filePath?.let { path ->
            val audioFile = File(path)
            val lrcFile = File(audioFile.parentFile, audioFile.nameWithoutExtension + ".lrc")
            if (lrcFile.exists() && lrcFile.canRead()) {
                val raw = runCatching { lrcFile.readBytes().toString(Charset.defaultCharset()) }.getOrNull()
                if (!raw.isNullOrBlank()) {
                    val parsed = parseLrcString(raw)
                    if (parsed.isNotEmpty()) return@withContext parsed
                }
            }
        }

        val cacheDir = File(context.cacheDir, "lyrics_cache_v2").apply { if (!exists()) mkdirs() }
        val safeKey = (song.artist.trim() + "__" + song.title.trim()).replace(Regex("[^a-zA-Z0-9а-яА-ЯёЁіІїЇєЄ_-]"), "_")
        val cachedFile = File(cacheDir, "$safeKey.lrc")
        if (cachedFile.exists() && cachedFile.length() > 10) {
            val cachedText = runCatching { cachedFile.readText() }.getOrNull()
            if (!cachedText.isNullOrBlank()) {
                val parsed = parseLrcString(cachedText)
                if (parsed.isNotEmpty()) return@withContext parsed
            }
        }

        val (resolvedTitle, resolvedArtist) = resolveTitleAndArtist(song.title, song.artist)
        val fetchedLrc = fetchLrcFromNetwork(resolvedTitle, resolvedArtist, song.durationMs)
            ?: fetchFromLyricsOvh(resolvedTitle, resolvedArtist, song.durationMs)
        if (!fetchedLrc.isNullOrBlank()) {
            runCatching { cachedFile.writeText(fetchedLrc) }
            val parsed = parseLrcString(fetchedLrc)
            if (parsed.isNotEmpty()) return@withContext parsed
        }

        emptyList()
    }

    private fun isUnknownArtistName(artist: String): Boolean {
        val a = artist.trim()
        if (a.isBlank()) return true
        val lower = a.lowercase()
        return lower.contains("неизвестн") ||
            lower.contains("невідом") ||
            lower.contains("unknown") ||
            lower == "<unknown>" ||
            lower == "telegram" ||
            lower == "various artists" ||
            lower == "va"
    }

    private fun cleanQueryTitle(raw: String): String {
        return raw
            .replace(Regex("\\.(mp3|flac|m4a|wav|ogg|aac|opus)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\((?:feat\\.|ft\\.|prod\\.|official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|live|remix).*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[(?:feat\\.|ft\\.|prod\\.|official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|live).*?]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[_^~]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun cleanQueryArtist(raw: String): String {
        return raw
            .replace(Regex("\\s*-\\s*Topic$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bVEVO\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\((?:feat\\.|ft\\.).*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[(?:feat\\.|ft\\.).*?]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[_^~]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun resolveTitleAndArtist(rawTitle: String, rawArtist: String): Pair<String, String> {
        var title = cleanQueryTitle(rawTitle)
        var artist = if (isUnknownArtistName(rawArtist)) "" else cleanQueryArtist(rawArtist)

        val dashSplitRegex = Regex("^(.+?)\\s+[-–—]\\s+(.+)$")
        val match = dashSplitRegex.find(title)
        if (match != null) {
            val left = match.groupValues[1].trim()
            val right = match.groupValues[2].trim()
            if (artist.isBlank()) {
                artist = cleanQueryArtist(left)
                title = cleanQueryTitle(right)
            } else if ( normalizeForCompare(left) == normalizeForCompare(artist) ||
                        normalizeForCompare(artist).contains(normalizeForCompare(left)) ) {
                title = cleanQueryTitle(right)
            } else if ( normalizeForCompare(right) == normalizeForCompare(artist) ) {
                title = cleanQueryTitle(left)
            }
        }

        // Extract feat. from title if present before cleaning
        val featInTitle = Regex("(?:\\(|\\[|\\s)(?:feat\\.|ft\\.)\\s*([^)\\]]+)", RegexOption.IGNORE_CASE).find(rawTitle)
        if (artist.isBlank() && featInTitle != null) {
            artist = cleanQueryArtist(featInTitle.groupValues[1])
        }

        return title to artist
    }

    private fun normalizeForCompare(text: String): String {
        return text.lowercase()
            .replace('ё', 'е')
            .replace(Regex("\\(.*?\\)|\\[.*?]"), " ")
            .replace(Regex("[^a-z0-9а-яіїєґ\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractArtistTokens(artist: String): List<String> {
        return artist
            .split(Regex("(?:,|&|;|/|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bx\\b|\\bи\\b|\\band\\b)", RegexOption.IGNORE_CASE))
            .map { normalizeForCompare(it) }
            .filter { it.length >= 2 }
    }

    private fun isArtistMatching(candidateArtist: String, candidateTitle: String, targetArtist: String): Boolean {
        val normTarget = normalizeForCompare(targetArtist)
        val normCandArtist = normalizeForCompare(candidateArtist)
        val normCandTitle = normalizeForCompare(candidateTitle)
        if (normTarget.isBlank()) return true
        if (normCandArtist.isBlank()) return false

        if (normCandArtist == normTarget ||
            normCandArtist.contains(normTarget) ||
            normTarget.contains(normCandArtist)
        ) {
            return true
        }

        val targetTokens = extractArtistTokens(targetArtist)
        val candTokens = extractArtistTokens(candidateArtist)

        for (t in targetTokens) {
            if (candTokens.any { c -> c == t || c.contains(t) || t.contains(c) }) {
                return true
            }
            if (normCandArtist.contains(t) || normCandTitle.contains(t)) {
                return true
            }
        }
        return false
    }

    private fun isTitleMatching(candidateTitle: String, targetTitle: String): Boolean {
        val normTarget = normalizeForCompare(targetTitle)
        val normCand = normalizeForCompare(candidateTitle)
        if (normTarget.isBlank() || normCand.isBlank()) return false
        if (normCand == normTarget) return true
        if (normCand.contains(normTarget) || normTarget.contains(normCand)) return true

        val targetWords = normTarget.split(" ").filter { it.length >= 2 }
        val candWords = normCand.split(" ").filter { it.length >= 2 }
        if (targetWords.isEmpty() || candWords.isEmpty()) return false
        val overlap = targetWords.count { tw -> candWords.any { cw -> cw == tw } }
        return overlap >= targetWords.size.coerceAtLeast(1)
    }

    private fun fetchLrcFromNetwork(rawTitle: String, rawArtist: String, durationMs: Long): String? {
        val cleanTitle = cleanQueryTitle(rawTitle)
        if (cleanTitle.length < 2) return null

        val cleanArtist = if (isUnknownArtistName(rawArtist)) "" else cleanQueryArtist(rawArtist)
        val hasRealArtist = cleanArtist.isNotBlank()
        val primaryArtist = cleanArtist
            .split(Regex("(?:,|&|;|/|\\bfeat\\.?\\b|\\bft\\.?\\b)", RegexOption.IGNORE_CASE))
            .firstOrNull()
            ?.trim()
            .orEmpty()

        val urlsToTry = buildList {
            if (hasRealArtist) {
                val encTitle = URLEncoder.encode(cleanTitle, "UTF-8")
                val encArtist = URLEncoder.encode(cleanArtist, "UTF-8")
                add("https://lrclib.net/api/search?track_name=$encTitle&artist_name=$encArtist")
                if (primaryArtist.isNotBlank() && !primaryArtist.equals(cleanArtist, ignoreCase = true)) {
                    val encPrimary = URLEncoder.encode(primaryArtist, "UTF-8")
                    add("https://lrclib.net/api/search?track_name=$encTitle&artist_name=$encPrimary")
                }
                val encCombined = URLEncoder.encode("$cleanArtist $cleanTitle", "UTF-8")
                add("https://lrclib.net/api/search?q=$encCombined")
            } else {
                val encTitle = URLEncoder.encode(cleanTitle, "UTF-8")
                add("https://lrclib.net/api/search?q=$encTitle")
            }
        }

        val songDurSec = if (durationMs > 0L) (durationMs / 1000L).toDouble() else 0.0

        data class ScoredCandidate(
            val score: Int,
            val synced: String?,
            val plain: String?
        )

        val validCandidates = mutableListOf<ScoredCandidate>()

        for (urlStr in urlsToTry) {
            try {
                val url = URL(urlStr)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "LorMusic/1.6.4.2 (Android; Media3)")
                }
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONArray(response)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val candTitle = obj.optString("trackName", "").ifBlank { obj.optString("name", "") }
                        val candArtist = obj.optString("artistName", "")
                        val candDurSec = obj.optDouble("duration", 0.0)
                        val synced = obj.optString("syncedLyrics", "").takeIf { it.isNotBlank() && it != "null" }
                        val plain = obj.optString("plainLyrics", "").takeIf { it.isNotBlank() && it != "null" }

                        if (synced == null && plain == null) continue
                        if (!isTitleMatching(candTitle, cleanTitle)) continue

                        if (hasRealArtist) {
                            if (!isArtistMatching(candArtist, candTitle, cleanArtist)) {
                                continue
                            }
                        } else if (songDurSec > 15.0 && candDurSec > 15.0) {
                            // If artist is unknown, require duration to be reasonably close so we don't pick a random song
                            if (kotlin.math.abs(candDurSec - songDurSec) > 20.0) continue
                        }

                        var score = 0
                        if (normalizeForCompare(candTitle) == normalizeForCompare(cleanTitle)) {
                            score += 50
                        } else {
                            score += 20
                        }

                        if (hasRealArtist) {
                            val normCandArtist = normalizeForCompare(candArtist)
                            val normCleanArtist = normalizeForCompare(cleanArtist)
                            if (normCandArtist == normCleanArtist) {
                                score += 60
                            } else if (normCandArtist.contains(normCleanArtist) || normCleanArtist.contains(normCandArtist)) {
                                score += 45
                            } else {
                                score += 25
                            }
                        }

                        if (songDurSec > 10.0 && candDurSec > 10.0) {
                            val diff = kotlin.math.abs(candDurSec - songDurSec)
                            when {
                                diff <= 3.0 -> score += 35
                                diff <= 8.0 -> score += 20
                                diff <= 15.0 -> score += 5
                                diff > 35.0 -> score -= 30
                            }
                        }

                        if (synced != null) score += 40 else score += 10

                        validCandidates.add(ScoredCandidate(score, synced, plain))
                    }
                    if (validCandidates.any { it.synced != null && it.score >= 120 }) {
                        break
                    }
                }
            } catch (_: Exception) {
            }
        }

        val best = validCandidates.maxByOrNull { it.score } ?: return null
        if (!best.synced.isNullOrBlank()) {
            return best.synced
        }
        val bestPlain = best.plain
        if (!bestPlain.isNullOrBlank()) {
            val lines = bestPlain.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isNotEmpty()) {
                val totalMs = durationMs.coerceAtLeast(30_000L)
                val stepMs = (totalMs / (lines.size + 1)).coerceAtLeast(2000L)
                return lines.mapIndexed { idx, line ->
                    val ms = (idx * stepMs) + 2500L
                    val min = (ms / 60000)
                    val sec = (ms % 60000) / 1000
                    val cs = (ms % 1000) / 10
                    "[%02d:%02d.%02d]%s".format(min, sec, cs, line)
                }.joinToString("\n")
            }
        }
        return null
    }

    fun parseLrcString(lrcContent: String): List<LyricLine> {
        val linesRegex = "\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{2,3}))?](.*)".toRegex()
        val rawList = mutableListOf<LyricLine>()

        lrcContent.lines().forEach { line ->
            linesRegex.find(line.trim())?.let { matchResult ->
                val min = matchResult.groupValues[1].toLongOrNull() ?: 0L
                val sec = matchResult.groupValues[2].toLongOrNull() ?: 0L
                val fracRaw = matchResult.groupValues[3]
                val ms = when (fracRaw.length) {
                    2 -> (fracRaw.toLongOrNull() ?: 0L) * 10L
                    3 -> fracRaw.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val startTime = min * 60_000L + sec * 1_000L + ms
                val content = matchResult.groupValues[4].trim()
                val isBreakSymbol = content.isEmpty() ||
                    content == "..." ||
                    content == "…" ||
                    content == "♪" ||
                    content == "♫" ||
                    content.equals("[Instrumental]", ignoreCase = true)

                val insertIndex = rawList.binarySearch { it.timeStampMs.compareTo(startTime) }
                if (isBreakSymbol) {
                    val pos = if (insertIndex < 0) -insertIndex - 1 else insertIndex + 1
                    rawList.add(pos, LyricLine(startTime, content = "", isInstrumental = true))
                } else {
                    if (insertIndex < 0) {
                        rawList.add(-insertIndex - 1, LyricLine(startTime, content, isTranslation = false))
                    } else {
                        rawList.add(insertIndex + 1, LyricLine(startTime, content, isTranslation = true))
                    }
                }
            }
        }

        if (rawList.isEmpty()) return emptyList()

        val enriched = mutableListOf<LyricLine>()
        val firstTime = rawList.first().timeStampMs
        if (firstTime >= 2000L && !rawList.first().isInstrumental) {
            enriched.add(LyricLine(timeStampMs = 0L, content = "", isInstrumental = true))
        }

        for (i in rawList.indices) {
            val current = rawList[i]

            if (current.isInstrumental && enriched.lastOrNull()?.isInstrumental == true) {
                continue
            }
            enriched.add(current)

            val nextIdx = (i + 1 until rawList.size).firstOrNull { !rawList[it].isTranslation }
            if (nextIdx != null && !current.isInstrumental) {
                val nextLine = rawList[nextIdx]
                val gap = nextLine.timeStampMs - current.timeStampMs
                if (gap >= 8500L && !nextLine.isInstrumental) {
                    enriched.add(
                        LyricLine(
                            timeStampMs = current.timeStampMs + 4500L,
                            content = "",
                            isInstrumental = true
                        )
                    )
                }
            }
        }

        return enriched
    }

    private fun fetchFromLyricsOvh(rawTitle: String, rawArtist: String, durationMs: Long): String? {
        val cleanTitle = cleanQueryTitle(rawTitle)
        if (cleanTitle.length < 2 || isUnknownArtistName(rawArtist)) return null
        val primaryArtist = cleanQueryArtist(rawArtist)
            .split(Regex("(?:,|&|;|/|\\bfeat\\.?\\b|\\bft\\.?\\b)", RegexOption.IGNORE_CASE))
            .firstOrNull()
            ?.trim()
            .orEmpty()
        if (primaryArtist.isBlank()) return null
        try {
            val encArtist = URLEncoder.encode(primaryArtist, "UTF-8")
            val encTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val url = URL("https://api.lyrics.ovh/v1/$encArtist/$encTitle")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(resp)
                val lyricsText = json.optString("lyrics", "")
                if (lyricsText.isNotBlank()) {
                    val lines = lyricsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    if (lines.isNotEmpty()) {
                        val totalMs = durationMs.coerceAtLeast(30_000L)
                        val stepMs = (totalMs / (lines.size + 1)).coerceAtLeast(2000L)
                        val sb = StringBuilder()
                        lines.forEachIndexed { idx, line ->
                            val ms = (idx * stepMs) + 2500L
                            val min = ms / 60000
                            val sec = (ms % 60000) / 1000
                            val hundredths = (ms % 1000) / 10
                            sb.append(String.format(java.util.Locale.US, "[%02d:%02d.%02d] %s\n", min, sec, hundredths, line))
                        }
                        return sb.toString()
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    suspend fun translateLyricLines(lines: List<LyricLine>, targetLang: String): List<LyricLine> = withContext(Dispatchers.IO) {
        val textLines = lines.filter { !it.isInstrumental && !it.isTranslation && it.content.isNotBlank() }
        if (textLines.isEmpty()) return@withContext lines

        val joined = textLines.joinToString("\n") { it.content }
        val translatedText = runCatching {
            val encoded = URLEncoder.encode(joined, "UTF-8")
            val url = URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }
            conn.outputStream.use { it.write("q=$encoded".toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArr = org.json.JSONArray(resp)
                val sentences = jsonArr.optJSONArray(0)
                val sb = StringBuilder()
                if (sentences != null) {
                    for (i in 0 until sentences.length()) {
                        val item = sentences.optJSONArray(i)
                        if (item != null) sb.append(item.optString(0))
                    }
                }
                sb.toString()
            } else null
        }.getOrNull()

        if (translatedText.isNullOrBlank()) return@withContext lines

        val translatedLines = translatedText.lines().map { it.trim() }
        var transIdx = 0
        val result = mutableListOf<LyricLine>()
        for (line in lines) {
            result.add(line)
            if (!line.isInstrumental && !line.isTranslation && line.content.isNotBlank() && transIdx < translatedLines.size) {
                val transContent = translatedLines[transIdx].ifBlank { null }
                if (transContent != null) {
                    result.add(LyricLine(timeStampMs = line.timeStampMs, content = transContent, isTranslation = true))
                }
                transIdx++
            }
        }
        result
    }
}
