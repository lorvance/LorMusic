package com.lormusic.app

import com.lormusic.app.model.*
import com.lormusic.app.ui.components.*
import org.junit.Assert.*
import org.junit.Test

class Release181Test {
    @Test
    fun serviceHomeIsCenteredByDefault() {
        assertEquals(
            listOf(YtMusicTab.LIBRARIES, YtMusicTab.HOME, YtMusicTab.MY_WAVE),
            parseYtTabOrder(null),
        )
        assertEquals(listOf(ScTab.CHARTS, ScTab.HOME, ScTab.LIBRARIES), parseScTabOrder(null))
    }

    @Test
    fun savedServiceOrdersSurviveAndSearchCannotEnterMainTabs() {
        val yt = listOf(YtMusicTab.MY_WAVE, YtMusicTab.HOME, YtMusicTab.LIBRARIES)
        assertEquals(yt, parseYtTabOrder(yt.joinToString(",") { it.name }))
        val sc = listOf(ScTab.LIBRARIES, ScTab.HOME, ScTab.CHARTS)
        assertEquals(sc, parseScTabOrder(sc.joinToString(",") { it.name }))
        assertEquals(DEFAULT_YT_TAB_ORDER, parseYtTabOrder("SEARCH,wrong,SEARCH"))
        assertEquals(DEFAULT_SC_TAB_ORDER, parseScTabOrder("wrong,SEARCH"))
        assertEquals(
            listOf(YtMusicTab.MY_WAVE, YtMusicTab.LIBRARIES, YtMusicTab.HOME),
            parseYtTabOrder("MY_WAVE,MY_WAVE,SEARCH"),
        )
        assertEquals(
            3,
            normalizeScTabOrder(listOf(ScTab.SEARCH, ScTab.HOME, ScTab.HOME)).distinct().size,
        )
    }

    @Test
    fun randomLanguageKeepsAllFormattingContracts() {
        val pattern = Regex("""%(?:\d+\$)?[-#+ 0,(<]*\d*(?:\.\d+)?[tT]?[A-Za-z%]""")
        fun verify(a: Any, b: Any) {
            a.javaClass.declaredFields
                .filter { it.type == String::class.java }
                .forEach { field ->
                    field.isAccessible = true
                    val original = field.get(a) as String
                    val changed = field.get(b) as String
                    assertEquals(
                        field.name,
                        pattern.findAll(original).map { it.value }.toList(),
                        pattern.findAll(changed).map { it.value }.toList(),
                    )
                }
        }
        verify(StringsRu, StringsRandom)
        verify(StringsRu.extra, StringsRandom.extra)
        assertEquals("Сухарики", StringsRandom.navSettings)
        assertEquals("Химия", StringsRandom.navLibrary)
        assertEquals("Смех", StringsRandom.searchTitle)
        assertTrue(
            StringsRandom.deletePlaylistConfirmMessage.contains(
                StringsRu.deletePlaylistConfirmMessage
            )
        )
        assertEquals(
            RandomRussian.text("navTabOrder", "Порядок кнопок"),
            StringsRandom.betaText("navTabOrder"),
        )
    }

    @Test
    fun randomLanguageUsesRussianForRemoteContent() {
        assertEquals(AppLanguage.RANDOM, AppLanguage.fromCode("random"))
        assertEquals("ru", AppLanguage.RANDOM.contentLanguageCode)
        assertSame(StringsRandom, resolveStrings(AppLanguage.RANDOM))
    }
}
