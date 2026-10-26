package com.lormusic.app.data

import android.content.Context
import android.net.Uri
import com.lormusic.app.model.Song
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object DemoSynthGenerator {

    private val chordProgressions = listOf(

        arrayOf(
            doubleArrayOf(220.00, 261.63, 329.63),
            doubleArrayOf(174.61, 220.00, 261.63),
            doubleArrayOf(261.63, 329.63, 392.00),
            doubleArrayOf(196.00, 246.94, 293.66)
        ),

        arrayOf(
            doubleArrayOf(293.66, 349.23, 440.00),
            doubleArrayOf(233.08, 293.66, 349.23),
            doubleArrayOf(196.00, 233.08, 293.66),
            doubleArrayOf(220.00, 277.18, 329.63)
        ),

        arrayOf(
            doubleArrayOf(164.81, 196.00, 246.94),
            doubleArrayOf(261.63, 329.63, 392.00),
            doubleArrayOf(196.00, 246.94, 293.66),
            doubleArrayOf(293.66, 369.99, 440.00)
        ),

        arrayOf(
            doubleArrayOf(174.61, 207.65, 261.63),
            doubleArrayOf(277.18, 349.23, 415.30),
            doubleArrayOf(207.65, 261.63, 311.13),
            doubleArrayOf(311.13, 392.00, 466.16)
        ),

        arrayOf(
            doubleArrayOf(261.63, 329.63, 392.00),
            doubleArrayOf(220.00, 261.63, 329.63),
            doubleArrayOf(293.66, 349.23, 440.00),
            doubleArrayOf(196.00, 246.94, 349.23)
        )
    )

    fun ensureDemoTracksReady(context: Context, demoSongs: List<Song>): List<Song> {
        val dir = File(context.cacheDir, "demo_tracks")
        if (!dir.exists()) dir.mkdirs()

        return demoSongs.mapIndexed { index, song ->
            val outFile = File(dir, "${song.id}.wav")
            if (!outFile.exists() || outFile.length() < 1024L) {
                val durationSec = (song.durationMs / 1000L).toInt().coerceIn(10, 40)
                generateSynthTrackWav(outFile, durationSec, index)
            }
            song.copy(uri = Uri.fromFile(outFile).toString())
        }
    }

    private fun generateSynthTrackWav(file: File, durationSec: Int, styleIndex: Int) {
        val sampleRate = 22050
        val numChannels = 1
        val bitsPerSample = 16
        val totalSamples = sampleRate * durationSec
        val dataSize = totalSamples * numChannels * (bitsPerSample / 8)

        val progression = chordProgressions[styleIndex % chordProgressions.size]
        val tempoBpm = 92.0 + (styleIndex * 8 % 36)
        val beatDuration = 60.0 / tempoBpm
        val arpSpeed = beatDuration / 2.0

        BufferedOutputStream(FileOutputStream(file), 32768).use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(36 + dataSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16)
            header.putShort(1)
            header.putShort(numChannels.toShort())
            header.putInt(sampleRate)
            header.putInt(sampleRate * numChannels * (bitsPerSample / 8))
            header.putShort((numChannels * (bitsPerSample / 8)).toShort())
            header.putShort(bitsPerSample.toShort())
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize)
            out.write(header.array())

            val chunkSamples = 2048
            val buf = ByteBuffer.allocate(chunkSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
            var i = 0
            while (i < totalSamples) {
                buf.clear()
                val end = minOf(i + chunkSamples, totalSamples)
                while (i < end) {
                    val t = i.toDouble() / sampleRate
                    val barIndex = (t / (beatDuration * 4.0)).toInt()
                    val chord = progression[barIndex % progression.size]

                    val pad = (
                        sin(2.0 * PI * chord[0] * t) +
                        sin(2.0 * PI * chord[1] * t) * 0.8 +
                        sin(2.0 * PI * chord[2] * t) * 0.7
                    ) / 2.5

                    val arpStep = (t / arpSpeed).toInt()
                    val arpPhase = (t % arpSpeed) / arpSpeed
                    val arpFreq = chord[arpStep % chord.size] * (if (arpStep % 4 == 3) 2.0 else 1.0)
                    val arpEnv = exp(-arpPhase * 4.5)
                    val arp = sin(2.0 * PI * arpFreq * t) * arpEnv * 0.45

                    val beatPhase = (t % beatDuration) / beatDuration
                    val bassEnv = exp(-beatPhase * 2.5)
                    val bass = sin(2.0 * PI * (chord[0] * 0.5) * t) * bassEnv * 0.45

                    val fade = when {
                        t < 1.0 -> t
                        t > durationSec - 1.5 -> ((durationSec - t) / 1.5).coerceAtLeast(0.0)
                        else -> 1.0
                    }

                    val mixed = ((pad * 0.45 + arp + bass) * fade).coerceIn(-0.95, 0.95)
                    val pcm = (mixed * 32767.0).toInt().toShort()
                    buf.putShort(pcm)
                    i++
                }
                out.write(buf.array(), 0, buf.position())
            }
        }
    }
}
