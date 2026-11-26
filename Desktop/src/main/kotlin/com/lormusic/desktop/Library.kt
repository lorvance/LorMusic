package com.lormusic.desktop

import com.google.gson.GsonBuilder
import com.lormusic.app.model.*
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey

val dataDir: File =
    File(
            System.getProperty("lormusic.dataDir")
                ?: if (System.getProperty("os.name").startsWith("Windows"))
                    "${System.getenv("APPDATA")}/LorMusic"
                else
                    "${System.getenv("XDG_DATA_HOME") ?: "${System.getProperty("user.home")}/.local/share"}/lormusic"
        )
        .apply {
            mkdirs()
            if (!System.getProperty("os.name").startsWith("Windows")) {
                setReadable(false, false)
                setWritable(false, false)
                setExecutable(false, false)
                setReadable(true, true)
                setWritable(true, true)
                setExecutable(true, true)
            }
        }
val gson = GsonBuilder().setPrettyPrinting().create()

data class UserList(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    val songs: MutableList<Song> = mutableListOf(),
)

data class SavedState(
    var library: MutableList<Song> = mutableListOf(),
    var playlists: MutableList<UserList> = mutableListOf(),
    var liked: MutableSet<String> = mutableSetOf(),
    var favorites: MutableSet<String> = mutableSetOf(),
    var history: MutableList<Song> = mutableListOf(),
    var hidden: MutableSet<String> = mutableSetOf(),
    var folders: MutableSet<String> = mutableSetOf(),
    var accent: Int = 3,
    var light: Boolean = false,
    var achievementsEnabled: Boolean = false,
    var heardTracks: MutableSet<String> = mutableSetOf(),
    var heardArtists: MutableSet<String> = mutableSetOf(),
    var earnedAwards: MutableSet<String> = mutableSetOf(),
    var autoUpdates: Boolean = true,
    var lastUpdateCheck: Long = 0,
    var playerGradient: Boolean = true,
    var dynamicColors: Boolean = true,
    var language: String = "ru",
    var volume: Int = 75,
    var speed: Float = 1f,
    var youtubeCookie: String = "",
    var soundcloudToken: String = "",
    var equalizer: MutableList<Float> = MutableList(10) { 0f },
    var eqEnabled: Boolean = false,
    var queue: MutableList<Song> = mutableListOf(),
    var lastSong: String? = null,
    var lastPosition: Long = 0,
    var downloadFolder: String = File(System.getProperty("user.home"), "Music/LorMusic").path,
)

object Library {
    private val stateFile = File(dataDir, "library.json")

    fun load(): SavedState =
        runCatching { gson.fromJson(stateFile.readText(), SavedState::class.java) }.getOrNull()
            ?: SavedState()

    @Synchronized
    fun save(state: SavedState) {
        val tmp = File(dataDir, "library.json.tmp")
        tmp.createNewFile()
        tmp.setReadable(false, false)
        tmp.setReadable(true, true)
        tmp.setWritable(false, false)
        tmp.setWritable(true, true)
        tmp.writeText(gson.toJson(state))
        runCatching {
                Files.move(
                    tmp.toPath(),
                    stateFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            }
            .getOrElse {
                Files.move(tmp.toPath(), stateFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        stateFile.setReadable(false, false)
        stateFile.setReadable(true, true)
        stateFile.setWritable(false, false)
        stateFile.setWritable(true, true)
    }

    val extensions = setOf("mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "aiff", "wma", "alac")

    fun scan(root: File): List<Song> =
        root
            .walkTopDown()
            .onEnter { !it.name.startsWith(".") }
            .filter { it.isFile && it.extension.lowercase() in extensions }
            .mapNotNull { runCatching { read(it) }.getOrNull() }
            .toList()

    fun read(file: File): Song {
        val canonical = file.canonicalFile
        val id =
            "local_" +
                MessageDigest.getInstance("SHA-256")
                    .digest(canonical.path.toByteArray())
                    .take(12)
                    .joinToString("") { "%02x".format(it) }
        val audio = runCatching { AudioFileIO.read(canonical) }.getOrNull()
        val tag = audio?.tag
        fun field(k: FieldKey) = runCatching { tag?.getFirst(k).orEmpty() }.getOrDefault("")
        val artwork = runCatching { tag?.firstArtwork?.binaryData }.getOrNull()
        val art =
            artwork?.let { bytes ->
                File(dataDir, "covers/$id.jpg")
                    .apply {
                        parentFile.mkdirs()
                        if (!exists()) writeBytes(bytes)
                    }
                    .toURI()
                    .toString()
            }
        return Song(
            id,
            field(FieldKey.TITLE).ifBlank { canonical.nameWithoutExtension },
            field(FieldKey.ARTIST).ifBlank { "Неизвестный исполнитель" },
            field(FieldKey.ALBUM),
            (audio?.audioHeader?.trackLength ?: 0) * 1000L,
            canonical.toURI().toString(),
            id.hashCode(),
            art,
            true,
            canonical.path,
            canonical.parentFile.name,
            canonical.lastModified() / 1000,
            field(FieldKey.GENRE),
            field(FieldKey.YEAR),
            field(FieldKey.TRACK),
            field(FieldKey.LYRICS).ifBlank { null },
        )
    }

    fun writeTags(song: Song): Song {
        val file = File(requireNotNull(song.filePath))
        val af = AudioFileIO.read(file)
        val t = af.tagOrCreateAndSetDefault
        mapOf(
                FieldKey.TITLE to song.title,
                FieldKey.ARTIST to song.artist,
                FieldKey.ALBUM to song.album,
                FieldKey.GENRE to song.genre,
                FieldKey.YEAR to song.year,
                FieldKey.TRACK to song.trackNumber,
                FieldKey.LYRICS to song.customLyrics.orEmpty(),
            )
            .forEach { (k, v) ->
                if (v.isNotBlank()) t.setField(k, v) else runCatching { t.deleteField(k) }
            }
        val finalSong =
            if (song.albumArtUri?.startsWith("http") == true) {
                val c = java.net.URL(song.albumArtUri).openConnection()
                c.connectTimeout = 10000
                c.readTimeout = 10000
                val cover = File(dataDir, "covers/${song.id}-tag.jpg")
                cover.parentFile.mkdirs()
                c.getInputStream().use { i -> cover.outputStream().use { o -> i.copyTo(o) } }
                song.copy(albumArtUri = cover.toURI().toString())
            } else song
        finalSong.albumArtUri
            ?.takeIf { it.startsWith("file:") }
            ?.let {
                val image = File(java.net.URI(it))
                t.deleteArtworkField()
                t.setField(org.jaudiotagger.tag.images.ArtworkFactory.createArtworkFromFile(image))
            }
        af.commit()
        return finalSong
    }

    fun exportPlaylist(list: List<Song>, file: File) {
        file.writeText(
            "#EXTM3U\n" +
                list.joinToString("\n") {
                    "#EXTINF:${it.durationMs/1000},${it.artist} - ${it.title}\n${it.filePath ?: it.uri}"
                }
        )
    }

    fun importPlaylist(file: File): UserList {
        val entries =
            file
                .readLines()
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .mapNotNull { value ->
                    runCatching {
                            val f =
                                if (value.startsWith("file:")) File(java.net.URI(value))
                                else
                                    File(value).let {
                                        if (it.isAbsolute) it else File(file.parentFile, value)
                                    }
                            if (f.isFile) read(f) else null
                        }
                        .getOrNull()
                }
        return UserList(name = file.nameWithoutExtension, songs = entries.toMutableList())
    }
}
