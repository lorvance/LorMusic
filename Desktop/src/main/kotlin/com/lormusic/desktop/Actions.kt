package com.lormusic.desktop

import androidx.compose.runtime.*
import com.lormusic.app.data.*
import com.lormusic.app.model.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.*

class Actions(val state: SavedState, val scope: CoroutineScope, val notify: (String) -> Unit) {
    var revision by mutableStateOf(0)
    var busy by mutableStateOf(false)
    var progress by mutableStateOf("")
    var remote by mutableStateOf<List<Song>>(emptyList())
    var searchJob: Job? = null
    val player = Player(state, scope, notify)

    fun changed() {
        revision++
        Library.save(state)
    }

    fun work(label: String, block: suspend () -> Unit) {
        scope.launch {
            busy = true
            progress = label
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                notify(e.message ?: "Ошибка")
            } finally {
                busy = false
                progress = ""
            }
        }
    }

    fun importFiles(folder: Boolean = false) {
        val picker =
            JFileChooser().apply {
                dialogTitle = if (folder) "Добавить папку с музыкой" else "Добавить музыку"
                isMultiSelectionEnabled = !folder
                fileSelectionMode =
                    if (folder) JFileChooser.DIRECTORIES_ONLY else JFileChooser.FILES_ONLY
                if (!folder)
                    fileFilter =
                        FileNameExtensionFilter("Аудиофайлы", *Library.extensions.toTypedArray())
            }
        if (picker.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return
        val selected = if (folder) listOf(picker.selectedFile) else picker.selectedFiles.toList()
        importPaths(selected)
    }

    fun importPaths(selected: List<File>, playFirst: Boolean = false) {
        work("Читаю теги и обложки…") {
            val lists =
                withContext(Dispatchers.IO) {
                    selected
                        .filter { it.isFile && it.extension.lowercase() in setOf("m3u", "m3u8") }
                        .map { Library.importPlaylist(it) }
                }
            val added =
                withContext(Dispatchers.IO) {
                    selected.flatMap {
                        when {
                            it.isDirectory -> Library.scan(it)
                            it.extension.lowercase() in Library.extensions ->
                                listOf(Library.read(it))
                            else -> emptyList()
                        }
                    }
                } + lists.flatMap { it.songs }
            state.playlists.addAll(lists)
            state.folders += selected.filter { it.isDirectory }.map { it.canonicalPath }
            state.library = (state.library + added).distinctBy { it.id }.toMutableList()
            changed()
            notify("Добавлено треков: ${added.size}")
            if (playFirst) added.firstOrNull()?.let { player.play(it, added) }
        }
    }

    fun rescan() {
        work("Обновляю медиатеку…") {
            val found =
                withContext(Dispatchers.IO) { state.folders.flatMap { Library.scan(File(it)) } }
            state.library =
                (state.library.filter { it.filePath?.let { p -> File(p).exists() } ?: true } +
                        found)
                    .associateBy { it.id }
                    .values
                    .toMutableList()
            changed()
            notify("Медиатека обновлена")
        }
    }

    fun search(service: String, query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            feed(service)
            return
        }
        searchJob =
            scope.launch {
                delay(450)
                busy = true
                progress = "Ищу музыку…"
                try {
                    remote =
                        if (service == "YouTube Music")
                            YouTubeMusicRepository.searchYtMusic(
                                query,
                                60,
                                state.youtubeCookie.ifBlank { null },
                            )
                        else SoundCloudRepository.searchSoundCloud(query, 60)
                    if (remote.isEmpty())
                        notify("Ничего не найдено. Измените запрос или проверьте подключение.")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    notify(e.message ?: "Ошибка поиска")
                } finally {
                    busy = false
                    progress = ""
                }
            }
    }

    fun feed(service: String) {
        searchJob?.cancel()
        remote = emptyList()
        searchJob =
            scope.launch {
                busy = true
                progress = "Загружаю музыку…"
                try {
                    val loaded =
                        if (service == "YouTube Music")
                            YouTubeMusicRepository.loadPersonalizedHomeFeed(
                                    state.history + state.library.take(8),
                                    state.youtubeCookie.ifBlank { null },
                                )
                                .let { (it.first + it.second).distinctBy { it.id } }
                        else SoundCloudRepository.fetchTrending(60)
                    ensureActive()
                    remote = loaded
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    notify(e.message ?: "Ошибка загрузки")
                } finally {
                    busy = false
                    progress = ""
                }
            }
    }

    fun favorite(s: Song, unified: Boolean = false) {
        val set = if (unified) state.favorites else state.liked
        if (!set.add(s.id)) set.remove(s.id)
        if (state.library.none { it.id == s.id }) state.library.add(s)
        changed()
        if (s.isRemoteYt && !unified)
            scope.launch {
                if (s.id in set)
                    YouTubeMusicRepository.likeYtMusicVideo(
                        YouTubeMusicRepository.extractVideoId(s.id) ?: "",
                        state.youtubeCookie,
                    )
                else
                    YouTubeMusicRepository.removeLikeFromYtMusic(
                        YouTubeMusicRepository.extractVideoId(s.id) ?: "",
                        state.youtubeCookie,
                    )
            }
    }

    fun download(s: Song) {
        if (s.isLocal) {
            notify("Этот трек уже на устройстве")
            return
        }
        work("Скачиваю ${s.title}…") {
            val result =
                withContext(Dispatchers.IO) {
                    val uri =
                        when {
                            s.isRemoteYt -> YouTubeMusicRepository.resolveSongStream(s)?.uri
                            s.isRemoteSc ->
                                SoundCloudRepository.resolveStreamUriForExoPlayer(URI.create(s.uri))
                                    .toString()
                            else -> s.uri
                        } ?: error("Поток недоступен")
                    require(uri.startsWith("http")) { "Поток недоступен" }
                    val conn = URL(uri).openConnection() as HttpURLConnection
                    conn.connectTimeout = 20000
                    conn.readTimeout = 30000
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    conn.connect()
                    require(conn.responseCode in 200..299) { "Сервер вернул ${conn.responseCode}" }
                    conn.disconnect()
                    val dir = File(state.downloadFolder).apply { mkdirs() }
                    val base =
                        "${s.artist} - ${s.title}".replace(Regex("[\\\\/:*?\"<>|]"), "_").take(170)
                    var target = File(dir, "$base.mp3")
                    var index = 2
                    while (target.exists()) {
                        target = File(dir, "$base (${index++}).mp3")
                    }
                    val tmp = File(dir, "${target.name}.part.mp3")
                    try {
                        DesktopMedia.convertToMp3(uri, tmp, s.durationMs)
                        java.nio.file.Files.move(tmp.toPath(), target.toPath())
                        val cover =
                            s.albumArtUri?.let { art ->
                                runCatching {
                                        if (art.startsWith("http")) {
                                            val c = URL(art).openConnection()
                                            c.connectTimeout = 10000
                                            c.readTimeout = 10000
                                            val dest =
                                                File(
                                                    dataDir,
                                                    "covers/download-${System.nanoTime()}.jpg",
                                                )
                                            dest.parentFile.mkdirs()
                                            c.getInputStream().use { i ->
                                                dest.outputStream().use { o -> i.copyTo(o) }
                                            }
                                            dest.toURI().toString()
                                        } else art
                                    }
                                    .getOrNull()
                            }
                        val local =
                            Library.read(target)
                                .copy(
                                    title = s.title,
                                    artist = s.artist,
                                    album = s.album,
                                    customLyrics = s.customLyrics,
                                    year = s.year,
                                    genre = s.genre,
                                    albumArtUri = cover,
                                )
                        Library.writeTags(local)
                        local
                    } catch (e: Throwable) {
                        target.delete()
                        throw e
                    } finally {
                        tmp.delete()
                    }
                }
            state.library.add(result)
            changed()
            notify("Сохранено: ${result.title}")
        }
    }

    fun share(s: Song) {
        val url =
            if (s.isRemoteYt) YouTubeMusicRepository.getShareUrl(s)
            else if (s.isRemoteSc) SoundCloudRepository.getShareUrl(s) else s.filePath
        java.awt.Toolkit.getDefaultToolkit()
            .systemClipboard
            .setContents(java.awt.datatransfer.StringSelection(url ?: s.uri), null)
        notify("Ссылка скопирована")
    }

    fun backup() {
        val p = JFileChooser().apply { selectedFile = File("LorMusic-backup.json") }
        if (p.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            p.selectedFile.writeText(
                gson.toJson(state.copy(youtubeCookie = "", soundcloudToken = ""))
            )
            notify("Резервная копия сохранена")
        }
    }

    fun importPlaylist() {
        val p =
            JFileChooser().apply {
                fileFilter = FileNameExtensionFilter("M3U / M3U8", "m3u", "m3u8")
            }
        if (p.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
            work("Импортирую плейлист…") {
                val list = withContext(Dispatchers.IO) { Library.importPlaylist(p.selectedFile) }
                state.playlists.add(list)
                state.library = (state.library + list.songs).distinctBy { it.id }.toMutableList()
                changed()
            }
    }

    fun syncAccounts() {
        YouTubeMusicRepository.currentAuthCookie = state.youtubeCookie.ifBlank { null }
        SoundCloudRepository.currentAuthToken = state.soundcloudToken.ifBlank { null }
        work("Синхронизирую аккаунты…") {
            if (state.youtubeCookie.isNotBlank()) {
                require(YouTubeMusicRepository.hasValidAuthCookie(state.youtubeCookie)) {
                    "В cookies YouTube нет SAPISID"
                }
                val sync = YouTubeMusicRepository.syncAuthenticatedAccount(state.youtubeCookie)
                state.library =
                    (state.library + sync.likedSongs + sync.historySongs)
                        .distinctBy { it.id }
                        .toMutableList()
                state.liked += sync.likedSongs.map { it.id }
                remote = sync.homeRecommendations
                notify("YouTube Music: ${sync.profile?.displayName ?: "сессия сохранена"}")
            }
            if (state.soundcloudToken.isNotBlank()) {
                SoundCloudRepository.currentAuthToken =
                    SoundCloudRepository.cleanToken(state.soundcloudToken)
                val profile = SoundCloudRepository.fetchUserProfile(state.soundcloudToken)
                require(profile != null) { "SoundCloud: не удалось войти" }
                val liked = SoundCloudRepository.fetchUserLikes(100)
                state.library = (state.library + liked).distinctBy { it.id }.toMutableList()
                state.liked += liked.map { it.id }
                notify("SoundCloud: ${profile.username}")
            }
            changed()
        }
    }
}
