package com.lormusic.desktop

import com.lormusic.app.data.YouTubeMusicRepository
import java.io.File
import java.net.URI
import java.net.http.*
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import org.json.JSONObject

/** Sign-in only reads the app's dedicated profile, never the user's regular browser profile. */
object YouTubeBrowser {
    private var process: Process? = null
    private var job: Job? = null

    fun executable(): String? = DefaultBrowser.detect()?.takeIf { it.chromium }?.executable

    fun open(a: Actions) {
        val chosen = DefaultBrowser.detect()
        if (chosen != null && chosen.name.contains("firefox")) {
            FirefoxLogin.open(a, chosen)
            return
        }
        if (job?.isActive == true) {
            a.notify("Окно входа YouTube уже открыто")
            return
        }
        val exe =
            chosen?.takeIf { it.chromium }?.executable
                ?: run {
                    browse("https://music.youtube.com")
                    a.notify(
                        "YouTube Music открыт в браузере по умолчанию. После входа передайте Cookie через раздел «Аккаунты → Импорт сессии»: этот браузер не поддерживает автоматическую передачу сессии LorMusic."
                    )
                    return
                }
        job =
            a.scope.launch {
                try {
                    a.notify(
                        "Войдите в Google в браузере по умолчанию. Для LorMusic используется отдельный профиль входа; аккаунт подключится автоматически."
                    )
                    val cookie =
                        withContext(Dispatchers.IO) {
                            val profile = File(dataDir, "youtube-signin-profile").apply { mkdirs() }
                            File(profile, "DevToolsActivePort").delete()
                            process =
                                ProcessBuilder(
                                        exe,
                                        "--user-data-dir=${profile.absolutePath}",
                                        "--remote-debugging-port=0",
                                        "--remote-debugging-address=127.0.0.1",
                                        "--no-first-run",
                                        "--no-default-browser-check",
                                        "--new-window",
                                        "https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fmusic.youtube.com%2F",
                                    )
                                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                                    .start()
                            var result: String? = null
                            withTimeout(900000) {
                                val portFile = File(profile, "DevToolsActivePort")
                                while (!portFile.exists()) {
                                    check(process?.isAlive == true) { "Окно браузера закрылось" }
                                    delay(500)
                                }
                                val lines = portFile.readLines()
                                val port = lines.first().toInt()
                                require(port in 1..65535)
                                val endpoint = lines.getOrNull(1) ?: error("Missing endpoint")
                                require(endpoint.startsWith("/devtools/browser/"))
                                val client =
                                    HttpClient.newBuilder()
                                        .connectTimeout(Duration.ofSeconds(5))
                                        .build()
                                var id = 0
                                while (isActive && result == null) {
                                    val response = CompletableFuture<String>()
                                    val buffer = StringBuilder()
                                    val requestId = ++id
                                    val socket =
                                        client
                                            .newWebSocketBuilder()
                                            .connectTimeout(Duration.ofSeconds(5))
                                            .buildAsync(
                                                URI("ws://127.0.0.1:$port$endpoint"),
                                                object : WebSocket.Listener {
                                                    override fun onOpen(ws: WebSocket) {
                                                        ws.request(1)
                                                    }

                                                    override fun onText(
                                                        ws: WebSocket,
                                                        text: CharSequence,
                                                        last: Boolean,
                                                    ): java.util.concurrent.CompletionStage<*>? {
                                                        buffer.append(text)
                                                        if (last) {
                                                            val value = buffer.toString()
                                                            buffer.setLength(0)
                                                            if (
                                                                runCatching {
                                                                        JSONObject(value)
                                                                            .optInt("id") ==
                                                                            requestId
                                                                    }
                                                                    .getOrDefault(false)
                                                            )
                                                                response.complete(value)
                                                        }
                                                        ws.request(1)
                                                        return null
                                                    }

                                                    override fun onError(
                                                        ws: WebSocket,
                                                        error: Throwable,
                                                    ) {
                                                        response.completeExceptionally(error)
                                                    }
                                                },
                                            )
                                            .get(8, TimeUnit.SECONDS)
                                    try {
                                        socket
                                            .sendText(
                                                JSONObject()
                                                    .put("id", requestId)
                                                    .put("method", "Storage.getCookies")
                                                    .toString(),
                                                true,
                                            )
                                            .get(5, TimeUnit.SECONDS)
                                        val cookies =
                                            JSONObject(response.get(8, TimeUnit.SECONDS))
                                                .optJSONObject("result")
                                                ?.optJSONArray("cookies")
                                        val jar = linkedMapOf<String, String>()
                                        if (cookies != null)
                                            for (i in 0 until cookies.length()) {
                                                val c = cookies.getJSONObject(i)
                                                val domain =
                                                    c.optString("domain")
                                                        .removePrefix(".")
                                                        .lowercase()
                                                if (
                                                    domain == "youtube.com" ||
                                                        domain.endsWith(".youtube.com")
                                                )
                                                    jar[c.getString("name")] = c.getString("value")
                                            }
                                        val value =
                                            jar.entries.joinToString("; ") {
                                                "${it.key}=${it.value}"
                                            }
                                        if (YouTubeMusicRepository.hasValidAuthCookie(value))
                                            result = value
                                    } finally {
                                        socket.abort()
                                    }
                                    if (result == null) delay(1800)
                                }
                            }
                            requireNotNull(result)
                        }
                    a.state.youtubeCookie = cookie
                    YouTubeMusicRepository.currentAuthCookie = cookie
                    a.changed()
                    a.syncAccounts()
                    a.notify("YouTube Music подключён")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    a.notify(
                        "Вход YouTube: ${e.message?.take(160)}. Повторите вход или импортируйте Cookie в настройках."
                    )
                } finally {
                    withContext(NonCancellable + Dispatchers.IO) {
                        process?.destroy()
                        process = null
                    }
                }
            }
    }

    fun shutdown() {
        FirefoxLogin.shutdown()
        job?.cancel()
        process?.destroy()
        process = null
    }

    fun signOut() {
        FirefoxLogin.signOut()
        shutdown()
        File(dataDir, "youtube-signin-profile").deleteRecursively()
    }
}
