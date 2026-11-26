package com.lormusic.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.lormusic.app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

internal const val LOR_MUSIC_REPOSITORY = "https://github.com/lorvance/lormusic"

internal enum class SystemDocument { LICENSE, CHANGELOG }

@Composable
internal fun SystemDocumentDialog(document: SystemDocument, close: () -> Unit) {
    val context = LocalContext.current
    val labels = systemLabels(com.lormusic.app.model.LocalStrings.current)
    var message by remember(document, labels) { mutableStateOf(labels.loading) }
    LaunchedEffect(document, labels) {
        message = try {
            withContext(Dispatchers.IO) {
                when (document) {
                    SystemDocument.LICENSE -> context.assets.open("gpl-3.0.txt").bufferedReader().use { it.readText().removePrefix("\uFEFF") }
                    SystemDocument.CHANGELOG -> findReleaseNotes(BuildConfig.VERSION_NAME)
                        ?.ifBlank { labels.noNotes } ?: labels.missing.format(BuildConfig.VERSION_NAME)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (document == SystemDocument.CHANGELOG) labels.historyError else labels.licenseError
        }
    }
    val paragraphs = remember(message) { message.split(Regex("\\r?\\n[ \\t]*\\r?\\n")) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(if (document == SystemDocument.LICENSE) "GNU GPL v3.0" else "${labels.history} · ${BuildConfig.VERSION_NAME}") },
        text = {
            LazyColumn(Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(paragraphs) { paragraph ->
                    Text(paragraph, fontFamily = if (document == SystemDocument.LICENSE) FontFamily.Monospace else FontFamily.Default)
                }
            }
        },
        confirmButton = { TextButton(onClick = close) { Text(labels.close) } }
    )
}

private fun findReleaseNotes(version: String): String? {
    fun normalized(value: String) = value.trim().removePrefix("LorMusic ").removePrefix("v").trim()
    var page = 1
    while (true) {
        val connection = (URL("https://api.github.com/repos/lorvance/lormusic/releases?per_page=100&page=$page")
            .openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "LorMusic-Android/$version")
        }
        try {
            check(connection.responseCode == 200)
            val releases = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
            for (index in 0 until releases.length()) {
                val release = releases.getJSONObject(index)
                if (release.optBoolean("draft") || release.optBoolean("prerelease")) continue
                if (normalized(release.optString("tag_name")) == version || normalized(release.optString("name")) == version) {
                    return release.optString("body", "").takeUnless { it == "null" }.orEmpty()
                }
            }
            if (releases.length() < 100) return null
            page++
        } finally {
            connection.disconnect()
        }
    }
}

internal data class SystemLabels(
    val openGithub: String, val cancel: String, val open: String, val openError: String,
    val source: String, val license: String, val history: String, val whatsNew: String,
    val loading: String, val missing: String, val noNotes: String, val historyError: String,
    val licenseError: String, val close: String, val build: String,
)

internal fun systemLabels(strings: com.lormusic.app.model.AppStrings): SystemLabels = when (strings) {
    com.lormusic.app.model.StringsEn -> SystemLabels(
        "Open GitHub?", "Cancel", "Open", "Could not open GitHub", "App source code", "License",
        "Changelog", "What’s new in this version", "Loading…", "Version %s is not available on GitHub.",
        "This GitHub release has no notes.", "Could not load the changelog from GitHub.", "Could not open the license.", "Close", "Build"
    )
    com.lormusic.app.model.StringsUk -> SystemLabels(
        "Перейти на GitHub?", "Скасувати", "Перейти", "Не вдалося відкрити GitHub", "Вихідний код застосунку", "Ліцензія",
        "Історія змін", "Що нового в цій версії", "Завантаження…", "Версія %s відсутня на GitHub.",
        "Цей випуск на GitHub не має опису.", "Не вдалося завантажити історію змін із GitHub.", "Не вдалося відкрити ліцензію.", "Закрити", "Збірка"
    )
    com.lormusic.app.model.StringsDe -> SystemLabels(
        "GitHub öffnen?", "Abbrechen", "Öffnen", "GitHub konnte nicht geöffnet werden", "Quellcode der App", "Lizenz",
        "Änderungsverlauf", "Neues in dieser Version", "Wird geladen…", "Version %s ist auf GitHub nicht verfügbar.",
        "Für diese GitHub-Veröffentlichung gibt es keine Versionshinweise.", "Der Änderungsverlauf konnte nicht von GitHub geladen werden.", "Die Lizenz konnte nicht geöffnet werden.", "Schließen", "Build"
    )
    com.lormusic.app.model.StringsPl -> SystemLabels(
        "Otworzyć GitHub?", "Anuluj", "Otwórz", "Nie udało się otworzyć GitHuba", "Kod źródłowy aplikacji", "Licencja",
        "Historia zmian", "Co nowego w tej wersji", "Wczytywanie…", "Wersja %s nie jest dostępna w serwisie GitHub.",
        "To wydanie w serwisie GitHub nie ma opisu.", "Nie udało się pobrać historii zmian z GitHuba.", "Nie udało się otworzyć licencji.", "Zamknij", "Kompilacja"
    )
    else -> SystemLabels(
        "Перейти на GitHub?", "Отмена", "Перейти", "Не удалось открыть GitHub", "Исходный код приложения", "Лицензия",
        "История изменений", "Что нового в этой версии", "Загрузка…", "Версия %s отсутствует на GitHub.",
        "На GitHub опубликован релиз без описания.", "Не удалось загрузить историю изменений с GitHub.", "Не удалось открыть текст лицензии.", "Закрыть", "Сборка"
    )
}
