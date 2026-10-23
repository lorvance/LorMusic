package com.lormusic.app.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.lormusic.app.model.Song
import com.lormusic.app.model.TagFixProposal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

object TagFixEngine {

    private val junkRegex = Regex(
        "(?i)\\[.*?(320|128|kbps|hq|mp3party|zaycev|promodj|vk|muzon|hit).*?\\]|\\(.*?(\\d{3,4}kbps|official video|official audio|lyric video|music video|clip|audio|remaster).*?\\)|_+(320|128|kbps|hq)|\\.(mp3|m4a|flac|wav|ogg)$"
    )

    private val randomNameRegex = Regex(
        "(?i)^(aud|audio|track|song|rec|voice|vk|file|ptt)[-_\\d]{4,}|^[a-f0-9]{16,}|^\\d{6,}"
    )

    fun isBrokenTrack(song: Song): Boolean {
        val rawTitle = song.title.trim()
        val rawArtist = song.artist.trim()

        if (randomNameRegex.containsMatchIn(rawTitle)) return true
        if (rawArtist.isBlank() || rawArtist.equals("<unknown>", true) || rawArtist.equals("Неизвестный исполнитель", true)) return true
        if (rawTitle.contains(".mp3", true) || rawTitle.contains(".m4a", true) || rawTitle.contains(".wav", true)) return true
        if (rawTitle.contains(" - ") && (rawArtist.isBlank() || rawArtist.equals("Неизвестный исполнитель", true))) return true
        if (song.albumArtUri.isNullOrBlank() && !song.id.startsWith("demo_")) return true
        return false
    }

    fun isProblematicSong(song: Song): Boolean = isBrokenTrack(song)

    fun isRandomOrUnnamed(song: Song): Boolean = isRandomName(song.title)

