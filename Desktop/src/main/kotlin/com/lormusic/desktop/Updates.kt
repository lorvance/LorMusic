package com.lormusic.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import java.net.URI
import java.net.http.*
import java.security.MessageDigest
import java.time.Duration
import kotlinx.coroutines.*
import org.json.*

const val DESKTOP_VERSION = "1.8.3"
fun currentAppImage():String? = System.getenv("LORMUSIC_APPIMAGE")?.takeIf{it.isNotBlank() && it == System.getenv("APPIMAGE")}

data class DesktopRelease(
    val version: String,
    val name: String,
    val url: String,
    val size: Long,
    val sha256: String?,
    val notes: String,
)

object DesktopUpdates {
    private val client =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build()

    fun newer(a: String, b: String): Boolean {
        fun parts(s: String) =
            Regex("\\d+(?:\\.\\d+)+").find(s)?.value?.split('.')?.map { it.toInt() } ?: emptyList()
        val x = parts(a)
        val y = parts(b)
        if (x.isEmpty()) return false
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
            if (d != 0) return d > 0
        }
        return false
    }

    fun select(
        json: String,
        windows: Boolean,
        current: String = DESKTOP_VERSION,
        appImage: Boolean = currentAppImage()!=null,
    ): DesktopRelease? {
        val result = mutableListOf<DesktopRelease>()
        val releases = JSONArray(json)
        for (i in 0 until releases.length()) {
            val r = releases.getJSONObject(i)
            if (r.optBoolean("draft") || r.optBoolean("prerelease")) continue
            val assets = r.optJSONArray("assets") ?: continue
            for (j in 0 until assets.length()) {
                val a = assets.getJSONObject(j)
                val name = a.optString("name")
                val lower = name.lowercase()
                if (
                    !lower.startsWith("lormusic-") ||
                        !(if (windows)
                            lower.contains("windows") &&
                                lower.contains("x64") &&
                                lower.endsWith(".exe")
                        else
                            lower.contains("linux") &&
                                (if (appImage)
                                    lower.contains("x86_64") && lower.endsWith(".appimage")
                                else lower.contains("amd64") && lower.endsWith(".deb")))
                )
                    continue
                val version = Regex("\\d+\\.\\d+\\.\\d+(?:\\.\\d+)?").find(name)?.value ?: continue
                val url = a.optString("browser_download_url")
                if (
                    !url.startsWith("https://github.com/lorvance/LorMusic/releases/download/", true)
                )
                    continue
                if (newer(version, current))
                    result +=
                        DesktopRelease(
                            version,
                            name,
                            url,
                            a.optLong("size"),
                            a.optString("digest")
                                .takeIf { it.matches(Regex("sha256:[a-fA-F0-9]{64}")) }
                                ?.substringAfter(':'),
                            r.optString("body"),
                        )
            }
        }
        return result.maxWithOrNull(
            Comparator { x, y ->
                if (newer(x.version, y.version)) 1 else if (newer(y.version, x.version)) -1 else 0
            }
        )
    }

    suspend fun check(): DesktopRelease? =
        withContext(Dispatchers.IO) {
            val request =
                HttpRequest.newBuilder(
                        URI("https://api.github.com/repos/lorvance/LorMusic/releases?per_page=100")
                    )
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "LorMusic-Desktop/$DESKTOP_VERSION")
                    .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            require(response.statusCode() == 200) { "GitHub: HTTP ${response.statusCode()}" }
            select(response.body(), System.getProperty("os.name").startsWith("Windows"))
        }

    suspend fun download(r: DesktopRelease, progress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            require(File(r.name).name == r.name && !r.name.contains('\\'))
            val dir = File(dataDir, "updates").apply { mkdirs() }
            val temp = File(dir, r.name + ".part")
            val final = File(dir, r.name)
            try {
                val response =
                    client.send(
                        HttpRequest.newBuilder(URI(r.url))
                            .timeout(Duration.ofMinutes(15))
                            .header("User-Agent", "LorMusic-Desktop/$DESKTOP_VERSION")
                            .build(),
                        HttpResponse.BodyHandlers.ofInputStream(),
                    )
                response.body().use { input ->
                    require(response.statusCode() == 200) {
                        "Download: HTTP ${response.statusCode()}"
                    }
                    temp.outputStream().use { out ->
                        val buffer = ByteArray(65536)
                        var count = 0L
                        while (true) {
                            ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            out.write(buffer, 0, n)
                            count += n
                            if (r.size > 0) progress((count.toFloat() / r.size).coerceIn(0f, 1f))
                        }
                        require(r.size <= 0 || count == r.size) { "Incomplete installer" }
                    }
                }
                r.sha256?.let { expected ->
                    val digest = MessageDigest.getInstance("SHA-256")
                    temp.inputStream().use { input ->
                        val b = ByteArray(65536)
                        while (true) {
                            val n = input.read(b)
                            if (n < 0) break
                            digest.update(b, 0, n)
                        }
                    }
                    require(
                        digest
                            .digest()
                            .joinToString("") { "%02x".format(it) }
                            .equals(expected, true)
                    ) {
                        "Installer checksum mismatch"
                    }
                }
                java.nio.file.Files.move(
                    temp.toPath(),
                    final.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                )
                final
            } finally {
                temp.delete()
            }
        }
}

