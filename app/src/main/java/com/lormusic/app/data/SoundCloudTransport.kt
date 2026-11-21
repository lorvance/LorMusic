package com.lormusic.app.data

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject

/** Public catalog/streams remain usable when an optional account session expires. */
object SoundCloudTransport {
    const val USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"

    fun open(
        url: String,
        token: String? = null,
        connectTimeout: Int = 10000,
        readTimeout: Int = 15000,
    ): HttpURLConnection {
        fun connection(auth: String?): HttpURLConnection =
            (URL(url).openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json, text/plain, */*")
                setRequestProperty("Origin", "https://soundcloud.com")
                setRequestProperty("Referer", "https://soundcloud.com/")
                if (!auth.isNullOrBlank() && auth != "guest")
                    setRequestProperty("Authorization", "OAuth $auth")
                this.connectTimeout = connectTimeout
                this.readTimeout = readTimeout
            }
        val auth = token?.takeIf { it.isNotBlank() && it != "guest" }
        val first = connection(auth)
        if (auth != null && first.responseCode in listOf(401, 403)) {
            first.disconnect()
            return connection(null)
        }
        return first
    }

    fun fetch(url: String, token: String?): JSONObject {
        val c = open(url, token)
        try {
            require(c.responseCode in 200..299) { "SoundCloud HTTP ${c.responseCode}" }
            return JSONObject(c.inputStream.bufferedReader().use { it.readText() })
        } finally {
            c.disconnect()
        }
    }

    fun transcodings(track: JSONObject, download: Boolean): List<String> {
        val all = track.optJSONObject("media")?.optJSONArray("transcodings") ?: return emptyList()
        return (0 until all.length())
            .mapNotNull { all.optJSONObject(it) }
            .filter { t ->
                val mime = t.optJSONObject("format")?.optString("mime_type").orEmpty()
                !download ||
                    (!t.optBoolean("snipped", false) &&
                        (mime.startsWith("audio/mpeg") ||
                            mime.startsWith("audio/mp4") ||
                            mime.startsWith("audio/aac")))
            }
            .sortedBy {
                if (it.optJSONObject("format")?.optString("protocol") == "progressive") 0 else 1
            }
            .mapNotNull { it.optString("url").takeIf { u -> u.startsWith("https://") } }
    }

    fun resolve(
        id: String,
        clientId: () -> String,
        refresh: () -> Unit,
        token: () -> String?,
        download: Boolean = false,
    ): String? {
        repeat(2) { attempt ->
            val cid = clientId()
            try {
                val track =
                    fetch("https://api-v2.soundcloud.com/tracks/$id?client_id=$cid", token())
                val auth = track.optString("track_authorization").takeIf { it.isNotBlank() }
                for (tc in transcodings(track, download)) {
                    val query =
                        (if (tc.contains('?')) "&" else "?") +
                            "client_id=$cid" +
                            (auth?.let { "&track_authorization=" + URLEncoder.encode(it, "UTF-8") }
                                ?: "")
                    val result =
                        runCatching {
                                fetch(
                                        tc + query,
                                        token().takeIf {
                                            URL(tc).host.lowercase() in
                                                listOf(
                                                    "api-v2.soundcloud.com",
                                                    "api.soundcloud.com",
                                                )
                                        },
                                    )
                                    .optString("url")
                            }
                            .getOrNull()
                    if (!result.isNullOrBlank() && result.startsWith("https://")) return result
                }
            } catch (_: Exception) {}
            if (attempt == 0) refresh()
        }
        return null
    }
}
