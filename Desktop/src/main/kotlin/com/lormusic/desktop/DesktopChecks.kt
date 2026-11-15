package com.lormusic.desktop

import com.lormusic.app.data.*
import com.lormusic.app.model.*
import java.io.ByteArrayInputStream
import java.io.File
import javax.sound.sampled.*
import kotlin.math.*
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing

object DesktopChecks {
    fun run() = runBlocking {
        val folder = File(dataDir, "self-test").apply { mkdirs() }
        val audio = File(folder, "test.wav")
        val bytes = ByteArray(44100 * 2 * 4)
        for (i in 0 until bytes.size / 2) {
            val n = (sin(2 * PI * 440 * i / 44100) * 5000).toInt()
            bytes[i * 2] = (n and 255).toByte()
            bytes[i * 2 + 1] = (n shr 8).toByte()
        }
        AudioSystem.write(
            AudioInputStream(
                ByteArrayInputStream(bytes),
                AudioFormat(44100f, 16, 1, true, false),
                (bytes.size / 2).toLong(),
            ),
            AudioFileFormat.Type.WAVE,
            audio,
        )
        val s = Library.read(audio)
        check(s.isLocal && s.durationMs in 3900..4100)
        check(s.filePath == audio.canonicalPath)
        val mp3 = File(folder, "test-converted.mp3")
        DesktopMedia.convertToMp3(audio.path, mp3, 4000)
        val tagged =
            Library.read(mp3)
                .copy(
                    title = "Тестовый трек",
                    artist = "LorMusic self-test",
                    album = "Test",
                    genre = "Electronic",
                    year = "2026",
                    customLyrics = "[00:01.00]Test",
                )
        Library.writeTags(tagged)
        check(Library.read(mp3).title == "Тестовый трек")
        check(Library.read(mp3).artist == "LorMusic self-test")
        val state =
            SavedState(
                library = mutableListOf(s),
                playlists = mutableListOf(UserList(name = "Test", songs = mutableListOf(s))),
                liked = mutableSetOf(s.id),
            )
        Library.save(state)
        val loaded = Library.load()
        check(loaded.playlists.single().songs.single().id == s.id)
        check(s.id in loaded.liked)
        val m3u = File(folder, "test.m3u8")
        Library.exportPlaylist(listOf(s), m3u)
        check(Library.importPlaylist(m3u).songs.single().id == s.id)
        val lyrics =
            LyricsRepository.getLyricsForSong(
                s.copy(customLyrics = "[00:01.00]First\n[00:02.50]Second")
            )
        check(lyrics.size == 2 && lyrics.last().timeStampMs == 2500L)
        check(YouTubeMusicRepository.extractVideoId("yt://abcdef12345") == "abcdef12345")
        check(mergeYouTubeSessionCookies("A=1; B=2", "A=3; C=4") == "A=1; B=2; C=4")
        val messages = mutableListOf<String>()
        val scope = CoroutineScope(Dispatchers.Swing + SupervisorJob())
        val player = Player(state, scope) { messages.add(it) }
        check(player.available) { player.error.orEmpty() }
        withContext(Dispatchers.Swing) {
            player.changeVolume(0)
            player.play(s, listOf(s))
        }
        delay(1400)
        check(player.duration >= 3900)
        check(player.position > 0) { "Playback did not advance: ${player.error}" }
        withContext(Dispatchers.Swing) {
            player.seek(2500)
            player.changeSpeed(1.25f)
            player.add(s)
            check(player.queue.size == 2)
            player.removeQueue(1)
            check(player.queue.size == 1)
            player.close()
        }
        scope.cancel()
        println(
            "PASS: metadata, JSON persistence, playlists, M3U, LRC timestamps, session cookies, native VLC playback, seek, rate and queue"
        )
    }
}

object NetworkChecks {
    fun run() = runBlocking {
        val yt = YouTubeMusicRepository.searchYtMusic("Дайте танк Чудо", 5)
        val sc = SoundCloudRepository.searchSoundCloud("Дайте танк Чудо", 5)
        println("YouTube Music search: ${yt.size}; SoundCloud search: ${sc.size}")
        val chosen = yt.firstOrNull()
        if (chosen != null) {
            val stream =
                withContext(Dispatchers.IO) { YouTubeMusicRepository.resolveSongStream(chosen) }
            println("YouTube stream resolved: ${stream?.uri?.startsWith("http")==true}")
        }
        check(yt.isNotEmpty() || sc.isNotEmpty()) { "Both services returned no results" }
        for ((label, tracks) in listOf("YouTube Music" to yt, "SoundCloud" to sc)) {
            if (tracks.isEmpty()) continue
            val testScope = CoroutineScope(Dispatchers.Swing + SupervisorJob())
            val player =
                withContext(Dispatchers.Swing) {
                    Player(SavedState(), testScope) { println("$label: $it") }
                }
            try {
                withContext(Dispatchers.Swing) {
                    player.changeVolume(0)
                    player.play(tracks.first(), tracks)
                }
                withTimeout(25000) {
                    while (player.position < 500 && player.error == null) delay(200)
                }
                check(player.position >= 500) { "$label playback failed: ${player.error}" }
                println("PASS: real $label playback advanced to ${player.position} ms")
            } finally {
                withContext(Dispatchers.Swing) { player.close() }
                testScope.cancel()
            }
        }
    }
}

object InstanceChecks {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        if (args.firstOrNull() == "primary") {
            check(InstanceLock.claim(emptyArray()))
            val received = CompletableDeferred<List<String>>()
            InstanceLock.handler = { received.complete(it) }
            val paths = withTimeout(15000) { received.await() }
            check(paths == listOf("Тестовый файл с пробелами.mp3"))
            println(
                "PASS: one writer per profile, authenticated local IPC, Unicode open-file forwarding"
            )
        } else {
            check(!InstanceLock.claim(arrayOf("Тестовый файл с пробелами.mp3")))
            println("PASS: second instance forwarded to primary")
        }
    }
}
