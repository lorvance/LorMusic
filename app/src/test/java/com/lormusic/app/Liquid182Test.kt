package com.lormusic.app

import com.lormusic.app.data.*
import com.lormusic.app.model.*
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class Liquid182Test {
    @Test
    fun emptyTimelineAndDiscreteEqValuesStayFinite() {
        assertEquals(0f, GlassControlValues.value(Float.NaN, 0f..0f, 0), .001f)
        assertTrue(GlassControlValues.range(0f..0f).endInclusive > 0f)
        assertEquals(100f, GlassControlValues.value(149f, -1200f..1200f, 23), .001f)
        assertEquals(1200f, GlassControlValues.value(1500f, -1200f..1200f, 23), .001f)
    }

    @Test
    fun powerDefaultsAndOldTransparencyMigration() {
        assertEquals(15, GlassStrength.resolve(null, null))
        assertEquals(85, GlassStrength.DEFAULT_TRANSPARENCY)
        assertEquals(90, GlassStrength.resolve(90, null))
        assertEquals(35, GlassStrength.resolve(null, 65))
        assertEquals(70, GlassStrength.resolve(70, 65))
        assertEquals(10, GlassStrength.transparency(90))
        assertEquals(100, GlassStrength.resolve(120, null))
        assertEquals(0, GlassStrength.resolve(-5, null))
    }

    @Test
    fun releaseSettingsPromptsAreAvailableInEachStandardLanguage() {
        val keys = listOf(
            "glassCategoryDesc", "betaJokesAccess", "betaJokesPasswordTitle",
            "betaJokesPasswordLabel", "betaJokesPasswordError", "betaJokesUnlock",
            "hideTrackTitle", "restoreTrackTitle", "restoreTrackAction",
        )
        for (strings in listOf(StringsRu, StringsEn, StringsUk, StringsPl, StringsDe)) {
            keys.forEach { assertTrue(strings.betaText(it).isNotBlank()) }
            assertTrue(strings.betaText("glassDefault").format(GlassStrength.DEFAULT).endsWith("15%"))
        }
        assertFalse(BetaTesterAccess.matches(""))
        assertFalse(BetaTesterAccess.matches("invalid"))
    }

    @Test
    fun expiredOptionalSessionRetriesPublicRequestWithoutCredentials() {
        val calls = AtomicInteger()
        val server =
            ServerSocket(0, 2, java.net.InetAddress.getByName("127.0.0.1")).apply {
                soTimeout = 10000
            }
        val executor = Executors.newSingleThreadExecutor()
        val task =
            executor.submit {
                repeat(2) {
                    server.accept().use { socket ->
                        socket.soTimeout = 5000
                        val reader = socket.getInputStream().bufferedReader()
                        var auth = false
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                            if (line.startsWith("Authorization:", true)) auth = true
                        }
                        calls.incrementAndGet()
                        val body = if (auth) "expired" else "{\"title\":\"Public music\"}"
                        val status = if (auth) "401 Unauthorized" else "200 OK"
                        socket.getOutputStream().use {
                            it.write(
                                ("HTTP/1.1 $status\r\nContent-Type: application/json\r\nContent-Length: ${body.toByteArray().size}\r\nConnection: close\r\n\r\n" +
                                        body)
                                    .toByteArray()
                            )
                            it.flush()
                        }
                    }
                }
            }
        try {
            val json =
                SoundCloudTransport.fetch(
                    "http://127.0.0.1:${server.localPort}/track",
                    "expired-test-session",
                )
            assertEquals("Public music", json.getString("title"))
            task.get(10, TimeUnit.SECONDS)
            assertEquals(2, calls.get())
        } finally {
            server.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun downloadingSkipsSnippetsAndUnsupportedCodecsButRetainsValidAac() {
        val track =
            JSONObject(
                """{"media":{"transcodings":[{"url":"https://cdn.example/opus","format":{"mime_type":"audio/ogg","protocol":"hls"}},{"url":"https://cdn.example/snippet","snipped":true,"format":{"mime_type":"audio/mpeg","protocol":"progressive"}},{"url":"https://cdn.example/full-aac","snipped":false,"format":{"mime_type":"audio/mp4; codecs=mp4a.40.2","protocol":"hls"}}]}}"""
            )
        assertEquals(
            listOf("https://cdn.example/full-aac"),
            SoundCloudTransport.transcodings(track, true),
        )
        assertEquals(3, SoundCloudTransport.transcodings(track, false).size)
    }

    @Test
    fun pulseAndMinimalAreSeparatePersistableLauncherOptions() {
        assertNotEquals(AppIconOption.PULSE.aliasClassName, AppIconOption.MINIMAL.aliasClassName)
        assertEquals("LorMusic Pulse", AppIconOption.PULSE.getDisplayName(StringsRu))
        for (s in listOf(StringsRu, StringsEn, StringsUk, StringsPl, StringsDe)) {
            assertTrue(AppIconOption.PULSE.getDisplaySubtitle(s).isNotBlank())
            assertTrue(s.betaText("strength").isNotBlank())
            assertTrue(s.betaText("glassAllLabels").isNotBlank())
        }
    }
}
