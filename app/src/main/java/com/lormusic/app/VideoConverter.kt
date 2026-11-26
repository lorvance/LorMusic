package com.lormusic.app

import android.content.Context
import android.media.*
import android.net.Uri
import java.io.File
import java.io.RandomAccessFile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Decode the first audio stream to lossless PCM and store editable RIFF INFO tags. */
internal suspend fun convertVideo(context: Context, source: Uri, destination: Uri, tags: Map<String,String>, cover: Uri?, outputFormat: AudioExport, bitrate: Int) {
    require(source != destination) { "Выберите другой файл для результата" }
    val temp = File.createTempFile("audio-", ".wav", context.cacheDir)
    val extractor = MediaExtractor()
    try {
        val artwork = cover?.let { uri ->
            requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                val bytes = java.io.ByteArrayOutputStream()
                val block = ByteArray(8192)
                while (true) {
                    val n = input.read(block)
                    if (n < 0) break
                    require(bytes.size() + n <= 12 * 1024 * 1024) { "Обложка должна быть меньше 12 МБ" }
                    bytes.write(block,0,n)
                }
                bytes.toByteArray()
            }
        }
        extractor.setDataSource(context, source, null)
        val track = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: error("В видео нет аудиодорожки")
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        try {
            codec.configure(format, null, null, 0)
            codec.start()
            RandomAccessFile(temp, "rw").use { out ->
                fun le(value: Long, bytes: Int) { repeat(bytes) { out.write((value ushr (it * 8)).toInt()) } }
                out.seek(44)
                var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                val info = MediaCodec.BufferInfo()
                var eof = false
                while (true) {
                    currentCoroutineContext().ensureActive()
                    if (!eof) {
                        val slot = codec.dequeueInputBuffer(10000)
                        if (slot >= 0) {
                            val buffer = codec.getInputBuffer(slot)!!
                            val size = extractor.readSampleData(buffer, 0)
                            eof = size < 0
                            codec.queueInputBuffer(slot, 0, size.coerceAtLeast(0), if (eof) 0 else extractor.sampleTime, if (eof) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                            if (!eof) extractor.advance()
                        }
                    }
                    val slot = codec.dequeueOutputBuffer(info, 10000)
                    if (slot == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val pcm = codec.outputFormat
                        rate = pcm.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = pcm.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        require(!pcm.containsKey(MediaFormat.KEY_PCM_ENCODING) || pcm.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_16BIT) { "Неподдерживаемая разрядность PCM" }
                    }
                    if (slot >= 0) {
                        if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            val buffer = codec.getOutputBuffer(slot)!!
                            buffer.position(info.offset); buffer.limit(info.offset + info.size)
                            while (buffer.hasRemaining()) out.channel.write(buffer)
                            require(out.filePointer < 0xffff0000L) { "Аудиофайл превышает лимит WAV" }
                        }
                        codec.releaseOutputBuffer(slot, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
                val audioBytes = out.filePointer - 44
                check(audioBytes > 0) { "Пустая аудиодорожка" }
                val list = out.filePointer
                out.writeBytes("LIST"); le(0,4); out.writeBytes("INFO")
                for ((id, text) in tags.filter { it.key in setOf("INAM", "IART", "IPRD", "IGNR", "ICRD", "ITRK", "ICMT", "ICOP") && it.value.isNotBlank() }) {
                    val data = text.toByteArray(Charsets.UTF_8)
                    out.writeBytes(id); le(data.size + 1L,4); out.write(data); out.write(0)
                    if (data.size % 2 == 0) out.write(0)
                }
                val listEnd = out.filePointer
                val id3 = waveId3(tags, artwork, cover?.let { context.contentResolver.getType(it) })
                out.writeBytes("id3 "); le(id3.size.toLong(),4); out.write(id3)
                if (id3.size % 2 != 0) out.write(0)
                val end = out.filePointer
                out.seek(list + 4); le(listEnd - list - 8,4)
                out.seek(0); out.writeBytes("RIFF"); le(end - 8,4); out.writeBytes("WAVEfmt ")
                le(16,4); le(1,2); le(channels.toLong(),2); le(rate.toLong(),4)
                le(rate.toLong()*channels*2,4); le(channels*2L,2); le(16,2)
                out.writeBytes("data"); le(audioBytes,4)
            }
        } finally { codec.release() }
        currentCoroutineContext().ensureActive()
        val result = if (outputFormat == AudioExport.WAV) temp else File.createTempFile("audio-", ".m4a", context.cacheDir)
        try {
            if (outputFormat == AudioExport.AAC) {
                encodeAac(temp, result, bitrate)
                if (tags.values.any { it.isNotBlank() } || artwork != null) {
                    require(result.length() < Runtime.getRuntime().maxMemory() / 8) { "Файл слишком большой для записи тегов M4A. Сохраните его без тегов или выберите WAV." }
                    com.lormusic.app.data.YouTubeMusicRepository.injectM4aMetadataAtoms(
                        result, tags["INAM"].orEmpty(), tags["IART"].orEmpty(), tags["IPRD"].orEmpty(),
                        tags["IGNR"].orEmpty(), tags["ICRD"].orEmpty(), artwork, tags
                    )
                }
            }
            currentCoroutineContext().ensureActive()
            requireNotNull(context.contentResolver.openOutputStream(destination, "wt")).use { output ->
                result.inputStream().use { it.copyTo(output) }
            }
        } finally {
            if (result != temp) result.delete()
        }
    } finally { extractor.release(); temp.delete() }
}

internal enum class AudioExport(val extension: String, val mime: String) {
    WAV("wav", "audio/wav"),
    AAC("m4a", "audio/mp4"),
}

internal suspend fun encodeAac(wave: File, output: File, bitrate: Int) {
    RandomAccessFile(wave, "r").use { pcm ->
        pcm.seek(22)
        val channels = java.lang.Short.reverseBytes(pcm.readShort()).toInt()
        val rate = Integer.reverseBytes(pcm.readInt())
        pcm.seek(40)
        var remaining = Integer.toUnsignedLong(Integer.reverseBytes(pcm.readInt()))
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, rate, channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        try {
            val muxer = MediaMuxer(output.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            try {
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                codec.start()
                val info = MediaCodec.BufferInfo()
                val buffer = ByteArray(16384)
                var sent = 0L
                var eof = false
                var track = -1
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val input = if (eof) -1 else codec.dequeueInputBuffer(10000)
                    if (input >= 0) {
                        val target = checkNotNull(codec.getInputBuffer(input))
                        target.clear()
                        val frameSize = channels * 2
                        val length = minOf(buffer.size.toLong(), target.remaining().toLong(), remaining).toInt().let { it - it % frameSize }
                        check(length > 0 || remaining == 0L) { "Неполный кадр PCM" }
                        if (length > 0) {
                            pcm.readFully(buffer, 0, length)
                            target.put(buffer, 0, length)
                        }
                        eof = length == 0
                        codec.queueInputBuffer(input, 0, length, sent / frameSize * 1_000_000 / rate,
                            if (eof) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                        sent += length
                        remaining -= length
                    }
                    val slot = codec.dequeueOutputBuffer(info, 10000)
                    if (slot == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        track = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                    } else if (slot >= 0) {
                        if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            muxer.writeSampleData(track, checkNotNull(codec.getOutputBuffer(slot)), info)
                        }
                        codec.releaseOutputBuffer(slot, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
                muxer.stop()
            } finally {
                muxer.release()
            }
        } finally {
            codec.release()
        }
    }
}
