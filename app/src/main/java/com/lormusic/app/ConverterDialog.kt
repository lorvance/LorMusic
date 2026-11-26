package com.lormusic.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel

internal class ConversionJob(application: android.app.Application) : AndroidViewModel(application) {
    var busy by mutableStateOf(false)
        private set
    var status by mutableStateOf("")
    fun start(src: Uri, dest: Uri, tags: Map<String,String>, cover: Uri?, format: AudioExport, bitrate: Int) {
        if (busy) return
        busy = true
        status = ""
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { convertVideo(getApplication(), src, dest, tags, cover, format, bitrate) }
                status = "Аудиофайл сохранён"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status = e.localizedMessage ?: "Ошибка конвертации"
            } finally {
                busy = false
            }
        }
    }
}

@Composable
fun ConverterDialog(close: () -> Unit) {
    val context = LocalContext.current
    val job: ConversionJob = viewModel()
    var source by rememberSaveable { mutableStateOf<Uri?>(null) }
    val fields = listOf("INAM" to "Название", "IART" to "Исполнитель", "IPRD" to "Альбом", "IGNR" to "Жанр", "ICRD" to "Год", "ITRK" to "Номер трека", "ICMT" to "Комментарий", "ICOP" to "Авторские права", "AART" to "Исполнитель альбома", "COMP" to "Композитор", "LYRI" to "Текст песни")
    var values by rememberSaveable { mutableStateOf(Array(fields.size) { "" }) }
    val busy = job.busy
    val status = job.status
    var cover by rememberSaveable { mutableStateOf<Uri?>(null) }
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { if (it != null) cover = it }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            source = uri
            cover = null
            values = Array(fields.size) { "" }
            job.status = ""
            runCatching { context.contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use {
                if (it.moveToFirst()) values = Array(fields.size) { index -> if (index == 0) it.getString(0).substringBeforeLast('.') else "" }
            } }
        }
    }
    var format by rememberSaveable { mutableStateOf(AudioExport.WAV) }
    var bitrate by rememberSaveable { mutableIntStateOf(160) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(format.mime)) { dest ->
        val src = source
        if (src != null && dest != null) {
            val tags = fields.mapIndexed { i, field -> field.first to values[i] }.toMap()
            job.start(src, dest, tags, cover, format, bitrate * 1000)
        }
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = { if (!busy) close() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Видео в аудио", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                    TextButton(close, enabled = !busy) { Text("Закрыть") }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { pick.launch(arrayOf("video/*")) }, enabled = !busy) {
                        Text(if (source == null) "Выбрать видео" else "Заменить видео")
                    }
                    source?.let { uri ->
                        VideoConversionPreview(uri, busy)
                        Text("Формат аудио", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(format == AudioExport.WAV, { format = AudioExport.WAV }, { Text("WAV · PCM") }, enabled = !busy)
                            FilterChip(format == AudioExport.AAC, { format = AudioExport.AAC }, { Text("M4A · AAC") }, enabled = !busy)
                        }
                        if (format == AudioExport.AAC) {
                            Text("Битрейт")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(96, 160, 256).forEach { rate ->
                                    FilterChip(bitrate == rate, { bitrate = rate }, { Text("$rate кбит/с") }, enabled = !busy)
                                }
                            }
                        }
                        run {
                            Text("Теги", style = MaterialTheme.typography.titleMedium)
                            TextButton({ pickCover.launch(arrayOf("image/*")) }, enabled = !busy) {
                                Text(if (cover == null) "Добавить обложку" else "Заменить обложку")
                            }
                            fields.forEachIndexed { i, field ->
                                OutlinedTextField(values[i], { text -> values = values.copyOf().also { it[i] = text } },
                                    modifier = Modifier.fillMaxWidth(), label = { Text(field.second) }, enabled = !busy)
                            }
                        }
                    }
                    if (status.isNotEmpty()) Text(status)
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Button({ save.launch(values[0].ifBlank { "Audio" } + "." + format.extension) },
                    modifier = Modifier.fillMaxWidth(), enabled = source != null && !busy) {
                    Text("Сохранить .${format.extension}")
                }
            }
        }
    }
}

@Composable
private fun VideoConversionPreview(uri: Uri, busy: Boolean) {
    val context = LocalContext.current
    key(uri) {
        val video = remember { android.widget.VideoView(context) }
        DisposableEffect(video) {
            video.setMediaController(android.widget.MediaController(context).apply { setAnchorView(video) })
            video.setVideoURI(uri)
            onDispose { video.stopPlayback() }
        }
        val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle, video) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) video.pause()
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
        LaunchedEffect(busy) { if (busy) video.pause() }
        androidx.compose.ui.viewinterop.AndroidView(factory = { video },
            modifier = Modifier.fillMaxWidth().height(200.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton({ video.start() }, enabled = !busy) { Text("Воспроизвести") }
            TextButton({ video.pause() }, enabled = !busy) { Text("Пауза") }
        }
    }
}
