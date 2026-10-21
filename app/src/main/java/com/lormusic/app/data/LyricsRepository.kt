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

        val cacheDir = File(context.cacheDir, "lyrics_cache").apply { if (!exists()) mkdirs() }
        val safeKey = (song.title + "_" + song.artist).replace(Regex("[^a-zA-Z0-9а-яА-Я_-]"), "_")
        val cachedFile = File(cacheDir, "$safeKey.lrc")
        if (cachedFile.exists() && cachedFile.length() > 10) {
            val cachedText = runCatching { cachedFile.readText() }.getOrNull()
            if (!cachedText.isNullOrBlank()) {
                val parsed = parseLrcString(cachedText)
                if (parsed.isNotEmpty()) return@withContext parsed
            }
        }

        val fetchedLrc = fetchLrcFromNetwork(song.title, song.artist, song.durationMs)
        if (!fetchedLrc.isNullOrBlank()) {
            runCatching { cachedFile.writeText(fetchedLrc) }
            val parsed = parseLrcString(fetchedLrc)
            if (parsed.isNotEmpty()) return@withContext parsed
        }

        emptyList()
    }

    private fun cleanQueryTitle(raw: String): String {
        return raw
            .replace(Regex("\\.(mp3|flac|m4a|wav|ogg)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(feat\\..*?\\)|\\[.*?\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[_^~]+"), " ")
            .trim()
    }

    private fun fetchLrcFromNetwork(rawTitle: String, rawArtist: String, durationMs: Long): String? {
        val cleanTitle = cleanQueryTitle(rawTitle)
        if (cleanTitle.length < 2) return null

        val hasRealArtist = rawArtist.isNotBlank() &&
            !rawArtist.contains("Неизвестный", ignoreCase = true) &&
            !rawArtist.contains("unknown", ignoreCase = true) &&
            !rawArtist.equals("Telegram", ignoreCase = true)

        val queries = buildList {
            if (hasRealArtist) add("$cleanTitle $rawArtist")
            add(cleanTitle)
        }

        for (query in queries) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = URL("https://lrclib.net/api/search?q=$encoded")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "LorMusic/1.2.0 (Android; Media3)")
                }
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONArray(response)
                    var bestPlain: String? = null
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val synced = obj.optString("syncedLyrics", "")
                        if (synced.isNotBlank() && synced != "null") {
                            return synced
                        }
                        val plain = obj.optString("plainLyrics", "")
                        if (bestPlain == null && plain.isNotBlank() && plain != "null") {
                            bestPlain = plain
                        }
                    }
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
                }
            } catch (_: Exception) {

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
}
