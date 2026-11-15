package com.lormusic.app.data

/**
 * Inputs are cookie jars for music.youtube.com, www.youtube.com, then youtube.com, in that order.
 */
fun mergeYouTubeSessionCookies(vararg youtubeJars: String?): String {
    val values = linkedMapOf<String, String>()
    youtubeJars.forEach { jar ->
        jar.orEmpty().split(';').forEach { part ->
            val split = part.indexOf('=')
            if (split > 0) {
                val name = part.substring(0, split).trim()
                val value = part.substring(split + 1).trim()
                if (name.matches(Regex("[A-Za-z0-9_-]+")) && value.isNotEmpty())
                    values.putIfAbsent(name, value)
            }
        }
    }
    return values.entries.joinToString("; ") { "${it.key}=${it.value}" }
}

fun isYouTubeMusicPage(url: String?): Boolean =
    runCatching {
            val uri = java.net.URI(url.orEmpty())
            uri.scheme.equals("https", ignoreCase = true) &&
                uri.port in listOf(-1, 443) &&
                uri.host.equals("music.youtube.com", ignoreCase = true)
        }
        .getOrDefault(false)