class UpdateController {
    var release by mutableStateOf<DesktopRelease?>(null)
    var status by mutableStateOf("")
    var busy by mutableStateOf(false)
    var progress by mutableStateOf(0f)
    var installer by mutableStateOf<File?>(null)

    suspend fun check(a: Actions, manual: Boolean) {
        if (busy) return
        busy = true
        status = translate(a.state.language, "Проверяю GitHub…")
        try {
            val next = DesktopUpdates.check()
            if (next?.url != release?.url) installer = null
            release = next
            a.state.lastUpdateCheck = System.currentTimeMillis()
            a.changed()
            status =
                if (release == null) translate(a.state.language, "Установлена актуальная версия %s").format(DESKTOP_VERSION)
                else translate(a.state.language, "Доступна версия %s").format(release!!.version)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            status = translate(a.state.language, "Не удалось проверить обновления")
            if (manual) a.notify(status)
        } finally {
            busy = false
        }
    }
}

val updates = UpdateController()

@Composable
fun UpdateSettings(a: Actions) {
    PreferenceSwitch("Проверять обновления автоматически", a.state.autoUpdates) {
        a.state.autoUpdates = it
        a.changed()
    }
    Text(updates.status.ifBlank { t("Текущая версия %s").format(DESKTOP_VERSION) })
    TextButton({ a.scope.launch { updates.check(a, true) } }, enabled = !updates.busy) {
        Text(t("Проверить обновления"))
    }
}

@Composable
fun UpdateNotice(a: Actions) {
    LaunchedEffect(Unit) {
        while (true) {
            if (
                a.state.autoUpdates &&
                    System.currentTimeMillis() - a.state.lastUpdateCheck > 86400000L
            )
                updates.check(a, false)
            delay(3600000)
        }
    }
    val r = updates.release ?: return
    AlertDialog(
        onDismissRequest = { if (!updates.busy) updates.release = null },
        title = { Text("LorMusic ${r.version}") },
        text = {
            Column {
                Text(r.notes.take(1500))
                Spacer(Modifier.height(12.dp))
                Text(
                    if (updates.installer != null)
                        "Установщик готов. Сохраните работу и откройте его для обновления."
                    else "${r.name} · ${r.size/1048576} МБ"
                )
                if (updates.busy)
                    LinearProgressIndicator(
                        progress = updates.progress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                if (updates.status.isNotBlank()) Text(updates.status)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !updates.busy,
                onClick = {
                    val file = updates.installer
                    if (file != null) {
                        runCatching {
                                if (file.extension.equals("AppImage", true)) {
                                    val current = System.getenv("APPIMAGE")?.let { File(it) }
                                    if (
                                        current != null &&
                                            current.isFile &&
                                            current.parentFile.canWrite()
                                    ) {
                                        val staged = File(current.parentFile, current.name + ".new")
                                        file.copyTo(staged, overwrite = true)
                                        staged.setExecutable(true)
                                        java.nio.file.Files.move(
                                            staged.toPath(),
                                            current.toPath(),
                                            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                                            java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                                        )
                                        a.notify(
                                            "Обновление установлено. Закройте LorMusic и запустите AppImage заново."
                                        )
                                        updates.release = null
                                    } else {
                                        file.setExecutable(true)
                                        java.awt.Desktop.getDesktop().open(file.parentFile)
                                        a.notify("AppImage готов: ${file.absolutePath}")
                                    }
                                } else java.awt.Desktop.getDesktop().open(file)
                            }
                            .onFailure { a.notify("Откройте установщик: ${file.absolutePath}") }
                    } else
                        a.scope.launch {
                            updates.busy = true
                            try {
                                updates.installer =
                                    DesktopUpdates.download(r) { v ->
                                        a.scope.launch { updates.progress = v }
                                    }
                                updates.status = translate(a.state.language, "Загрузка завершена")
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                updates.status = translate(a.state.language, "Ошибка загрузки")
                            } finally {
                                updates.busy = false
                            }
                        }
                },
            ) {
                Text(t(if (updates.installer != null) "Открыть установщик" else "Скачать обновление"))
            }
        },
        dismissButton = {
            TextButton({ updates.release = null }, enabled = !updates.busy) { Text(t("Позже")) }
        },
    )
}
