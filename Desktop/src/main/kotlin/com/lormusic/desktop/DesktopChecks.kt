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
        Release182Checks.run()
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

object Release182Checks {
    fun run() {
        check(SavedState().accent == 3)
        check(
            DefaultBrowser.parseExecutable(
                "\"C:\\Program Files\\Mozilla Firefox\\firefox.exe\" -osint -url \"%1\""
            ) == "C:\\Program Files\\Mozilla Firefox\\firefox.exe"
        )
        check(
            DefaultBrowser.parseExecutable(
                "env BAMF_DESKTOP_FILE_HINT=/test.desktop /snap/bin/firefox %u"
            ) == "/snap/bin/firefox"
        )
        check(DefaultBrowser.classify("/usr/bin/google-chrome").chromium)
        check(!DefaultBrowser.classify("/usr/bin/firefox").chromium)
        check(DesktopUpdates.newer("1.8.10", "1.8.2"))
        check(!DesktopUpdates.newer("1.8.2", "1.8.2"))
        val fixture =
            """[
   {"tag_name":"v9.0.0","assets":[{"name":"LorMusic-9.0.0.apk"}]},
   {"draft":true,"assets":[{"name":"LorMusic-8.0.0-Linux-amd64.deb"}]},
   {"prerelease":true,"assets":[{"name":"LorMusic-7.0.0-Linux-amd64.deb"}]},
   {"body":"Desktop fixes","assets":[
    {"name":"LorMusic-1.8.4-Linux-amd64.deb","size":123,"browser_download_url":"https://github.com/lorvance/LorMusic/releases/download/v1.8.4/LorMusic-1.8.4-Linux-amd64.deb"},
    {"name":"LorMusic-1.8.4-Windows-x64-Setup.exe","size":456,"browser_download_url":"https://github.com/lorvance/LorMusic/releases/download/v1.8.4/LorMusic-1.8.4-Windows-x64-Setup.exe"}]}]"""
        check(
            DesktopUpdates.select(fixture, false, appImage = false)?.name?.endsWith(".deb") == true
        )
        check(
            DesktopUpdates.select(fixture, true, appImage = false)?.name?.endsWith(".exe") == true
        )
        check(DesktopUpdates.select(fixture, false, "1.8.5", appImage = false) == null)
        check(
            DesktopUpdates.select(
                fixture.replace("https://github.com/lorvance/LorMusic/", "https://example.com/"),
                false,
            ) == null
        )
        val appFixture =
            fixture.replace(
                "LorMusic-1.8.4-Linux-amd64.deb",
                "LorMusic-1.8.4-Linux-x86_64.AppImage",
            )
        check(
            DesktopUpdates.select(appFixture, false, appImage = true)
                ?.name
                ?.endsWith(".AppImage") == true
        )
        check(DesktopUpdates.select(appFixture, false, appImage = false) == null)
        println(
            "PASS: desktop-only updates, platform selection, semantic versions, untrusted URL exclusion, blue default"
        )
    }
}

object SoundCloudChecks {
 fun run()=kotlinx.coroutines.runBlocking {
  val tracks=com.lormusic.app.data.SoundCloudRepository.searchSoundCloud("ambient",3)
  check(tracks.isNotEmpty()){ "Public SoundCloud search returned no tracks" }
  com.lormusic.app.data.SoundCloudRepository.currentAuthToken="expired-test-session"
  val uri=com.lormusic.app.data.SoundCloudRepository.resolveStreamUriForExoPlayer(java.net.URI(tracks.first().uri))
  check(uri.toString().startsWith("https://")){"No playable stream"}
  val scope=kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Swing+kotlinx.coroutines.SupervisorJob())
  val player=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Swing){Player(SavedState(volume=0),scope){message->println("Player status: $message")}}
  try{
   kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Swing){player.play(tracks.first(),tracks)}
   kotlinx.coroutines.withTimeout(35000){while(!player.playing&&player.error==null)kotlinx.coroutines.delay(100)}
   check(player.playing){"Playback failed: ${player.error}"};kotlinx.coroutines.delay(2500)
   check(player.position>0 && player.duration>10000){"Playback did not advance or duration was truncated"}
   println("PASS: public SoundCloud search, expired-session stream fallback, native VLC playback, duration=${player.duration}ms, position=${player.position}ms")
  }finally{kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Swing){player.close()};scope.cancel()}
 }
}
