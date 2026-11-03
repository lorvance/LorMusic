package com.lormusic.app.data

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.zip.CRC32
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin

object ShazamRecognizer {

    private const val SAMPLE_RATE = 16000
    private const val FFT_SIZE = 2048
    private const val HOP_SIZE = 128
    private const val NUM_BINS = 1025

    private val hanningWindow = FloatArray(FFT_SIZE) { i ->
        (0.5f * (1.0f - cos(2.0 * Math.PI * (i + 1) / (FFT_SIZE + 1)))).toFloat()
    }

    data class RecognitionResult(
        val title: String,
        val artist: String,
        val album: String,
        val year: String = "",
        val genre: String = "",
        val coverUrl: String? = null
    )

    suspend fun recognizeAudioFile(context: Context, uriString: String): RecognitionResult? = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        val candidateOffsets = listOf(0.25f, 0.45f, 0.10f, 0.65f)

        for (offsetFraction in candidateOffsets) {
            val samples = decodeAudioSliceTo16kMono(context, uri, offsetFraction, 7)
            if (samples != null && samples.size >= SAMPLE_RATE * 3) {
                val signatureUri = generateShazamSignatureUri(samples)
                if (signatureUri != null) {
                    val result = sendShazamRecognizeRequest(signatureUri, (samples.size * 1000L) / SAMPLE_RATE)
                    if (result != null) {
                        return@withContext result
                    }
                }
            }
        }
        null
    }

    private fun decodeAudioSliceTo16kMono(
        context: Context,
        uri: Uri,
        offsetFraction: Float,
        durationSeconds: Int
    ): ShortArray? {
        return runCatching {
            val extractor = MediaExtractor()
            if (uri.scheme == "file") {
                extractor.setDataSource(uri.path ?: return null)
            } else if (uri.scheme == "content") {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
                pfd.use { fd ->
                    extractor.setDataSource(fd.fileDescriptor)
                }
            } else {
                val file = File(uri.path ?: return null)
                if (!file.exists()) return null
                extractor.setDataSource(file.absolutePath)
            }

            var audioTrackIndex = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    inputFormat = format
                    break
                }
            }
            if (audioTrackIndex < 0 || inputFormat == null) {
                extractor.release()
                return null
            }

            val totalDurationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) {
                inputFormat.getLong(MediaFormat.KEY_DURATION)
            } else 0L

            val seekTimeUs = (totalDurationUs * offsetFraction).toLong()
            extractor.selectTrack(audioTrackIndex)
            if (seekTimeUs > 0) {
                extractor.seekTo(seekTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            }

            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: ""
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            val rawPcmStream = ByteArrayOutputStream()
            var actualSampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var actualChannels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            var isEOS = false
            val maxBytesToRead = durationSeconds * actualSampleRate * actualChannels * 2
            var totalReadBytes = 0

            while (!isEOS && totalReadBytes < maxBytesToRead) {
                val inIndex = codec.dequeueInputBuffer(10000L)
                if (inIndex >= 0) {
                    val inBuffer = codec.getInputBuffer(inIndex)
                    if (inBuffer != null) {
                        val sampleSize = extractor.readSampleData(inBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10000L)
                if (outIndex >= 0) {
                    val outBuffer = codec.getOutputBuffer(outIndex)
                    if (outBuffer != null && bufferInfo.size > 0) {
                        outBuffer.position(bufferInfo.offset)
                        outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        val chunk = ByteArray(bufferInfo.size)
                        outBuffer.get(chunk)
                        rawPcmStream.write(chunk)
                        totalReadBytes += chunk.size
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = codec.outputFormat
                    if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        actualSampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        actualChannels = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            val rawBytes = rawPcmStream.toByteArray()
            if (rawBytes.isEmpty()) return null

            val shortCount = rawBytes.size / 2
            val shorts = ShortArray(shortCount)
            ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

            val monoShorts = if (actualChannels > 1) {
                val monoCount = shortCount / actualChannels
                val mono = ShortArray(monoCount)
                for (i in 0 until monoCount) {
                    var sum = 0
                    for (ch in 0 until actualChannels) {
                        sum += shorts[i * actualChannels + ch]
                    }
                    mono[i] = (sum / actualChannels).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                mono
            } else {
                shorts
            }

            if (actualSampleRate == SAMPLE_RATE) {
                monoShorts
            } else {
                resampleTo16k(monoShorts, actualSampleRate)
            }
        }.getOrNull()
    }

    private fun resampleTo16k(input: ShortArray, fromSampleRate: Int): ShortArray {
        if (input.isEmpty() || fromSampleRate <= 0) return input
        val outputLength = ((input.size.toLong() * SAMPLE_RATE) / fromSampleRate).toInt()
        val output = ShortArray(outputLength)
        val step = fromSampleRate.toDouble() / SAMPLE_RATE.toDouble()

        for (i in 0 until outputLength) {
            val srcPos = i * step
            val srcIndex = srcPos.toInt()
            val frac = srcPos - srcIndex
            if (srcIndex + 1 < input.size) {
                val s0 = input[srcIndex].toDouble()
                val s1 = input[srcIndex + 1].toDouble()
                output[i] = (s0 + frac * (s1 - s0)).coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toInt().toShort()
            } else if (srcIndex < input.size) {
                output[i] = input[srcIndex]
            }
        }
        return output
    }

    private data class Peak(
        val fftNumber: Int,
        val magnitude: Int,
        val frequencyBin: Int
    )

    private fun generateShazamSignatureUri(samples: ShortArray): String? {
        if (samples.size < FFT_SIZE) return null

        val sampleRing = ShortArray(FFT_SIZE)
        var sampleRingPos = 0

        val fftOutputs = Array(256) { FloatArray(NUM_BINS) }
        val spreadFfts = Array(256) { FloatArray(NUM_BINS) }
        var fftPosition = 0
        var spreadWritten = 0

        val peaksBand0 = ArrayList<Peak>()
        val peaksBand1 = ArrayList<Peak>()
        val peaksBand2 = ArrayList<Peak>()
        val peaksBand3 = ArrayList<Peak>()

        val realPart = DoubleArray(FFT_SIZE)
        val imagPart = DoubleArray(FFT_SIZE)

        val totalHops = (samples.size - FFT_SIZE) / HOP_SIZE
        val hopsToProcess = minOf(totalHops, (SAMPLE_RATE * 3.1 / HOP_SIZE).toInt())

        for (hop in 0 until hopsToProcess) {
            val sampleOffset = hop * HOP_SIZE
            for (i in 0 until HOP_SIZE) {
                sampleRing[sampleRingPos] = samples[sampleOffset + i]
                sampleRingPos = (sampleRingPos + 1) % FFT_SIZE
            }

            for (i in 0 until FFT_SIZE) {
                val ringIdx = (sampleRingPos + i) % FFT_SIZE
                realPart[i] = sampleRing[ringIdx] * hanningWindow[i].toDouble()
                imagPart[i] = 0.0
            }

            computeFft(realPart, imagPart)

            val currentFft = fftOutputs[fftPosition]
            for (bin in 0 until NUM_BINS) {
                val r = realPart[bin]
                val im = imagPart[bin]
                val power = ((r * r + im * im) / (1 shl 17)).toFloat()
                currentFft[bin] = max(power, 1e-10f)
            }

            val currentSpread = spreadFfts[fftPosition]
            for (bin in 0 until NUM_BINS) {
                var maxFreq = currentFft[bin]
                if (bin < 1023) {
                    if (currentFft[bin + 1] > maxFreq) maxFreq = currentFft[bin + 1]
                    if (bin + 2 < NUM_BINS && currentFft[bin + 2] > maxFreq) maxFreq = currentFft[bin + 2]
                }
                currentSpread[bin] = maxFreq

                var maxVal = maxFreq
                for (offset in intArrayOf(-1, -3, -6)) {
                    val prevIdx = (fftPosition + offset + 256) % 256
                    if (spreadFfts[prevIdx][bin] > maxVal) {
                        maxVal = spreadFfts[prevIdx][bin]
                    }
                    spreadFfts[prevIdx][bin] = maxVal
                }
            }

            spreadWritten++
            fftPosition = (fftPosition + 1) % 256

            if (spreadWritten >= 46) {
                val fft46Idx = (fftPosition - 46 + 256) % 256
                val spread49Idx = (fftPosition - 49 + 256) % 256
                val fft46 = fftOutputs[fft46Idx]
                val spread49 = spreadFfts[spread49Idx]

                for (bin in 10 until 1015) {
                    val val46 = fft46[bin]
                    if (val46 >= (1f / 64f) && val46 >= spread49[bin - 1]) {
                        var maxNeighbor = 0f
                        val neighborOffsets = intArrayOf(-10, -7, -4, -3, 1, 2, 5, 8)
                        for (off in neighborOffsets) {
                            val nb = spread49[bin + off]
                            if (nb > maxNeighbor) maxNeighbor = nb
                        }

                        if (val46 > maxNeighbor) {
                            var maxOther = maxNeighbor
                            val otherTimeOffsets = intArrayOf(-53, -45, 165, 172, 179, 186, 193, 200, 214, 221, 228, 235, 242, 249)
                            for (tOff in otherTimeOffsets) {
                                val tIdx = (fftPosition + tOff + 256) % 256
                                val v = spreadFfts[tIdx][bin - 1]
                                if (v > maxOther) maxOther = v
                            }

                            if (val46 > maxOther) {
                                val fftNumber = spreadWritten - 46
                                val pMag = (ln(max(1f / 64f, val46).toDouble()) * 1477.3 + 6144.0).toInt()
                                val pMagBefore = ln(max(1f / 64f, fft46[bin - 1]).toDouble()) * 1477.3 + 6144.0
                                val pMagAfter = ln(max(1f / 64f, fft46[bin + 1]).toDouble()) * 1477.3 + 6144.0

                                val variation1 = pMag * 2.0 - pMagBefore - pMagAfter
                                if (variation1 > 0) {
                                    val variation2 = (pMagAfter - pMagBefore) * 32.0 / variation1
                                    val correctedBin = (bin * 64 + variation2).toInt()
                                    val freqHz = correctedBin * (SAMPLE_RATE / 2.0 / 1024.0 / 64.0)

                                    val peak = Peak(fftNumber, pMag, correctedBin)
                                    when {
                                        freqHz in 250.0..520.0 -> peaksBand0.add(peak)
                                        freqHz in 520.0..1450.0 -> peaksBand1.add(peak)
                                        freqHz in 1450.0..3500.0 -> peaksBand2.add(peak)
                                        freqHz in 3500.0..5500.0 -> peaksBand3.add(peak)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val totalPeaks = peaksBand0.size + peaksBand1.size + peaksBand2.size + peaksBand3.size
        if (totalPeaks == 0) return null

        val bands = listOf(
            0 to peaksBand0,
            1 to peaksBand1,
            2 to peaksBand2,
            3 to peaksBand3
        )

        val contentsStream = ByteArrayOutputStream()
        for ((bandId, peaks) in bands) {
            if (peaks.isEmpty()) continue
            val peaksStream = ByteArrayOutputStream()
            var lastFftPass = 0
            for (p in peaks) {
                val diff = p.fftNumber - lastFftPass
                if (diff >= 255) {
                    peaksStream.write(0xFF)
                    peaksStream.write(p.fftNumber and 0xFF)
                    peaksStream.write((p.fftNumber shr 8) and 0xFF)
                    peaksStream.write((p.fftNumber shr 16) and 0xFF)
                    peaksStream.write((p.fftNumber shr 24) and 0xFF)
                    lastFftPass = p.fftNumber
                }
                peaksStream.write((p.fftNumber - lastFftPass) and 0xFF)
                peaksStream.write(p.magnitude and 0xFF)
                peaksStream.write((p.magnitude shr 8) and 0xFF)
                peaksStream.write(p.frequencyBin and 0xFF)
                peaksStream.write((p.frequencyBin shr 8) and 0xFF)
                lastFftPass = p.fftNumber
            }

            val peaksBytes = peaksStream.toByteArray()
            val bandHeaderTag = 0x60030040 + bandId
            writeLeInt(contentsStream, bandHeaderTag)
            writeLeInt(contentsStream, peaksBytes.size)
            contentsStream.write(peaksBytes)
            val pad = (-peaksBytes.size) and 3
            for (i in 0 until pad) contentsStream.write(0)
        }

        val contentsBytes = contentsStream.toByteArray()
        val sizeMinusHeader = contentsBytes.size + 8

        val bodyStream = ByteArrayOutputStream()
        writeLeInt(bodyStream, sizeMinusHeader)
        writeLeInt(bodyStream, 0x94119C00.toInt())
        writeLeInt(bodyStream, 0)
        writeLeInt(bodyStream, 0)
        writeLeInt(bodyStream, 0)
        writeLeInt(bodyStream, 3 shl 27)
        writeLeInt(bodyStream, 0)
        writeLeInt(bodyStream, 0)
        val numSamplesWithOffset = samples.size + (SAMPLE_RATE * 0.24).toInt()
        writeLeInt(bodyStream, numSamplesWithOffset)
        writeLeInt(bodyStream, (15 shl 19) + 0x40000)
        writeLeInt(bodyStream, 0x40000000)
        writeLeInt(bodyStream, sizeMinusHeader)
        bodyStream.write(contentsBytes)

        val bodyBytes = bodyStream.toByteArray()
        val crc = CRC32()
        crc.update(bodyBytes)
        val checksum = crc.value.toInt()

        val fullStream = ByteArrayOutputStream()
        writeLeInt(fullStream, 0xCAFE2580.toInt())
        writeLeInt(fullStream, checksum)
        fullStream.write(bodyBytes)

        val fullBytes = fullStream.toByteArray()
        val b64 = Base64.encodeToString(fullBytes, Base64.NO_WRAP)
        return "data:audio/vnd.shazam.sig;base64,$b64"
    }

    private fun writeLeInt(stream: ByteArrayOutputStream, value: Int) {
        stream.write(value and 0xFF)
        stream.write((value shr 8) and 0xFF)
        stream.write((value shr 16) and 0xFF)
        stream.write((value shr 24) and 0xFF)
    }

    private fun computeFft(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        var j = 0
        for (i in 0 until n) {
            if (j > i) {
                val tempR = real[i]
                val tempI = imag[i]
                real[i] = real[j]
                imag[i] = imag[j]
                j = j
                real[j] = tempR
                imag[j] = tempI
            }
            var m = n shr 1
            while (m >= 1 && j >= m) {
                j -= m
                m = m shr 1
            }
            j += m
        }

        var l = 1
        while (l < n) {
            val step = l shl 1
            val theta = -Math.PI / l
            val wpr = cos(theta)
            val wpi = sin(theta)
            var wr = 1.0
            var wi = 0.0

            for (m in 0 until l) {
                var i = m
                while (i < n) {
                    val k = i + l
                    val tr = wr * real[k] - wi * imag[k]
                    val ti = wr * imag[k] + wi * real[k]
                    real[k] = real[i] - tr
                    imag[k] = imag[i] - ti
                    real[i] += tr
                    imag[i] += ti
                    i += step
                }
                val nextWr = wr * wpr - wi * wpi
                wi = wr * wpi + wi * wpr
                wr = nextWr
            }
            l = step
        }
    }

    private fun sendShazamRecognizeRequest(signatureUri: String, sampleMs: Long): RecognitionResult? {
        return runCatching {
            val uuidA = UUID.randomUUID().toString().uppercase()
            val uuidB = UUID.randomUUID().toString().uppercase()
            val urlStr = "https://amp.shazam.com/discovery/v5/ru/RU/iphone/-/tag/$uuidA/$uuidB?sync=true&webv3=true&sampling=true&connected=&shazamapiversion=v3&sharehub=true&hubv5minorversion=v5.1&hidelb=true&video=v3"

            val jsonBody = JSONObject().apply {
                put("timezone", "Europe/Moscow")
                put("signature", JSONObject().apply {
                    put("uri", signatureUri)
                    put("samplems", sampleMs.toInt())
                })
                put("timestamp", System.currentTimeMillis())
                put("context", JSONObject())
                put("geolocation", JSONObject())
            }

            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-Shazam-Platform", "IPHONE")
                setRequestProperty("X-Shazam-AppVersion", "14.1.0")
                setRequestProperty("User-Agent", "Shazam/3685 CFNetwork/1197 Darwin/20.0.0")
                setRequestProperty("Accept", "*/*")
                setRequestProperty("Accept-Language", "ru")
            }

            conn.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                parseShazamResponse(resp)
            } else {
                conn.disconnect()
                null
            }
        }.getOrNull()
    }

    private fun parseShazamResponse(jsonString: String): RecognitionResult? {
        return runCatching {
            val root = JSONObject(jsonString)
            val track = root.optJSONObject("track") ?: return null

            val title = track.optString("title", "").trim()
            val artist = track.optString("subtitle", "").trim()
            if (title.isBlank()) return null

            var album = ""
            var year = ""
            val sections = track.optJSONArray("sections")
            if (sections != null) {
                for (s in 0 until sections.length()) {
                    val sec = sections.getJSONObject(s)
                    val metadata = sec.optJSONArray("metadata")
                    if (metadata != null) {
                        for (m in 0 until metadata.length()) {
                            val item = metadata.getJSONObject(m)
                            val titleKey = item.optString("title", "")
                            val textVal = item.optString("text", "")
                            if (titleKey.equals("Album", ignoreCase = true) || titleKey.equals("Альбом", ignoreCase = true)) {
                                album = textVal
                            } else if (titleKey.equals("Released", ignoreCase = true) || titleKey.equals("Год", ignoreCase = true)) {
                                year = textVal
                            }
                        }
                    }
                }
            }

            var genre = ""
            val genres = track.optJSONObject("genres")
            if (genres != null) {
                genre = genres.optString("primary", "")
            }

            var coverUrl: String? = null
            val images = track.optJSONObject("images")
            if (images != null) {
                coverUrl = images.optString("coverarthq", "").takeIf { it.isNotBlank() }
                    ?: images.optString("coverart", "").takeIf { it.isNotBlank() }
            }

            RecognitionResult(
                title = title,
                artist = artist.ifBlank { "Unknown Artist" },
                album = album,
                year = year,
                genre = genre,
                coverUrl = coverUrl
            )
        }.getOrNull()
    }
}
