package com.lormusic.desktop

import java.io.File
import java.util.concurrent.TimeUnit

object DesktopMedia {
    fun vlcExecutable(): String {
        if (!System.getProperty("os.name").startsWith("Windows")) return "vlc"
        val dirs =
            listOfNotNull(
                System.getProperty("compose.application.resources.dir"),
                System.getProperty("lormusic.home"),
                System.getProperty("user.dir"),
            )
        return dirs.map { File(it, "vlc/vlc.exe") }.firstOrNull { it.isFile }?.path ?: "vlc.exe"
    }

    fun convertToMp3(uri: String, out: File, expectedDuration: Long = 0) {
        convertAudio(uri, out, "mp3", expectedDuration)
    }

    fun convertAudio(uri: String, out: File, extension: String, expectedDuration: Long = 0) {
        val (codec, bitrate, mux) = when (extension) {
            "mp3" -> Triple("mp3", 320, "raw")
            "m4a" -> Triple("mp4a", 160, "mp4")
            else -> error("Unsupported format: $extension")
        }
        val destination = out.absolutePath.replace('\\', '/')
        val sout = "#transcode{vcodec=none,acodec=$codec,ab=$bitrate,channels=2,samplerate=44100}:std{access=file,mux=$mux,dst=$destination}"
        val log = File(dataDir, "transcode.log")
        val process =
            ProcessBuilder(
                    vlcExecutable(),
                    "-I",
                    "dummy",
                    "--quiet",
                    "--no-video",
                    "--no-one-instance",
                    "--http-user-agent=Mozilla/5.0",
                    "--sout=$sout",
                    uri,
                    "vlc://quit",
                )
                .redirectError(log)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start()
        val timeout = ((expectedDuration / 1000) * 2 + 90).coerceIn(120, 1800)
        try {
            if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                error("Истекло время скачивания")
            }
            require(out.isFile && out.length() > 1024) {
                "Не удалось сохранить поток. ${log.readText().takeLast(250)}"
            }
        } finally {
            if (process.isAlive) process.destroyForcibly()
        }
    }
}
