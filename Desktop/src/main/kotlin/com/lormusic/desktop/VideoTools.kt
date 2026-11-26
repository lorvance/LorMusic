package com.lormusic.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val videoExtensions = arrayOf("mp4", "mkv", "webm", "mov", "avi", "m4v", "ts")

private fun chooseVideo(): File? = JFileChooser().run {
    fileFilter = FileNameExtensionFilter("Video", *videoExtensions)
    if (showOpenDialog(null) == JFileChooser.APPROVE_OPTION) selectedFile else null
}

internal fun previewVideo(file: File): Process =
    ProcessBuilder(DesktopMedia.vlcExecutable(), "--intf=qt", "--no-one-instance", "--play-and-exit", file.absolutePath)
        .redirectError(ProcessBuilder.Redirect.DISCARD).redirectOutput(ProcessBuilder.Redirect.DISCARD).start()

@Composable
internal fun VideoTools(actions: Actions, close: () -> Unit) {
    val labels = LocalLanguage.current
    var source by remember { mutableStateOf<File?>(null) }
    var cover by remember { mutableStateOf<File?>(null) }
    var preview by remember { mutableStateOf<Process?>(null) }
    var format by remember { mutableStateOf("mp3") }
    val fields = listOf("Название", "Исполнитель", "Альбом", "Жанр", "Год", "Номер трека", "Текст песни")
    val values = remember { mutableStateListOf(*Array(fields.size) { "" }) }
    DisposableEffect(Unit) { onDispose { preview?.destroy() } }
    DialogWindow(onCloseRequest = close, state = rememberDialogState(width = 700.dp, height = 680.dp), title = t("Видео в аудио")) {
        CompositionLocalProvider(LocalLanguage provides labels) {
            MaterialTheme {
                Surface {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = {
                                chooseVideo()?.let {
                                    preview?.destroy()
                                    source = it
                                    values[0] = it.nameWithoutExtension
                                }
                            }) { Text(t("Выбрать видео")) }
                            val previewError = t("Не удалось открыть видео")
                            FilledTonalButton(enabled = source != null, onClick = {
                                preview?.destroy()
                                runCatching { preview = previewVideo(requireNotNull(source)) }
                                    .onFailure { actions.notify(previewError) }
                            }) { Text(t("Предпросмотр со звуком")) }
                        }
                        source?.let { Text(it.name) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("mp3", "m4a").forEach { choice ->
                                FilterChip(format == choice, { format = choice }, { Text(choice.uppercase()) })
                            }
                        }
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            fields.forEachIndexed { i, label ->
                                OutlinedTextField(values[i], { values[i] = it }, Modifier.fillMaxWidth(), label = { Text(t(label)) })
                            }
                            TextButton(onClick = {
                                val picker = JFileChooser()
                                picker.fileFilter = FileNameExtensionFilter("PNG / JPEG", "png", "jpg", "jpeg")
                                if (picker.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) cover = picker.selectedFile
                            }) { Text(cover?.name ?: t("Выбрать обложку")) }
                        }
                        val progress = t("Конвертация видео")
                        val success = t("Аудиофайл сохранён")
                        val exists = t("Файл уже существует")
                        val failure = t("Не удалось преобразовать видео")
                        Button(enabled = source != null && !actions.busy, onClick = {
                            val input = requireNotNull(source)
                            val extension = format
                            val tags = values.toList()
                            val artwork = cover?.toURI()?.toString()
                            val picker = JFileChooser().apply { selectedFile = File(input.nameWithoutExtension + "." + extension) }
                            if (picker.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return@Button
                            val destination = picker.selectedFile.let { if (it.extension.equals(extension, true)) it else File(it.path + "." + extension) }
                            if (destination.exists() || destination.canonicalFile == input.canonicalFile) {
                                actions.notify(exists)
                                return@Button
                            }
                            preview?.destroy()
                            actions.work(progress) {
                                withContext(Dispatchers.IO) {
                                    val dir = Files.createTempDirectory("lormusic-video").toFile()
                                    try {
                                        val audio = File(dir, "audio.$extension")
                                        runCatching {
                                            DesktopMedia.convertAudio(input.absolutePath, audio, extension)
                                            val track = Library.read(audio).copy(
                                                title = tags[0], artist = tags[1], album = tags[2], genre = tags[3],
                                                year = tags[4], trackNumber = tags[5], customLyrics = tags[6], albumArtUri = artwork
                                            )
                                            Library.writeTags(track)
                                            Files.copy(audio.toPath(), destination.toPath())
                                        }.getOrElse { error(failure) }
                                    } finally {
                                        dir.deleteRecursively()
                                    }
                                }
                                actions.notify(success)
                            }
                            close()
                        }) { Text(t("Сохранить")) }
                    }
                }
            }
        }
    }
}
