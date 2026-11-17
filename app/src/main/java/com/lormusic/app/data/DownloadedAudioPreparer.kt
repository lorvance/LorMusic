package com.lormusic.app.data

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

class AudioTimestampNormalizer(private var stepUs: Long) {
    private var first: Long? = null
    private var previousRaw: Long? = null
    private var previous = -1L
    private var correction = 0L

    fun next(raw: Long): Long {
        val origin = first ?: raw.also { first = it }
        previousRaw?.let {
            val delta = raw - it
            if (delta in 1..999999) stepUs = delta
        }
        var time = raw - origin + correction
        if (time <= previous) {
            correction += previous + stepUs - time
            time = previous + stepUs
        }
        previous = time
        previousRaw = raw
        return time
    }

    val durationMs: Long
        get() = if (previous < 0) 0 else (previous + stepUs) / 1000
}

data class PreparedDownload(
    val file: File,
    val durationMs: Long,
    val extension: String,
    val mime: String,
)

object DownloadedAudioPreparer {
    fun prepare(input: File, output: File, tags: Mp3DownloadMetadata.Tags): PreparedDownload {
        val probe = ByteArray(4)
        input.inputStream().use { it.read(probe) }
        val mp3 =
            (probe[0].toInt() and 255) == 255 &&
                (probe[1].toInt() and 0xe0) == 0xe0 &&
                ((probe[1].toInt() ushr 1) and 3) == 1
        if (mp3)
            return PreparedDownload(
                output,
                Mp3DownloadMetadata.normalize(input, output, tags),
                ".mp3",
                "audio/mpeg",
            )
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(input.absolutePath)
            val index =
                (0 until extractor.trackCount).firstOrNull {
                    extractor
                        .getTrackFormat(it)
                        .getString(MediaFormat.KEY_MIME)
                        ?.startsWith("audio/") == true
                } ?: error("No audio track in download")
            val format = extractor.getTrackFormat(index)
            if (format.getString(MediaFormat.KEY_MIME) == "audio/mpeg")
                return PreparedDownload(
                    output,
                    Mp3DownloadMetadata.normalize(input, output, tags),
                    ".mp3",
                    "audio/mpeg",
                )
            require(format.getString(MediaFormat.KEY_MIME) == "audio/mp4a-latm") {
                "Unsupported SoundCloud download format"
            }
            extractor.selectTrack(index)
            muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val track = muxer.addTrack(format)
            muxer.start()
            val rate =
                if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE))
                    format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                else 44100
            val timeline = AudioTimestampNormalizer(1024_000_000L / rate.coerceAtLeast(1))
            val max =
                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE))
                    format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                else 262144
            val buffer = ByteBuffer.allocate(max.coerceIn(262144, 2097152))
            val info = MediaCodec.BufferInfo()
            var samples = 0
            while (true) {
                buffer.clear()
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                require(extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_ENCRYPTED == 0) {
                    "Encrypted audio is not supported"
                }
                info.set(
                    0,
                    size,
                    timeline.next(extractor.sampleTime),
                    extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC,
                )
                muxer.writeSampleData(track, buffer, info)
                extractor.advance()
                samples++
            }
            require(samples > 0) { "Empty audio download" }
            muxer.stop()
            muxer.release()
            muxer = null
            YouTubeMusicRepository.injectM4aMetadataAtoms(
                output,
                tags.title,
                tags.artist,
                tags.album,
                tags.genre,
                tags.year,
                tags.cover,
            )
            return PreparedDownload(output, timeline.durationMs, ".m4a", "audio/mp4")
        } finally {
            runCatching { muxer?.release() }
            extractor.release()
        }
    }
}