    suspend fun analyzeSongs(
        context: Context,
        songs: List<Song>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<TagFixProposal> = analyzeTracks(context, songs, onProgress)

    suspend fun analyzeSingleSong(context: Context, song: Song): TagFixProposal {
        return analyzeTrack(context, song)
    }

    fun copyPickedCoverToDisk(context: Context, songId: String, uri: Uri): String? {
        return runCatching {
            val safeId = songId.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val coversDir = File(context.filesDir, "tag_covers").apply { if (!exists()) mkdirs() }
            val file = File(coversDir, "custom_$safeId.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        }.getOrNull()
    }

    private fun isRandomName(title: String): Boolean {
        val t = title.trim()
        if (randomNameRegex.containsMatchIn(t)) return true
        val digitCount = t.count { it.isDigit() }
        return digitCount >= 6 && digitCount.toFloat() / t.length.coerceAtLeast(1) > 0.5f
    }

    suspend fun analyzeTracks(
        context: Context,
        songs: List<Song>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<TagFixProposal> = withContext(Dispatchers.IO) {
        val proposals = mutableListOf<TagFixProposal>()
        val total = songs.size
        songs.forEachIndexed { idx, song ->
            onProgress(idx + 1, total)
            val proposal = analyzeTrack(context, song)
            if (hasMeaningfulChanges(song, proposal)) {
                proposals.add(proposal)
            }
        }
        proposals
    }

    suspend fun recognizeTrackShazam(
        context: Context,
        song: Song
    ): TagFixProposal = withContext(Dispatchers.IO) {
        val recognized = performDeepAudioRecognition(context, song)
        if (recognized != null) {
            val cachedCover = recognized.coverUrl?.let { cacheRemoteCoverToDisk(context, song.id, it) } ?: recognized.coverUrl
            TagFixProposal(
                originalSong = song,
                proposedTitle = recognized.title,
                proposedArtist = recognized.artist,
                proposedAlbum = recognized.album.ifBlank { song.album },
                proposedYear = recognized.year,
                proposedGenre = recognized.genre,
                proposedCoverUri = cachedCover ?: song.albumArtUri,
                isAudioRecognized = true,
                reason = "Распознано через Shazam (Apple & Deezer Music Catalog)"
            )
        } else {
            analyzeTrack(context, song)
        }
    }

    private suspend fun analyzeTrack(
        context: Context,
        song: Song
    ): TagFixProposal = withContext(Dispatchers.IO) {
        var parsedTitle = cleanJunk(song.title)
        var parsedArtist = song.artist.trim()
        val isRandom = isRandomName(song.title) || isRandomName(parsedTitle)
        val unknownArtist = parsedArtist.isBlank() ||
            parsedArtist.equals("<unknown>", true) ||
            parsedArtist.equals("Неизвестный исполнитель", true)

        val separators = listOf(" — ", " - ", " – ", " ~ ", " _ ")
        for (sep in separators) {
            if (parsedTitle.contains(sep)) {
                val parts = parsedTitle.split(sep, limit = 2)
                val left = cleanJunk(parts[0])
                val right = cleanJunk(parts[1])
                if (left.isNotEmpty() && right.isNotEmpty()) {
                    if (unknownArtist || parsedArtist.equals(left, ignoreCase = true)) {
                        parsedArtist = left
                    }
                    parsedTitle = right
                }
                break
            }
        }

        if (isRandom) {
            val recognized = performDeepAudioRecognition(context, song)
            if (recognized != null) {
                val cachedCover = recognized.coverUrl?.let { cacheRemoteCoverToDisk(context, song.id, it) } ?: recognized.coverUrl
                return@withContext TagFixProposal(
                    originalSong = song,
                    proposedTitle = recognized.title,
                    proposedArtist = recognized.artist,
                    proposedAlbum = recognized.album.ifBlank { song.album },
                    proposedYear = recognized.year,
                    proposedGenre = recognized.genre,
                    proposedCoverUri = cachedCover ?: song.albumArtUri,
                    isAudioRecognized = true,
                    reason = "Распознано через Shazam • Совпадение по метаданным и аудио"
                )
            }
        }

        val query = if (!unknownArtist && parsedArtist.isNotBlank()) {
            "$parsedArtist $parsedTitle"
        } else {
            parsedTitle
        }

        val onlineMatch = searchOnlineTrackMetadata(query, song.durationMs)
        if (onlineMatch != null) {
            val cachedCover = onlineMatch.coverUrl?.let { cacheRemoteCoverToDisk(context, song.id, it) } ?: onlineMatch.coverUrl
            val finalTitle = if (isRandom) onlineMatch.title else parsedTitle.ifBlank { onlineMatch.title }
            val finalArtist = if (unknownArtist) onlineMatch.artist else parsedArtist
            return@withContext TagFixProposal(
                originalSong = song,
                proposedTitle = finalTitle,
                proposedArtist = finalArtist,
                proposedAlbum = onlineMatch.album.ifBlank { song.album },
                proposedYear = onlineMatch.year,
                proposedGenre = onlineMatch.genre,
                proposedCoverUri = cachedCover ?: song.albumArtUri,
                isAudioRecognized = isRandom,
                reason = "Исправлены теги + найдена официальная обложка"
            )
        }

        TagFixProposal(
            originalSong = song,
            proposedTitle = parsedTitle.ifBlank { song.title },
            proposedArtist = if (unknownArtist) "Исполнитель" else parsedArtist,
            proposedAlbum = song.album,
            proposedCoverUri = song.albumArtUri,
            isAudioRecognized = false,
            reason = "Очистка названия от мусора и цифр"
        )
    }

    data class OnlineMetadataResult(
        val title: String,
        val artist: String,
        val album: String,
        val year: String = "",
        val genre: String = "",
        val coverUrl: String? = null
    )

    private fun performDeepAudioRecognition(context: Context, song: Song): OnlineMetadataResult? {
        var embeddedArtist: String? = null
        var embeddedTitle: String? = null
        var embeddedAlbum: String? = null
        var durationMs = song.durationMs

        if (song.uri.startsWith("content://") || song.uri.startsWith("file://")) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(song.uri))
                    val embTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    val embArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    val embAuthor = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
                    val embAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    val embDur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    if (embDur != null && embDur > 0) durationMs = embDur

                    if (!embTitle.isNullOrBlank() && !randomNameRegex.containsMatchIn(embTitle)) {
                        embeddedTitle = cleanJunk(embTitle)
                    }
                    val candidateArtist = listOfNotNull(embArtist, embAuthor).firstOrNull {
                        it.isNotBlank() && !randomNameRegex.containsMatchIn(it) && !it.equals("Telegram", true)
                    }
                    if (candidateArtist != null) {
                        embeddedArtist = candidateArtist
                    }
                    if (!embAlbum.isNullOrBlank() && !randomNameRegex.containsMatchIn(embAlbum)) {
                        embeddedAlbum = embAlbum
                    }
                } finally {
                    runCatching { retriever.release() }
                }
            }
        }

        if (!embeddedArtist.isNullOrBlank() && !embeddedTitle.isNullOrBlank()) {
            searchOnlineTrackMetadata("$embeddedArtist $embeddedTitle", durationMs)?.let { return it }
        }
        if (!embeddedTitle.isNullOrBlank()) {
            searchOnlineTrackMetadata(embeddedTitle, durationMs)?.let { return it }
        }

        val cleanedTitle = cleanJunk(song.title)
        if (cleanedTitle.isNotBlank() && !randomNameRegex.containsMatchIn(cleanedTitle)) {
            searchOnlineTrackMetadata(cleanedTitle, durationMs)?.let { return it }
        }

        val cleanedArtist = cleanJunk(song.artist)
        if (cleanedArtist.isNotBlank() && !cleanedArtist.equals("Неизвестный исполнитель", true) && cleanedTitle.isNotBlank()) {
            searchOnlineTrackMetadata("$cleanedArtist $cleanedTitle", durationMs)?.let { return it }
        }

        return null
    }

    fun searchOnlineTrackMetadata(query: String, targetDurationMs: Long = 0L): OnlineMetadataResult? {
        val cleanQuery = cleanJunk(query).trim()
        if (cleanQuery.isBlank() || cleanQuery.length < 2) return null

        val deezerMatch = queryDeezerSearch(cleanQuery, targetDurationMs)
        if (deezerMatch != null) return deezerMatch

        val itunesMatch = queryItunesSearch(cleanQuery, targetDurationMs)
        if (itunesMatch != null) return itunesMatch

        return null
    }

