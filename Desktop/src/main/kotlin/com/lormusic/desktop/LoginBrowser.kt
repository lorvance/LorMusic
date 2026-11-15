package com.lormusic.desktop

import com.lormusic.app.data.YouTubeMusicRepository
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.*
import kotlinx.coroutines.*
import me.friwi.jcefmaven.CefAppBuilder
import org.cef.CefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceRequestHandler
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefCookieManager
import org.cef.network.CefRequest

/** Dedicated sign-in browser. Cookies are read only for the selected music service. */
object LoginBrowser {
    @Volatile private var app: CefApp? = null
    private var frame: JFrame? = null

    @Synchronized
    fun engine(progress: ((String) -> Unit)? = null): CefApp {
        app?.let {
            return it
        }
        val builder = CefAppBuilder()
        builder.setInstallDir(File(dataDir, "chromium"))
        builder.setProgressHandler { stage, percent ->
            progress?.invoke(
                "Chromium: $stage ${if(percent>=0)percent.toInt().toString()+"%" else ""}"
            )
        }
        builder.cefSettings.apply {
            windowless_rendering_enabled = false
            cache_path = File(dataDir, "browser-profile/default").path
            root_cache_path = File(dataDir, "browser-profile").path
            persist_session_cookies = true
            log_file = File(dataDir, "browser.log").path
            log_severity = org.cef.CefSettings.LogSeverity.LOGSEVERITY_DISABLE
        }
        builder.addJcefArgs("--disable-gpu", "--disable-features=MediaRouter")

        return builder.build().also { app = it }
    }

    fun open(a: Actions, youtube: Boolean) {
        frame?.let {
            if (it.isDisplayable) {
                it.toFront()
                return
            }
        }
        val title = if (youtube) "YouTube Music" else "SoundCloud"
        val f = JFrame("LorMusic · $title")
        frame = f
        f.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        f.size = Dimension(1024, 760)
        f.setLocationRelativeTo(null)
        val status =
            JLabel("Подготавливаю окно входа. При первом запуске загружается Chromium…").apply {
                foreground = Color(230, 220, 242)
                border = BorderFactory.createEmptyBorder(14, 18, 14, 18)
            }
        val connect =
            JButton("Подключить аккаунт").apply {
                isEnabled = false
                background = Color(195, 165, 255)
                foreground = Color(40, 27, 52)
            }
        val top =
            JPanel(BorderLayout()).apply {
                background = Color(37, 31, 45)
                add(status, BorderLayout.CENTER)
                add(connect, BorderLayout.EAST)
            }
        f.add(top, BorderLayout.NORTH)
        val loading = JProgressBar().apply { isIndeterminate = true }
        f.add(loading, BorderLayout.SOUTH)
        f.isVisible = true
        a.scope.launch {
            try {
                val engine =
                    withContext(Dispatchers.IO) {
                        engine { message -> SwingUtilities.invokeLater { status.text = message } }
                    }
                if (!f.isDisplayable) return@launch
                val client = engine.createClient()
                val completed = AtomicBoolean(false)
                var browser: CefBrowser? = null
                fun submit(value: String) {
                    if (!f.isDisplayable) return
                    if (!completed.compareAndSet(false, true)) return
                    SwingUtilities.invokeLater {
                        if (youtube) a.state.youtubeCookie = value
                        else a.state.soundcloudToken = value
                        a.changed()
                        a.syncAccounts()
                        f.dispose()
                    }
                }
                client.addRequestHandler(
                    object : CefRequestHandlerAdapter() {
                        override fun getResourceRequestHandler(
                            b: CefBrowser?,
                            fr: CefFrame?,
                            request: CefRequest?,
                            navigation: Boolean,
                            download: Boolean,
                            initiator: String?,
                            disable: BoolRef?,
                        ): CefResourceRequestHandler? {
                            if (youtube) return null
                            return object : CefResourceRequestHandlerAdapter() {
                                override fun onBeforeResourceLoad(
                                    b: CefBrowser?,
                                    fr: CefFrame?,
                                    request: CefRequest?,
                                ): Boolean {
                                    val host =
                                        runCatching { URI(request?.url.orEmpty()).host }
                                            .getOrNull()
                                            .orEmpty()
                                    if (
                                        host == "api-v2.soundcloud.com" ||
                                            host == "api.soundcloud.com"
                                    ) {
                                        val headers = hashMapOf<String, String>()
                                        request?.getHeaderMap(headers)
                                        headers.entries
                                            .firstOrNull { it.key.equals("Authorization", true) }
                                            ?.value
                                            ?.removePrefix("OAuth ")
                                            ?.takeIf { it.length > 8 && it != "guest" }
                                            ?.let { submit(it) }
                                    }
                                    return false
                                }
                            }
                        }
                    }
                )
                client.addLifeSpanHandler(
                    object : CefLifeSpanHandlerAdapter() {
                        override fun onBeforePopup(
                            b: CefBrowser?,
                            fr: CefFrame?,
                            url: String?,
                            name: String?,
                        ): Boolean {
                            url?.let { b?.loadURL(it) }
                            return true
                        }
                    }
                )
                browser =
                    client.createBrowser(
                        if (youtube) "https://music.youtube.com"
                        else "https://soundcloud.com/signin",
                        false,
                        false,
                    )
                val poll =
                    Timer(1800) {
                        val jar = linkedMapOf<String, String>()
                        CefCookieManager.getGlobalManager().visitUrlCookies(
                            if (youtube) "https://music.youtube.com" else "https://soundcloud.com",
                            true,
                        ) { cookie, count, total, _ ->
                            jar[cookie.name] = cookie.value
                            if (!youtube && cookie.name == "oauth_token" && cookie.value.length > 8)
                                submit(cookie.value)
                            if (youtube && count == total - 1) {
                                val cookies =
                                    jar.entries.joinToString("; ") { "${it.key}=${it.value}" }
                                if (YouTubeMusicRepository.hasValidAuthCookie(cookies))
                                    submit(cookies)
                            }
                            true
                        }
                    }
                connect.addActionListener {
                    poll.actionListeners.forEach { it.actionPerformed(null) }
                }
                f.addWindowListener(
                    object : WindowAdapter() {
                        override fun windowClosed(e: WindowEvent?) {
                            poll.stop()
                            browser.close(true)
                            client.dispose()
                            if (frame === f) frame = null
                        }
                    }
                )
                f.remove(loading)
                f.add(browser.uiComponent, BorderLayout.CENTER)
                status.text = "Войдите в $title. LorMusic подключит аккаунт автоматически."
                connect.isEnabled = true
                f.revalidate()
                f.repaint()
                poll.start()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                status.text = "Не удалось открыть браузер. Доступен ручной импорт сессии."
                f.remove(loading)
                f.revalidate()
                a.notify("Окно входа: ${e.message?.take(180)}")
            }
        }
    }

    fun signOut() {
        frame?.dispose()
        if (app != null) {
            CefCookieManager.getGlobalManager().deleteCookies(null, null)
            CefCookieManager.getGlobalManager().flushStore(null)
        } else File(dataDir, "browser-profile").deleteRecursively()
    }

    fun shutdown() {
        frame?.dispose()
        app?.dispose()
        app = null
    }
}
