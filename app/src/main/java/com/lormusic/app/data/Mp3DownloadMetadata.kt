package com.lormusic.app.data

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Rebuilds joined MP3 audio, instead of keeping a ten-second segment's Xing seek header. */
object Mp3DownloadMetadata {
    data class Tags(
        val title: String,
        val artist: String,
        val album: String,
        val genre: String = "",
        val year: String = "",
        val cover: ByteArray? = null,
    )

    private data class Frame(
        val offset: Long,
        val size: Int,
        val rate: Int,
        val samples: Int,
        val xingOffset: Int,
        val metadata: Boolean,
    )

    fun normalize(input: File, output: File, tags: Tags): Long {
        require(input.canonicalFile != output.canonicalFile)
        val frames = scan(input)
        require(frames.size > 1) { "Downloaded file contains no complete MP3 audio" }
        val firstMetadata = frames.firstOrNull { it.metadata }
        val kept = frames.filter { !it.metadata || it === firstMetadata }
        val byteCount = kept.sumOf { it.size.toLong() }
        require(byteCount <= 0xffffffffL)
        val duration = kept.sumOf { it.samples * 1000.0 / it.rate }.toLong()
        val offsets = LongArray(kept.size + 1)
        kept.forEachIndexed { i, f -> offsets[i + 1] = offsets[i] + f.size }
        output.outputStream().buffered().use { out ->
            out.write(id3(tags, duration))
            RandomAccessFile(input, "r").use { source ->
                kept.forEach { frame ->
                    val bytes = ByteArray(frame.size)
                    source.seek(frame.offset)
                    source.readFully(bytes)
                    if (frame === firstMetadata && frame.xingOffset + 116 <= bytes.size) {
                        val p = frame.xingOffset
                        "Xing".toByteArray(Charsets.ISO_8859_1).copyInto(bytes, p)
                        putInt(bytes, p + 4, 7)
                        putInt(bytes, p + 8, kept.size)
                        putInt(bytes, p + 12, byteCount.toInt())
                        for (percent in 0..99) {
                            val index =
                                (kept.size.toLong() * percent / 100)
                                    .toInt()
                                    .coerceAtMost(kept.lastIndex)
                            bytes[p + 16 + percent] =
                                (offsets[index] * 256 / byteCount).coerceIn(0, 255).toByte()
                        }
                    }
                    out.write(bytes)
                }
            }
        }
        require(output.length() > 0)
        return duration
    }

    fun measuredDurationMs(input: File): Long =
        scan(input).sumOf { it.samples * 1000.0 / it.rate }.toLong()

    private fun scan(input: File): List<Frame> {
        val frames = ArrayList<Frame>()
        val header = ByteArray(10)
        RandomAccessFile(input, "r").use { file ->
            var p = 0L
            val length = file.length()
            while (p + 4 <= length) {
                file.seek(p)
                val n = file.read(header)
                if (
                    n >= 10 &&
                        header[0] == 73.toByte() &&
                        header[1] == 68.toByte() &&
                        header[2] == 51.toByte()
                ) {
                    val bytes = header.sliceArray(6..9)
                    require(bytes.all { it.toInt() and 0x80 == 0 })
                    val size = bytes.fold(0) { acc, b -> (acc shl 7) or (b.toInt() and 127) }
                    p +=
                        10 +
                            size +
                            (if (header[3] == 4.toByte() && header[5].toInt() and 0x10 != 0) 10
                            else 0)
                    continue
                }
                val h = ByteBuffer.wrap(header, 0, 4).order(ByteOrder.BIG_ENDIAN).int
                val version = (h ushr 19) and 3
                val layer = (h ushr 17) and 3
                val bitrateIndex = (h ushr 12) and 15
                val rateIndex = (h ushr 10) and 3
                if (
                    h ushr 21 != 0x7ff ||
                        version == 1 ||
                        layer != 1 ||
                        bitrateIndex !in 1..14 ||
                        rateIndex == 3
                ) {
                    p++
                    continue
                }
                val rates = intArrayOf(44100, 48000, 32000)
                val rate =
                    rates[rateIndex] /
                        when (version) {
                            3 -> 1
                            2 -> 2
                            else -> 4
                        }
                val bitrate =
                    if (version == 3)
                        intArrayOf(
                            0,
                            32,
                            40,
                            48,
                            56,
                            64,
                            80,
                            96,
                            112,
                            128,
                            160,
                            192,
                            224,
                            256,
                            320,
                        )[bitrateIndex]
                    else
                        intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160)[
                            bitrateIndex]
                val size =
                    (if (version == 3) 144000 else 72000) * bitrate / rate + ((h ushr 9) and 1)
                if (p + size > length) break
                val mono = ((h ushr 6) and 3) == 3
                val crc = if (h and 0x10000 == 0) 2 else 0
                val xing =
                    4 +
                        crc +
                        if (version == 3) {
                            if (mono) 17 else 32
                        } else {
                            if (mono) 9 else 17
                        }
                val data = ByteArray(minOf(size, 160))
                file.seek(p)
                file.readFully(data)
                fun signature(at: Int, value: String) =
                    at + value.length <= data.size &&
                        String(data, at, value.length, Charsets.ISO_8859_1) == value
                val metadata =
                    signature(xing, "Xing") || signature(xing, "Info") || signature(36, "VBRI")
                frames.add(Frame(p, size, rate, if (version == 3) 1152 else 576, xing, metadata))
                p += size
            }
        }
        return frames
    }

    private fun putInt(bytes: ByteArray, offset: Int, value: Int) {
        ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.BIG_ENDIAN).putInt(value)
    }

    private fun id3(tags: Tags, duration: Long): ByteArray {
        val body = ByteArrayOutputStream()
        fun frame(name: String, data: ByteArray) {
            body.write(name.toByteArray(Charsets.ISO_8859_1))
            body.write(ByteBuffer.allocate(4).putInt(data.size).array())
            body.write(byteArrayOf(0, 0))
            body.write(data)
        }
        fun text(name: String, value: String) {
            if (value.isNotBlank()) frame(name, byteArrayOf(1) + value.toByteArray(Charsets.UTF_16))
        }
        text("TIT2", tags.title)
        text("TPE1", tags.artist)
        text("TALB", tags.album)
        text("TCON", tags.genre)
        text("TYER", tags.year)
        text("TRCK", "1")
        text("TLEN", duration.toString())
        tags.cover
            ?.takeIf { it.isNotEmpty() }
            ?.let { cover ->
                val mime =
                    if (cover.size >= 4 && cover[0] == 0x89.toByte() && cover[1] == 0x50.toByte())
                        "image/png"
                    else "image/jpeg"
                frame(
                    "APIC",
                    byteArrayOf(0) +
                        mime.toByteArray(Charsets.ISO_8859_1) +
                        byteArrayOf(0, 3, 0) +
                        cover,
                )
            }
        val payload = body.toByteArray()
        require(payload.size < 0x10000000)
        val size = payload.size
        return byteArrayOf(
            73,
            68,
            51,
            3,
            0,
            0,
            ((size ushr 21) and 127).toByte(),
            ((size ushr 14) and 127).toByte(),
            ((size ushr 7) and 127).toByte(),
            (size and 127).toByte(),
        ) + payload
    }
}