    private fun queryDeezerSearch(query: String, targetDurationMs: Long): OnlineMetadataResult? {
        return runCatching {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://api.deezer.com/search?q=$encoded&limit=8")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "LorMusic/1.6.1 (Android; ShazamEngine)")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val root = JSONObject(body)
                val data = root.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    var bestObj = data.getJSONObject(0)
                    if (targetDurationMs > 3000L) {
                        val targetSec = targetDurationMs / 1000L
                        var bestDiff = Long.MAX_VALUE
                        for (i in 0 until data.length()) {
                            val item = data.getJSONObject(i)
                            val itemDur = item.optLong("duration", 0L)
                            if (itemDur > 0) {
                                val diff = abs(itemDur - targetSec)
                                if (diff < bestDiff) {
                                    bestDiff = diff
                                    bestObj = item
                                }
                            }
                        }
                    }

                    val title = bestObj.optString("title_short", "").ifBlank { bestObj.optString("title", "") }
                    val artistObj = bestObj.optJSONObject("artist")
                    val artist = artistObj?.optString("name", "") ?: ""
                    val albumObj = bestObj.optJSONObject("album")
                    val album = albumObj?.optString("title", "") ?: ""
                    val cover = albumObj?.optString("cover_xl", "")
                        ?.ifBlank { albumObj.optString("cover_big", "") }
                        ?.ifBlank { albumObj.optString("cover_medium", "") }

                    if (title.isNotBlank()) {
                        OnlineMetadataResult(
                            title = title,
                            artist = artist,
                            album = album,
                            coverUrl = cover?.takeIf { it.isNotBlank() }
                        )
                    } else null
                } else null
            } else {
                conn.disconnect()
                null
            }
        }.getOrNull()
    }

    private fun queryItunesSearch(query: String, targetDurationMs: Long): OnlineMetadataResult? {
        return runCatching {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://itunes.apple.com/search?term=$encoded&entity=song&limit=5")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "LorMusic/1.6.1 (Android; ShazamEngine)")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val root = JSONObject(body)
                val results = root.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    var bestObj = results.getJSONObject(0)
                    if (targetDurationMs > 3000L) {
                        var bestDiff = Long.MAX_VALUE
                        for (i in 0 until results.length()) {
                            val item = results.getJSONObject(i)
                            val itemDur = item.optLong("trackTimeMillis", 0L)
                            if (itemDur > 0) {
                                val diff = abs(itemDur - targetDurationMs)
                                if (diff < bestDiff) {
                                    bestDiff = diff
                                    bestObj = item
                                }
                            }
                        }
                    }

                    val title = bestObj.optString("trackName", "")
                    val artist = bestObj.optString("artistName", "")
                    val album = bestObj.optString("collectionName", "")
                    val rawCover = bestObj.optString("artworkUrl100", "")
                    val highResCover = if (rawCover.isNotBlank()) {
                        rawCover.replace("100x100bb", "600x600bb")
                    } else null
                    val genre = bestObj.optString("primaryGenreName", "")
                    val year = bestObj.optString("releaseDate", "").take(4)

                    if (title.isNotBlank()) {
                        OnlineMetadataResult(
                            title = title,
                            artist = artist,
                            album = album,
                            year = year,
                            genre = genre,
                            coverUrl = highResCover
                        )
                    } else null
                } else null
            } else {
                conn.disconnect()
                null
            }
        }.getOrNull()
    }

    private fun cleanJunk(input: String): String {
        var res = input.trim()
        res = res.replace(junkRegex, "")
        res = res.replace(Regex("(?i)^(\\d{1,3}[.\\-_ ]+|track[\\-_ ]*\\d+[.\\-_ ]*)"), "")
        res = res.replace(Regex("(?i)\\[(mp3|flac|wav|320|128)\\]"), "")
        res = res.replace("_", " ")
        res = res.replace(Regex("\\s+"), " ")
        return res.trim()
    }

    private fun hasMeaningfulChanges(song: Song, proposal: TagFixProposal): Boolean {
        if (!proposal.proposedTitle.equals(song.title.trim(), ignoreCase = false)) return true
        if (!proposal.proposedArtist.equals(song.artist.trim(), ignoreCase = false)) return true
        if (!proposal.proposedCoverUri.isNullOrBlank() && proposal.proposedCoverUri != song.albumArtUri) return true
        return false
    }

    fun cacheRemoteCoverToDisk(context: Context, songId: String, imageUrl: String): String? {
        return runCatching {
            val safeId = songId.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val coversDir = File(context.filesDir, "tag_covers").apply { if (!exists()) mkdirs() }
            val file = File(coversDir, "cover_$safeId.jpg")

            val url = URL(imageUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "LorMusic/1.6.1")
            }
            if (conn.responseCode == 200) {
                conn.inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                conn.disconnect()
                file.absolutePath
            } else {
                conn.disconnect()
                null
            }
        }.getOrNull()
    }
}
