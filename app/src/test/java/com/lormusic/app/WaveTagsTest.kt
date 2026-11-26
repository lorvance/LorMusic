package com.lormusic.app

import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class WaveTagsTest {
    @Test fun textAndArtworkSurviveId3Framing() {
        val picture = ByteArray(450) { it.toByte() }
        val tag = waveId3(mapOf("INAM" to "Трек", "LYRI" to "Слова", "IGNR" to ""),picture,"image/png")
        assertEquals("ID3", String(tag,0,3))
        val size = tag.slice(6..9).fold(0) { n,b -> (n shl 7) or b.toInt() }
        assertEquals(tag.size-10,size)
        val frames = mutableMapOf<String,ByteArray>()
        val buffer = ByteBuffer.wrap(tag).apply { position(10) }
        while (buffer.hasRemaining()) {
            val id = ByteArray(4).also { buffer.get(it) }
            val length = buffer.int
            assertEquals(0,buffer.short.toInt())
            frames[String(id)] = ByteArray(length).also { buffer.get(it) }
        }
        assertEquals(setOf("TIT2","USLT","APIC"),frames.keys)
        assertEquals("Трек",frames.getValue("TIT2").drop(1).toByteArray().toString(Charsets.UTF_16))
        assertArrayEquals(picture,frames.getValue("APIC").takeLast(picture.size).toByteArray())
    }
}
