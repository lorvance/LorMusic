package com.lormusic.app.ui.components

import com.lormusic.app.model.betaText
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lormusic.app.data.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun BackupDialogs(onExport: () -> String, onImport: (String) -> Boolean, onDismiss: () -> Unit) {
    val strings = com.lormusic.app.model.LocalStrings.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val repository = remember { PreferencesRepository(context) }
    var textMode by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    var pendingExport by rememberSaveable { mutableStateOf("") }
    var validated by rememberSaveable { mutableStateOf<String?>(null) }
    var summary by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun validate(value: String) {
        scope.launch {
            busy = true
            runCatching {
                require(value.length <= 4 * 1024 * 1024) { strings.betaText("tooLarge") }
                withContext(Dispatchers.IO) { repository.validateBackup(value) }
            }.onSuccess { summary = it.replace("Формат:", strings.betaText("format") + ":"); validated = value; message = null }
                .onFailure { message = strings.betaText("invalidJson") }
            busy = false
        }
    }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val payload = pendingExport
            runCatching {
                withContext(Dispatchers.IO) {
                    require(payload.isNotBlank()) { strings.betaText("retryExport") }
                    val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error(strings.betaText("openFailed"))
                    stream.bufferedWriter(Charsets.UTF_8).use { it.write(payload) }
                }
            }.onSuccess { message = strings.betaText("saved") }
                .onFailure { message = strings.betaText("saveError") }
            pendingExport = ""
            busy = false
        } else pendingExport = ""
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val loaded = runCatching {
                withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri) ?: error(strings.betaText("fileUnavailable"))
                    stream.use {
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = it.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= 4 * 1024 * 1024) { strings.betaText("tooLarge") }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray().toString(Charsets.UTF_8)
                    }
                }
            }
            busy = false
            loaded.onSuccess { validate(it) }.onFailure { message = strings.betaText("readError") }
        }
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        icon = { Icon(Icons.Filled.Backup, null) },
        title = { Text(if (textMode) strings.betaText("restoreText") else strings.betaText("backup")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (textMode) {
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("JSON") },
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp), enabled = !busy)
                    TextButton(onClick = { clipboard.getText()?.text?.let { text = it } }, enabled = !busy) { Text(strings.betaText("paste")) }
                    Button(onClick = { validate(text) }, enabled = !busy && text.isNotBlank()) { Text(strings.betaText("validate")) }
                } else {
                    TextButton(onClick = {
                        scope.launch {
                            busy = true
                            runCatching { withContext(Dispatchers.IO) { onExport() } }
                                .onSuccess {
                                    pendingExport = it
                                    create.launch("LorMusic_Backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")) + ".json")
                                }.onFailure { message = strings.betaText("exportError") }
                            busy = false
                        }
                    }, enabled = !busy) { Text(strings.betaText("createBackup")) }
                    TextButton(onClick = { open.launch(arrayOf("application/json", "text/json", "text/plain", "application/octet-stream")) }, enabled = !busy) { Text(strings.betaText("restoreFile")) }
                    TextButton(onClick = { textMode = true; message = null }, enabled = !busy) { Text(strings.betaText("restoreText")) }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                message?.let { Text(it) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(strings.betaText("close")) } },
        dismissButton = { if (textMode) TextButton(onClick = { textMode = false }, enabled = !busy) { Text(strings.betaText("back")) } }
    )
    validated?.let { payload ->
        AlertDialog(onDismissRequest = { validated = null }, shape = RoundedCornerShape(28.dp),
            title = { Text(strings.betaText("restoreQuestion")) },
            text = { Text(summary + "\n\n" + strings.betaText("restoreWarning")) },
            confirmButton = {
                TextButton(onClick = {
                    // The ViewModel refreshes Compose state, so apply on the main thread.
                    val ok = onImport(payload)
                    validated = null
                    message = if (ok) strings.betaText("restored") else strings.betaText("restoreFailed")
                }) { Text(strings.betaText("restore")) }
            }, dismissButton = { TextButton(onClick = { validated = null }) { Text(strings.betaText("cancel")) } })
    }
}
