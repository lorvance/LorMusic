package com.lormusic.desktop

import java.io.File
import java.util.concurrent.TimeUnit

object DesktopMedia {
    fun vlcExecutable(): String {
        if (!System.getProperty("os.name").startsWith("Windows")) return "cvlc"
        val dirs =
            listOfNotNull(
                System.getProperty("compose.application.resources.dir"),
                System.getProperty("lormusic.home"),
                System.getProperty("user.dir"),
            )
        return dirs.map { File(it, "vlc/vlc.exe") }.firstOrNull { it.isFile }?.path ?: "vlc.exe"
    }

    fun convertToMp3(uri: String, out: File, expectedDuration: Long = 0) {
        val sout =
            "#transcode{vcodec=none,acodec=mp3,ab=320,channels=2,samplerate=44100}:std{access=file,mux=raw,dst=${out.path.replace("\\","/")}}"
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
