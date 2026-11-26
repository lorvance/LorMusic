package com.lormusic.app

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

internal fun waveId3(tags: Map<String,String>, artwork: ByteArray?, mime: String?): ByteArray {
    val frames = ByteArrayOutputStream()
    val writer = DataOutputStream(frames)
    fun frame(id: String, bytes: ByteArray) {
        writer.writeBytes(id)
        writer.writeInt(bytes.size)
        writer.writeShort(0)
        writer.write(bytes)
    }
    val ids = mapOf("INAM" to "TIT2","IART" to "TPE1","IPRD" to "TALB","IGNR" to "TCON","ICRD" to "TYER","ITRK" to "TRCK","ICOP" to "TCOP","ICMT" to "COMM","LYRI" to "USLT","AART" to "TPE2","COMP" to "TCOM")
    for ((key,value) in tags) {
        if (value.isBlank()) continue
        val id = ids[key] ?: continue
        val prefix = if (id == "USLT" || id == "COMM") byteArrayOf(1) + "und".toByteArray() + byteArrayOf(0,0) else byteArrayOf(1)
        frame(id, prefix + value.toByteArray(Charsets.UTF_16))
    }
    if (artwork != null && mime != null) frame("APIC",byteArrayOf(0) + mime.toByteArray(Charsets.US_ASCII) + byteArrayOf(0,3,0) + artwork)
    val size = frames.size()
    val length = ByteArray(4) { ((size ushr ((3-it)*7)) and 127).toByte() }
    return "ID3".toByteArray() + byteArrayOf(3,0,0) + length + frames.toByteArray()
}
