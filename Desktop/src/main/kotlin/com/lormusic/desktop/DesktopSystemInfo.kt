package com.lormusic.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val repository = "https://github.com/lorvance/lormusic"

@Composable
internal fun DesktopSystemInfo() {
    var github by remember { mutableStateOf(false) }
    var license by remember { mutableStateOf(false) }
    SettingsGroup("О приложении") {
        Text("LorMusic · $DESKTOP_VERSION", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton({ github = true }) { Text("GitHub") }
            FilledTonalButton({ license = true }) { Text(t("Лицензия")) }
        }
    }
    if (github) AlertDialog(
        onDismissRequest = { github = false },
        title = { Text(t("Перейти на GitHub?")) },
        text = { Text(repository) },
        dismissButton = { TextButton({ github = false }) { Text(t("Отмена")) } },
        confirmButton = { TextButton({ github = false; browse(repository) }) { Text(t("Перейти")) } }
    )
    if (license) {
        val paragraphs = remember {
            object {}.javaClass.getResourceAsStream("/gpl-3.0.txt")!!.bufferedReader()
                .use { it.readText().removePrefix("\uFEFF") }.split(Regex("\\r?\\n[ \\t]*\\r?\\n"))
        }
        AlertDialog(
            onDismissRequest = { license = false },
            title = { Text("GNU GPL v3.0") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(paragraphs) { Text(it) }
                }
            },
            confirmButton = { TextButton({ license = false }) { Text(t("Закрыть")) } }
        )
    }
}
