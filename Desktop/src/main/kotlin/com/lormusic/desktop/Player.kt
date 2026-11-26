package com.lormusic.desktop

import androidx.compose.runtime.*
import com.lormusic.app.data.*
import com.lormusic.app.model.*
import java.net.URI
import javax.swing.SwingUtilities
import kotlinx.coroutines.*
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

class Player(val state: SavedState, val scope: CoroutineScope, val notify: (String) -> Unit) {
    private var factory: MediaPlayerFactory? = null
    private var native: MediaPlayer? = null
    var available by mutableStateOf(false)
        private set

    var song by mutableStateOf<Song?>(null)
        private set

    var playing by mutableStateOf(false)
        private set

    var loading by mutableStateOf(false)
        private set

    var position by mutableStateOf(0L)
        private set

    var duration by mutableStateOf(0L)
        private set

    var volume by mutableStateOf(state.volume)
        private set

    var speed by mutableStateOf(state.speed)
        private set

    var shuffle by mutableStateOf(false)
    var repeat by mutableStateOf(RepeatMode.OFF)
    var queue by mutableStateOf(state.queue.toList())
        private set

    var error by mutableStateOf<String?>(null)
        private set

    private var resolve: Job? = null
    private var sleep: Job? = null

    init {
        try {
            if (System.getProperty("os.name").startsWith("Windows")) {
                val vlc =
                    java.io.File(
                        System.getProperty("compose.application.resources.dir")
                            ?: System.getProperty("lormusic.home")
                            ?: System.getProperty("user.dir"),
                        "vlc",
                    )
                if (vlc.exists()) {
                    System.setProperty("jna.library.path", vlc.path)
                    System.setProperty("VLC_PLUGIN_PATH", java.io.File(vlc, "plugins").path)
                }
            }
            factory =
                MediaPlayerFactory(
                    "--no-video",
                    "--quiet",
                    "--no-snapshot-preview",
                    "--no-metadata-network-access",
                )
            native = factory!!.mediaPlayers().newMediaPlayer()
            native!!
                .events()
                .addMediaPlayerEventListener(
                    object : MediaPlayerEventAdapter() {
                        override fun playing(mediaPlayer: MediaPlayer) {
                            ui {
                                playing = true
                                loading = false
                                error = null
                            }
                        }

                        override fun paused(mediaPlayer: MediaPlayer) {
                            ui { playing = false }
                        }

                        override fun stopped(mediaPlayer: MediaPlayer) {
                            ui { playing = false }
                        }

                        override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
                            ui { position = newTime }
                        }

                        override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
                            ui { if (newLength > 0) duration = newLength }
                        }

                        override fun finished(mediaPlayer: MediaPlayer) {
                            ui { next(true) }
                        }

                        override fun error(mediaPlayer: MediaPlayer) {
                            ui {
                                playing = false
                                loading = false
                                error =
                                    "Не удалось воспроизвести трек. Проверьте файл или доступность потока."
                                notify(error!!)
                            }
                        }
                    }
                )
            native!!.audio().setVolume(volume)
            available = true
            song = queue.firstOrNull { it.id == state.lastSong }
            position = state.lastPosition
            duration = song?.durationMs ?: 0
        } catch (e: Throwable) {
            error = "VLC не найден. Установите VLC и перезапустите LorMusic."
            notify(error!!)
        }
    }

    private fun ui(f: () -> Unit) {
        SwingUtilities.invokeLater(f)
    }

    fun play(s: Song, list: List<Song> = queue) {
        if (!available) {
            notify(error ?: "VLC недоступен")
            return
        }
        queue = if (list.any { it.id == s.id }) list.toList() else list + s
        resolve?.cancel()
        song = s
        duration = s.durationMs
        position = 0
        loading = true
        error = null
        state.history.removeAll { it.id == s.id }
        state.history.add(0, s)
        while (state.history.size > 150) state.history.removeLast()
        resolve =
            scope.launch {
                try {
                    val uri =
                        withContext(Dispatchers.IO) {
                            when {
                                s.isRemoteYt ->
                                    YouTubeMusicRepository.resolveSongStream(s)?.uri?.takeIf {
                                        it.startsWith("http")
                                    }
                                        ?: error(
                                            "YouTube не выдал аудиопоток. Проверьте вход в аккаунт."
                                        )
                                s.isRemoteSc ->
                                    SoundCloudRepository.resolveStreamUriForExoPlayer(
                                            URI.create(s.uri)
                                        )
                                        .toString()
                                        .takeIf { it.startsWith("http") }
                                        ?: error("SoundCloud не выдал поток")
                                else -> s.uri
                            }
                        }
                    ensureActive()
                    native!!
                        .media()
                        .play(
                            if (uri.startsWith("file:")) java.io.File(URI(uri)).path else uri,
                            ":no-video",
                            ":network-caching=2000",
                            ":http-user-agent=Mozilla/5.0",
                        )
                    native!!.controls().setRate(speed)
                    applyEqualizer()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    loading = false
                    playing = false
                    error = e.message
                    notify(e.message ?: "Ошибка воспроизведения")
                }
            }
    }

    fun toggle() {
        val n = native ?: return
        if (playing) n.controls().pause()
        else if (n.status().isPlayable) n.controls().play() else song?.let { play(it) }
    }

    fun seek(ms: Long) {
        native?.controls()?.setTime(ms.coerceIn(0, duration.coerceAtLeast(0)))
        position = ms
    }

    fun changeVolume(v: Int) {
        volume = v.coerceIn(0, 100)
        state.volume = volume
        native?.audio()?.setVolume(volume)
    }

    fun changeSpeed(v: Float) {
        speed = v
        state.speed = v
        native?.controls()?.setRate(v)
    }

    fun add(s: Song, next: Boolean = false) {
        val q = queue.toMutableList()
        if (next) {
            val i = q.indexOfFirst { it.id == song?.id }
            q.add((i + 1).coerceIn(0, q.size), s)
        } else q.add(s)
        queue = q
        notify(if (next) "Сыграет следующим" else "Добавлено в очередь")
    }

    fun moveQueue(index: Int, delta: Int) {
        val target = index + delta
        if (index !in queue.indices || target !in queue.indices) return
        queue = queue.toMutableList().apply { add(target, removeAt(index)) }
        save()
    }

    fun removeQueue(index: Int) {
        queue = queue.toMutableList().apply { removeAt(index) }
    }

    fun restoreQueue(list: List<Song>, lastId: String?, time: Long) {
        resolve?.cancel()
        native?.controls()?.stop()
        queue = list.toList()
        song = list.firstOrNull { it.id == lastId }
        position = time.coerceAtLeast(0)
        duration = song?.durationMs ?: 0
        playing = false
        loading = false
    }

    fun clearQueue() {
        queue = song?.let { listOf(it) } ?: emptyList()
    }

    fun next(ended: Boolean = false) {
        if (queue.isEmpty()) return
        if (ended && repeat == RepeatMode.ONE) {
            song?.let { play(it) }
            return
        }
        val i = queue.indexOfFirst { it.id == song?.id }
        val next =
            if (shuffle && queue.size > 1) queue.filter { it.id != song?.id }.random()
            else queue.getOrNull(i + 1) ?: if (repeat == RepeatMode.ALL) queue.first() else null
        if (next != null) play(next)
        else {
            playing = false
            loading = false
        }
    }

    fun previous() {
        if (position > 3000) {
            seek(0)
            return
        }
        val i = queue.indexOfFirst { it.id == song?.id }
        queue.getOrNull(i - 1)?.let { play(it) }
    }

    fun applyEqualizer() {
        runCatching {
            if (!state.eqEnabled) {
                native?.audio()?.setEqualizer(null)
            } else {
                val eq = factory!!.equalizer().newEqualizer()
                state.equalizer.forEachIndexed { i, v -> eq.setAmp(i, v) }
                native?.audio()?.setEqualizer(eq)
            }
        }
    }

    fun timer(minutes: Int) {
        sleep?.cancel()
        if (minutes > 0)
            sleep =
                scope.launch {
                    delay(minutes * 60000L)
                    if (playing) toggle()
                    notify("Таймер сна остановил воспроизведение")
                }
    }

    fun save() {
        state.queue = queue.toMutableList()
        state.lastSong = song?.id
        state.lastPosition = position
        Library.save(state)
    }

    fun close() {
        resolve?.cancel()
        save()
        native?.release()
        factory?.release()
    }
}
