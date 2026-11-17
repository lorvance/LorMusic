package com.lormusic.app

import com.lormusic.app.data.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class Release1811Test {
    private fun segment(count: Int): ByteArray {
        // MPEG-1 Layer III, 128 kbps, 44.1 kHz: one Xing header + count audio frames.
        val frameSize = 417
        val result = ByteArray(frameSize * (count + 1))
        repeat(count + 1) { i ->
            byteArrayOf(0xff.toByte(), 0xfb.toByte(), 0x90.toByte(), 0x00)
                .copyInto(result, i * frameSize)
        }
        "Xing".toByteArray().copyInto(result, 36)
        ByteBuffer.wrap(result, 40, 12)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(7)
            .putInt(count + 1)
            .putInt(result.size)
        return result
    }

    @Test
    fun concatenatedMp3HasFullDurationSeekHeaderAndEmbeddedCover() {
        val dir = Files.createTempDirectory("sc-mp3-test").toFile()
        try {
            val input = File(dir, "joined.mp3")
            input.writeBytes(segment(382) + segment(382) + segment(382))
            val output = File(dir, "fixed.mp3")
            val cover =
                byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0xe0.toByte(), 1, 2, 3, 4)
            val duration =
                Mp3DownloadMetadata.normalize(
                    input,
                    output,
                    Mp3DownloadMetadata.Tags("Чудо", "Артист", "Альбом", "Rock", "2026", cover),
                )
            assertTrue(
                "Must be about 30 seconds, not the first ten-second segment",
                duration in 29900..30100,
            )
            val bytes = output.readBytes()
            assertEquals("ID3", String(bytes, 0, 3))
            val id3Size = (6..9).fold(0) { acc, i -> (acc shl 7) or (bytes[i].toInt() and 127) }
            val audioStart = 10 + id3Size
            assertEquals("Xing", String(bytes, audioStart + 36, 4))
            val counters = ByteBuffer.wrap(bytes, audioStart + 44, 8).order(ByteOrder.BIG_ENDIAN)
            assertEquals(1147, counters.int)
            assertEquals(1147 * 417, counters.int)
            assertTrue(String(bytes, Charsets.ISO_8859_1).contains("APIC"))
            assertTrue(String(bytes, Charsets.ISO_8859_1).contains("TLEN"))
            assertTrue(
                kotlin.math.abs(Mp3DownloadMetadata.measuredDurationMs(output) - duration) < 2
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun hlsResolvesRelativeInitializationSegmentsAndByteRanges() {
        val p =
            HlsAudioPlaylist.parse(
                "https://cdn.example/music/lists/audio.m3u8?token=x",
                """#EXTM3U
#EXT-X-MAP:URI="../init.mp4"
#EXTINF:10.0,
../parts/one.m4s?signature=1
#EXTINF:10.25,
#EXT-X-BYTERANGE:80@20
../parts/two.m4s
#EXTINF:9.75,
#EXT-X-BYTERANGE:80
../parts/two.m4s
#EXT-X-ENDLIST
""",
            )
        assertEquals(30000L, p.durationMs)
        assertEquals(4, p.parts.size)
        assertEquals("https://cdn.example/music/init.mp4", p.parts[0].url)
        assertEquals("https://cdn.example/music/parts/one.m4s?signature=1", p.parts[1].url)
        assertEquals(HlsByteRange(80, 20), p.parts[2].range)
        assertEquals(HlsByteRange(80, 100), p.parts[3].range)
    }

    @Test
    fun hlsMasterReturnsPlaylistRatherThanDownloadingPlaylistTextAsAudio() {
        val p =
            HlsAudioPlaylist.parse(
                "https://cdn.example/master.m3u8",
                "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=128000\naudio/list.m3u8\n",
            )
        assertEquals("https://cdn.example/audio/list.m3u8", p.childPlaylist)
        assertTrue(p.parts.isEmpty())
    }

    @Test
    fun aacTimelineRemainsContinuousAcrossFragmentResets() {
        val timeline = AudioTimestampNormalizer(20000)
        val times =
            listOf(10000000L, 10020000L, 10040000L, 0L, 20000L, 40000L).map { timeline.next(it) }
        assertEquals(listOf(0L, 20000L, 40000L, 60000L, 80000L, 100000L), times)
        assertEquals(120L, timeline.durationMs)
    }

    @Test
    fun monochromeIconContainsAllFiveInnerMarks() {
        val file = File("src/main/res/drawable/ic_launcher_regular_mono.xml")
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val doc = factory.newDocumentBuilder().parse(file)
        val path = doc.getElementsByTagName("path").item(0)
        val data =
            path.attributes
                .getNamedItemNS("http://schemas.android.com/apk/res/android", "pathData")
                .nodeValue
        assertEquals("Flower plus three bars plus two rays", 6, Regex("[Mm]").findAll(data).count())
    }
}
