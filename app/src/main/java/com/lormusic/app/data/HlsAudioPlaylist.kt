package com.lormusic.app.data

import java.net.URI

/** HLS addresses are resolved against the playlist, including ../ paths and signed queries. */
data class HlsByteRange(val length: Long, val offset: Long)

data class HlsAudioPart(val url: String, val range: HlsByteRange? = null)

data class HlsAudioPlaylist(
    val parts: List<HlsAudioPart>,
    val durationMs: Long,
    val childPlaylist: String? = null,
) {
    companion object {
        fun parse(baseUrl: String, text: String): HlsAudioPlaylist {
            require(text.trimStart().startsWith("#EXTM3U")) { "Invalid HLS playlist" }
            val base = URI(baseUrl)
            fun url(value: String) = base.resolve(value).toString()
            fun attribute(line: String, key: String): String? =
                Regex("(?:^|,)" + key + "=(?:\"([^\"]*)\"|([^,]*))")
                    .find(line.substringAfter(':'))
                    ?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } }
            val parts = mutableListOf<HlsAudioPart>()
            var duration = 0.0
            var pendingRange: String? = null
            var lastRangeUrl: String? = null
            var rangeEnd = 0L
            var variant = false
            var initialization: HlsAudioPart? = null
            for (line in text.lineSequence().map { it.trim() }.filter { it.isNotBlank() }) {
                when {
                    line.startsWith("#EXT-X-KEY:") ->
                        require(attribute(line, "METHOD") == "NONE") {
                            "Encrypted HLS is not supported for download"
                        }
                    line.startsWith("#EXT-X-STREAM-INF:") -> variant = true
                    line.startsWith("#EXTINF:") -> {
                        val seconds =
                            line.substringAfter(':').substringBefore(',').toDoubleOrNull()
                                ?: error("Invalid HLS duration")
                        require(seconds.isFinite() && seconds >= 0)
                        duration += seconds
                    }
                    line.startsWith("#EXT-X-BYTERANGE:") -> pendingRange = line.substringAfter(':')
                    line.startsWith("#EXT-X-MAP:") -> {
                        val address =
                            url(attribute(line, "URI") ?: error("Missing HLS initialization URI"))
                        val range = attribute(line, "BYTERANGE")?.let { parseRange(it, 0) }
                        initialization = HlsAudioPart(address, range)
                    }
                    !line.startsWith("#") -> {
                        if (variant) return HlsAudioPlaylist(emptyList(), 0, url(line))
                        val address = url(line)
                        if (initialization != null && parts.lastOrNull() != initialization) {
                            if (parts.isEmpty() || parts.none { it == initialization })
                                parts.add(initialization!!)
                        }
                        val range =
                            pendingRange?.let { spec ->
                                if (!spec.contains('@'))
                                    require(lastRangeUrl == address) {
                                        "Missing previous HLS byte range"
                                    }
                                parseRange(spec, rangeEnd)
                            }
                        parts.add(HlsAudioPart(address, range))
                        pendingRange = null
                        if (range != null) {
                            lastRangeUrl = address
                            rangeEnd = range.offset + range.length
                        } else {
                            lastRangeUrl = null
                            rangeEnd = 0
                        }
                    }
                }
            }
            require(parts.isNotEmpty()) { "Empty HLS playlist" }
            return HlsAudioPlaylist(parts, (duration * 1000).toLong())
        }

        private fun parseRange(value: String, previousEnd: Long): HlsByteRange {
            val length = value.substringBefore('@').toLong()
            val offset = value.substringAfter('@', "").toLongOrNull() ?: previousEnd
            require(length > 0 && offset >= 0 && offset <= Long.MAX_VALUE - length) {
                "Invalid HLS byte range"
            }
            return HlsByteRange(length, offset)
        }
    }
}
