package com.lormusic.desktop

import com.lormusic.app.data.YouTubeMusicRepository
import java.io.File
import java.net.*
import java.net.http.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import org.json.JSONObject

/** Firefox's BiDi connection is confined to a separately created LorMusic profile. */
object FirefoxLogin {
    private var job: Job? = null
    private var process: Process? = null
    private var rpc: Rpc? = null

    fun open(a: Actions, browser: BrowserChoice) {
        if (job?.isActive == true) {
            a.notify("Окно входа уже открыто")
            return
        }
        job =
            a.scope.launch {
                try {
                    a.notify(
                        "Войдите в YouTube Music в браузере по умолчанию. LorMusic использует отдельный профиль входа."
                    )
                    val cookie =
                        withContext(Dispatchers.IO) {
                            val profile =
                                if (browser.executable.contains("/snap/"))
                                    File(
                                        System.getProperty("user.home"),
                                        "snap/firefox/common/lormusic-signin",
                                    )
                                else File(dataDir, "firefox-youtube-signin")
                            profile.mkdirs()
                            val port =
                                ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use {
                                    it.localPort
                                }
                            process =
                                ProcessBuilder(
                                        browser.executable,
                                        "--no-remote",
                                        "--new-instance",
                                        "--profile",
                                        profile.absolutePath,
                                        "--remote-debugging-port",
                                        port.toString(),
                                        "https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fmusic.youtube.com%2F",
                                    )
                                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                                    .start()
                            var channel: Rpc? = null
                            withTimeout(30000) {
                                while (channel == null) {
                                    channel =
                                        runCatching { Rpc("ws://127.0.0.1:$port/session") }
                                            .getOrNull()
                                    if (channel == null) delay(400)
                                }
                            }
                            rpc = requireNotNull(channel)
                            rpc!!.call(
                                "session.new",
                                JSONObject().put("capabilities", JSONObject()),
                            )
                            var result: String? = null
                            withTimeout(900000) {
                                while (result == null) {
                                    val rows =
                                        rpc!!
                                            .call("storage.getCookies", JSONObject())
                                            .getJSONObject("result")
                                            .getJSONArray("cookies")
                                    val jar = linkedMapOf<String, String>()
                                    for (i in 0 until rows.length()) {
                                        val c = rows.getJSONObject(i)
                                        val host =
                                            c.optString("domain").removePrefix(".").lowercase()
                                        if (
                                            host == "youtube.com" || host.endsWith(".youtube.com")
                                        ) {
                                            val value =
                                                c.optJSONObject("value")?.optString("value")
                                                    ?: c.optString("value")
                                            jar[c.getString("name")] = value
                                        }
                                    }
                                    val joined =
                                        jar.entries.joinToString("; ") { "${it.key}=${it.value}" }
                                    if (YouTubeMusicRepository.hasValidAuthCookie(joined))
                                        result = joined
                                    else delay(1800)
                                }
                            }
                            requireNotNull(result)
                        }
                    a.state.youtubeCookie = cookie
                    YouTubeMusicRepository.currentAuthCookie = cookie
                    a.changed()
                    a.syncAccounts()
                    a.notify("YouTube Music подключён")
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    a.notify(
                        "Не удалось завершить вход в Firefox. Повторите вход или используйте импорт Cookie в разделе «Аккаунты»."
                    )
                } finally {
                    withContext(NonCancellable + Dispatchers.IO) {
                        runCatching { rpc?.call("browser.close", JSONObject()) }
                        rpc?.close()
                        rpc = null
                        process?.destroy()
                        process = null
                    }
                }
            }
    }

    fun shutdown() {
        job?.cancel()
    }

    fun signOut() {
        shutdown()
        val clean = {
            listOf(
                    File(dataDir, "firefox-youtube-signin"),
                    File(System.getProperty("user.home"), "snap/firefox/common/lormusic-signin"),
                )
                .forEach { it.deleteRecursively() }
            Unit
        }
        val active = job
        if (active != null && !active.isCompleted) active.invokeOnCompletion { clean() }
        else clean()
    }

    private class Rpc(url: String) {
        private val ids = AtomicInteger()
        private val pending = ConcurrentHashMap<Int, CompletableFuture<JSONObject>>()
        private val buffer = StringBuilder()
        private val socket =
            HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(2))
                .buildAsync(
                    URI(url),
                    object : WebSocket.Listener {
                        override fun onOpen(ws: WebSocket) {
                            ws.request(1)
                        }

                        override fun onText(
                            ws: WebSocket,
                            data: CharSequence,
                            last: Boolean,
                        ): CompletionStage<*>? {
                            buffer.append(data)
                            if (last) {
                                val json = runCatching { JSONObject(buffer.toString()) }.getOrNull()
                                buffer.setLength(0)
                                json?.let { pending.remove(it.optInt("id"))?.complete(it) }
                            }
                            ws.request(1)
                            return null
                        }

                        override fun onError(ws: WebSocket, error: Throwable) {
                            pending.values.forEach { it.completeExceptionally(error) }
                        }
                    },
                )
                .get(3, TimeUnit.SECONDS)

        fun call(method: String, params: JSONObject): JSONObject {
            val id = ids.incrementAndGet()
            val reply = CompletableFuture<JSONObject>()
            pending[id] = reply
            try {
                socket
                    .sendText(
                        JSONObject()
                            .put("id", id)
                            .put("method", method)
                            .put("params", params)
                            .toString(),
                        true,
                    )
                    .get(3, TimeUnit.SECONDS)
                val result = reply.get(8, TimeUnit.SECONDS)
                require(result.optString("type") != "error") { "Browser command failed" }
                return result
            } finally {
                pending.remove(id)
            }
        }

        fun close() {
            socket.abort()
        }
    }
}
