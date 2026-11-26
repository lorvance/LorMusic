@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.lormusic.desktop

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.lormusic.app.data.*
import com.lormusic.app.model.*
import java.io.File
import kotlinx.coroutines.*

@Composable
fun Settings(
    a: Actions,
    accent: Int,
    light: Boolean,
    language: String,
    setLanguage: (String) -> Unit,
    color: (Int) -> Unit,
    theme: (Boolean) -> Unit,
    dialog: (String) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SettingsGroup("Оформление") {
            Text(t("Акцентный цвет"), fontWeight = FontWeight.SemiBold)
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                accents.forEachIndexed { i, v ->
                    Box(
                        Modifier.size(43.dp).clip(CircleShape).clickable { color(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        val light = when (i) {
                            6 -> Color(0xFFE8A3B2)
                            7 -> Color(0xFFB2B5EF)
                            8 -> Color(0xFFF0B98F)
                            else -> lerp(v, Color.White, .35f)
                        }
                        val deep = when (i) {
                            6 -> Color(0xFF713344)
                            7 -> Color(0xFF393C76)
                            8 -> Color(0xFF80502F)
                            else -> lerp(v, Color.Black, .42f)
                        }
                        Canvas(Modifier.fillMaxSize()) {
                            drawArc(v, 180f, 180f, useCenter = true)
                            drawArc(deep, 90f, 90f, useCenter = true)
                            drawArc(light, 0f, 90f, useCenter = true)
                        }
                        if (i == accent) Icon(Icons.Rounded.Check, null, tint = Color(0xFF27212D))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t("Светлая тема"))
                    Text(
                        t("Тональные поверхности Material 3"),
                        fontSize = 12.sp,
                        color = c.onSurfaceVariant,
                    )
                }
                Switch(light, theme)
            }
        }
        DesktopAchievementSettings(a)
        SettingsGroup("Обновления") { UpdateSettings(a) }
        SettingsGroup("Плеер") {
            PreferenceSwitch("Градиент по обложке", a.state.playerGradient) {
                a.state.playerGradient = it
                a.changed()
            }
            PreferenceSwitch("Цвет элементов по обложке", a.state.dynamicColors) {
                a.state.dynamicColors = it
                a.changed()
            }
        }
        SettingsGroup("Воспроизведение") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("Скорость"), modifier = Modifier.weight(1f))
                listOf(.75f, 1f, 1.25f, 1.5f, 2f).forEach { v ->
                    FilterChip(
                        a.player.speed == v,
                        { a.player.changeSpeed(v) },
                        label = { Text("${v}×") },
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            TextButton({ dialog("Эквалайзер") }) {
                Icon(Icons.Rounded.GraphicEq, null)
                Spacer(Modifier.width(12.dp))
                Text(t("Эквалайзер"))
            }
            TextButton({ dialog("Таймер сна") }) {
                Icon(Icons.Rounded.Bedtime, null)
                Spacer(Modifier.width(12.dp))
                Text(t("Таймер сна"))
            }
            Text(
                t("Горячие клавиши: Ctrl + пробел — пауза; Ctrl + ← / → — сменить трек."),
                fontSize = 12.sp,
                color = c.onSurfaceVariant,
            )
        }
        SettingsGroup("Коллекция и аккаунты") {
            Row {
                TextButton({ a.importFiles(true) }) {
                    Icon(Icons.Rounded.CreateNewFolder, null)
                    Spacer(Modifier.width(8.dp))
                    Text(t("Добавить папку"))
                }
                TextButton({ a.rescan() }) {
                    Icon(Icons.Rounded.Refresh, null)
                    Text(t("Пересканировать"))
                }
            }
            a.state.folders.forEach { folder ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(folder, modifier = Modifier.weight(1f), fontSize = 12.sp, maxLines = 2)
                    IconButton({
                        a.state.folders.remove(folder)
                        a.changed()
                    }) {
                        Icon(Icons.Rounded.Close, "Убрать папку")
                    }
                }
            }
            TextButton({ dialog("Аккаунты") }) {
                Icon(Icons.Rounded.AccountCircle, null)
                Spacer(Modifier.width(8.dp))
                Text(t("YouTube Music и SoundCloud"))
            }
            Text(t("Папка загрузок"), fontSize = 12.sp, color = c.onSurfaceVariant)
            Text(a.state.downloadFolder, fontSize = 13.sp)
            TextButton({
                val f =
                    javax.swing.JFileChooser().apply {
                        fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                    }
                if (f.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                    a.state.downloadFolder = f.selectedFile.path
                    a.changed()
                }
            }) {
                Text(t("Изменить папку"))
            }
        }
        SettingsGroup("Язык интерфейса") {
            listOf(
                    "ru" to "Русский",
                    "en" to "English",
                    "uk" to "Українська",
                    "pl" to "Polski",
                    "de" to "Deutsch",
                )
                .forEach { (code, name) ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { setLanguage(code) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Flag(code)
                        Text(name, Modifier.weight(1f).padding(start = 12.dp))
                        if (language == code) Icon(Icons.Rounded.Check, null, tint = c.primary)
                    }
                }
        }
        SettingsGroup("Резервная копия") {
            Row {
                TextButton({ a.backup() }) { Text(t("Экспорт библиотеки")) }
                TextButton({ dialog("Восстановить") }) { Text(t("Восстановить")) }
            }
        }
        DesktopSystemInfo()

    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        t(title),
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        color = MaterialTheme.colorScheme.primary,
    )
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun SongActions(
    a: Actions,
    song: Song,
    close: () -> Unit,
    action: (String) -> Unit,
    remove: () -> Unit,
    inPlaylist: Boolean,
) {
    val c = MaterialTheme.colorScheme
    Dialog(
        onCloseRequest = close,
        title = "Действия с треком",
        state = rememberDialogState(width = 620.dp, height = 650.dp),
        resizable = true,
    ) {
        CenterDialogOnPlayer(window)
        MaterialTheme(colorScheme = c) {
            Column(
                Modifier.fillMaxSize()
                    .background(c.surface)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Art(song, 70.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 18.dp)) {
                        Text(
                            song.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                        )
                        Text(
                            "${song.artist} · ${song.album}",
                            fontSize = 13.sp,
                            color = c.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                    FilledIconButton(
                        {
                            a.player.play(song)
                            close()
                        },
                        Modifier.size(50.dp),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAction("В очередь", Icons.Rounded.PlaylistAdd, Modifier.weight(1f)) {
                        a.player.add(song, true)
                        close()
                    }
                    QuickAction("В плейлист", Icons.Rounded.QueueMusic, Modifier.weight(1f)) {
                        action("В плейлист")
                    }
                    QuickAction(
                        "Нравится",
                        if (song.id in a.state.liked) Icons.Rounded.Favorite
                        else Icons.Rounded.FavoriteBorder,
                        Modifier.weight(1f),
                    ) {
                        a.favorite(song)
                        close()
                    }
                    QuickAction("Избранное", Icons.Rounded.AutoAwesome, Modifier.weight(1f)) {
                        a.favorite(song, true)
                        close()
                    }
                }
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(c.surfaceVariant.copy(alpha = .55f))
                ) {
                    ActionRow("Исполнитель", song.artist, Icons.Rounded.Person) {
                        action("Исполнитель")
                    }
                    ActionRow(
                        "Редактор тегов и обложки",
                        "Название, исполнитель, альбом, жанр и текст",
                        Icons.Rounded.Sell,
                    ) {
                        action("Редактор тегов")
                    }
                }
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(c.surfaceVariant.copy(alpha = .55f))
                ) {
                    ActionRow("Сведения о треке", null, Icons.Rounded.Info) { action("Сведения") }
                    ActionRow("Эквалайзер", null, Icons.Rounded.GraphicEq) { action("Эквалайзер") }
                    ActionRow("Поделиться", null, Icons.Rounded.Share) {
                        a.share(song)
                        close()
                    }
                    if (!song.isLocal)
                        ActionRow("Скачать на компьютер", null, Icons.Rounded.Download) {
                            a.download(song)
                            close()
                        }
                    ActionRow(
                        if (song.id in a.state.hidden) "Показать в медиатеке"
                        else "Скрыть из медиатеки",
                        null,
                        Icons.Rounded.VisibilityOff,
                    ) {
                        if (!a.state.hidden.add(song.id)) a.state.hidden.remove(song.id)
                        a.changed()
                        close()
                    }
                }
                if (inPlaylist)
                    TextButton(remove) {
                        Icon(Icons.Rounded.PlaylistRemove, null)
                        Spacer(Modifier.width(10.dp))
                        Text(t("Удалить из плейлиста"))
                    }
                if (song.isLocal) DeleteFileButton(a, song, close)
            }
        }
    }
}

@Composable
fun DeleteFileButton(a: Actions, s: Song, close: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    TextButton(
        { confirm = true },
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) {
        Icon(Icons.Rounded.DeleteOutline, null)
        Spacer(Modifier.width(10.dp))
        Text(t("Удалить файл с устройства"))
    }
    if (confirm)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(t("Удалить аудиофайл?")) },
            text = {
                Text(s.title + "\n\n" + t("Файл будет удалён с компьютера. Это действие нельзя отменить."))
            },
            confirmButton = {
                TextButton({
                    confirm = false
                    a.work("Удаляю файл…") {
                        withContext(Dispatchers.IO) {
                            require(File(requireNotNull(s.filePath)).delete()) {
                                "Не удалось удалить файл"
                            }
                        }
                        a.state.library.removeAll { it.id == s.id }
                        a.state.playlists.forEach { it.songs.removeAll { it.id == s.id } }
                        a.changed()
                        close()
                    }
                }) {
                    Text(t("Удалить"))
                }
            },
            dismissButton = { TextButton({ confirm = false }) { Text(t("Отмена")) } },
        )
}

@Composable
fun QuickAction(text: String, icon: ImageVector, modifier: Modifier, click: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier
            .height(91.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(c.primary.copy(alpha = .13f))
            .clickable(onClick = click)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = c.primary, modifier = Modifier.size(26.dp))
        Text(t(text), Modifier.padding(top = 9.dp), fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
fun ActionRow(title: String, subtitle: String?, icon: ImageVector, click: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = click).padding(18.dp, 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(24.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.padding(horizontal = 18.dp).weight(1f)) {
            Text(t(title), fontSize = 15.sp)
            if (subtitle != null)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Rounded.ChevronRight,
            null,
            Modifier.size(19.dp),
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun EditTags(a: Actions, s: Song, close: () -> Unit) {
    val c = MaterialTheme.colorScheme
    var title by remember { mutableStateOf(s.title) }
    var artist by remember { mutableStateOf(s.artist) }
    var album by remember { mutableStateOf(s.album) }
    var genre by remember { mutableStateOf(s.genre) }
    var year by remember { mutableStateOf(s.year) }
    var track by remember { mutableStateOf(s.trackNumber) }
    var lyrics by remember { mutableStateOf(s.customLyrics.orEmpty()) }
    var art by remember { mutableStateOf(s.albumArtUri) }
    var status by remember { mutableStateOf("") }
    Dialog(
        onCloseRequest = close,
        title = "Редактор тегов",
        state = rememberDialogState(width = 620.dp, height = 740.dp),
        resizable = true,
    ) {
        CenterDialogOnPlayer(window)
        MaterialTheme(colorScheme = c) {
            Column(Modifier.fillMaxSize().background(c.surface).padding(24.dp)) {
                Text(t("Теги и обложка"), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Art(s.copy(albumArtUri = art), 90.dp)
                        Column(Modifier.padding(start = 18.dp)) {
                            TextButton({
                                val f = javax.swing.JFileChooser()
                                if (
                                    f.showOpenDialog(null) ==
                                        javax.swing.JFileChooser.APPROVE_OPTION
                                ) {
                                    val dest =
                                        File(
                                            dataDir,
                                            "covers/${s.id}-custom.${f.selectedFile.extension}",
                                        )
                                    dest.parentFile.mkdirs()
                                    f.selectedFile.copyTo(dest, true)
                                    art = dest.toURI().toString()
                                }
                            }) {
                                Text(t("Выбрать обложку"))
                            }
                            TextButton({
                                a.work("Ищу теги…") {
                                    val result =
                                        withContext(Dispatchers.IO) {
                                            TagFixEngine.searchOnlineTrackMetadata(
                                                "$artist $title",
                                                s.durationMs,
                                            )
                                        }
                                    if (result != null) {
                                        title = result.title
                                        artist = result.artist
                                        album = result.album
                                        year = result.year
                                        genre = result.genre
                                        art = result.coverUrl
                                        status = "Проверьте предложенные теги перед сохранением"
                                    } else status = "Совпадений не найдено"
                                }
                            }) {
                                Text(t("Найти теги в сети"))
                            }
                        }
                    }
                    if (s.isLocal)
                        TextButton({
                            a.work("Распознаю аудиоотпечаток…") {
                                val result = ShazamRecognizer.recognizeAudioFile(s.uri)
                                if (result != null) {
                                    title = result.title
                                    artist = result.artist
                                    album = result.album
                                    year = result.year
                                    genre = result.genre
                                    art = result.coverUrl
                                    status = "Распознано. Проверьте теги и нажмите «Сохранить»."
                                } else status = "Распознать трек не удалось"
                            }
                        }) {
                            Icon(Icons.Rounded.AutoFixHigh, null)
                            Spacer(Modifier.width(8.dp))
                            Text(t("Распознать аудио"))
                        }
                    if (status.isNotBlank()) Text(status, fontSize = 12.sp, color = c.primary)
                    listOf(
                            Triple("Название", title, { v: String -> title = v }),
                            Triple("Исполнитель", artist, { v: String -> artist = v }),
                            Triple("Альбом", album, { v: String -> album = v }),
                            Triple("Жанр", genre, { v: String -> genre = v }),
                            Triple("Год", year, { v: String -> year = v }),
                            Triple("Номер трека", track, { v: String -> track = v }),
                        )
                        .forEach { (label, v, set) ->
                            OutlinedTextField(
                                v,
                                set,
                                Modifier.fillMaxWidth(),
                                label = { Text(label) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                            )
                        }
                    OutlinedTextField(
                        lyrics,
                        { lyrics = it },
                        Modifier.fillMaxWidth().heightIn(min = 160.dp),
                        label = { Text(t("Текст песни / LRC")) },
                        shape = RoundedCornerShape(14.dp),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(close) { Text(t("Отмена")) }
                    Button({
                        a.work("Сохраняю теги…") {
                            var updated =
                                s.copy(
                                    title = title,
                                    artist = artist,
                                    album = album,
                                    genre = genre,
                                    year = year,
                                    trackNumber = track,
                                    customLyrics = lyrics,
                                    albumArtUri = art,
                                )
                            if (s.isLocal)
                                updated = withContext(Dispatchers.IO) { Library.writeTags(updated) }
                            a.state.library.replaceAll { if (it.id == s.id) updated else it }
                            a.state.history.replaceAll { if (it.id == s.id) updated else it }
                            a.state.playlists.forEach {
                                it.songs.replaceAll { old -> if (old.id == s.id) updated else old }
                            }
                            LyricsRepository.clearCacheForSong(s)
                            a.changed()
                            close()
                        }
                    }) {
                        Text(t("Сохранить"))
                    }
                }
            }
        }
    }
}

@Composable
fun EqualizerDialog(a: Actions, close: () -> Unit) {
    var enabled by remember { mutableStateOf(a.state.eqEnabled) }
    val values = remember { a.state.equalizer.toMutableStateList() }
    AlertDialog(
        onDismissRequest = close,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("Эквалайзер"), modifier = Modifier.weight(1f))
                Switch(
                    enabled,
                    {
                        enabled = it
                        a.state.eqEnabled = it
                        a.player.applyEqualizer()
                        a.changed()
                    },
                )
            }
        },
        text = {
            Column {
                Row {
                    TextButton({
                        values.indices.forEach {
                            values[it] = 0f
                            a.state.equalizer[it] = 0f
                        }
                        a.player.applyEqualizer()
                        a.changed()
                    }) {
                        Text(t("Сбросить"))
                    }
                    TextButton({
                        val preset = listOf(6f, 5f, 3f, 1f, 0f, 0f, 1f, 2f, 3f, 4f)
                        preset.forEachIndexed { i, v ->
                            values[i] = v
                            a.state.equalizer[i] = v
                        }
                        a.player.applyEqualizer()
                        a.changed()
                    }) {
                        Text(t("Бас"))
                    }
                }
                Column(Modifier.heightIn(max = 390.dp).verticalScroll(rememberScrollState())) {
                    listOf(
                            "31 Гц",
                            "62 Гц",
                            "125 Гц",
                            "250 Гц",
                            "500 Гц",
                            "1 кГц",
                            "2 кГц",
                            "4 кГц",
                            "8 кГц",
                            "16 кГц",
                        )
                        .forEachIndexed { i, hz ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(hz, fontSize = 11.sp, modifier = Modifier.width(46.dp))
                                Slider(
                                    values[i],
                                    {
                                        values[i] = it
                                        a.state.equalizer[i] = it
                                        a.player.applyEqualizer()
                                    },
                                    onValueChangeFinished = { a.changed() },
                                    valueRange = -20f..20f,
                                    enabled = enabled,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    t("%+.0f").format(values[i]),
                                    modifier = Modifier.width(28.dp),
                                    fontSize = 11.sp,
                                )
                            }
                        }
                }
            }
        },
        confirmButton = { TextButton(close) { Text(t("Готово")) } },
    )
}

@Composable
fun AccountsDialog(a: Actions, close: () -> Unit) {
    var youtube by remember { mutableStateOf(a.state.youtubeCookie) }
    var sc by remember { mutableStateOf(a.state.soundcloudToken) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(t("Подключить аккаунты")) },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    t(
                        "Войдите во встроенном окне. При первом входе загружается Chromium. Сессия хранится на этом компьютере."
                    ),
                    fontSize = 13.sp,
                )
                DialogAction("Войти в YouTube Music", Icons.Rounded.PlayCircle) {
                    LoginBrowser.open(a, true)
                    close()
                }
                DialogAction("Войти в SoundCloud", Icons.Rounded.Cloud) {
                    LoginBrowser.open(a, false)
                    close()
                }
                if (a.state.youtubeCookie.isNotBlank() || a.state.soundcloudToken.isNotBlank())
                    TextButton({
                        LoginBrowser.signOut()
                        a.state.youtubeCookie = ""
                        a.state.soundcloudToken = ""
                        YouTubeMusicRepository.currentAuthCookie = null
                        SoundCloudRepository.currentAuthToken = null
                        SoundCloudRepository.currentProfile = null
                        a.changed()
                        close()
                    }) {
                        Text(t("Отключить аккаунты"))
                    }
                Text(t("Ручной импорт сессии"), fontSize = 12.sp)
                TextButton({ browse("https://music.youtube.com") }) {
                    Text(t("Открыть YouTube Music"))
                }
                OutlinedTextField(
                    youtube,
                    { youtube = it },
                    label = { Text(t("YouTube Music: Cookie")) },
                    visualTransformation =
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
                Text(
                    t(
                        "Cookie заголовок запроса к music.youtube.com: DevTools → Network → Headers. Нужен SAPISID и связанные cookies."
                    ),
                    fontSize = 11.sp,
                )
                TextButton({ browse("https://soundcloud.com") }) { Text(t("Открыть SoundCloud")) }
                OutlinedTextField(
                    sc,
                    { sc = it },
                    label = { Text(t("SoundCloud: OAuth token")) },
                    visualTransformation =
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                )
                Text(
                    t("Authorization из запроса к api-v2.soundcloud.com в DevTools браузера."),
                    fontSize = 11.sp,
                )
            }
        },
        confirmButton = {
            TextButton({
                a.state.youtubeCookie = youtube.trim()
                a.state.soundcloudToken = sc.trim()
                a.changed()
                a.syncAccounts()
                close()
            }) {
                Text(t("Подключить"))
            }
        },
        dismissButton = { TextButton(close) { Text(t("Отмена")) } },
    )
}

@Composable
fun ArtistDialog(a: Actions, s: Song, close: () -> Unit, add: (Song) -> Unit) {
    var tracks by remember {
        mutableStateOf(a.state.library.filter { it.artist.equals(s.artist, true) })
    }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(s.artist) {
        runCatching {
            val profile = YouTubeMusicRepository.fetchArtistProfile(s.artist)
            tracks =
                (tracks + profile.topTracks + profile.albums.values.flatten() + profile.singles)
                    .distinctBy { it.id }
        }
        loading = false
    }
    val c = MaterialTheme.colorScheme
    Dialog(
        onCloseRequest = close,
        title = s.artist,
        state = rememberDialogState(width = 800.dp, height = 680.dp),
        resizable = true,
    ) {
        CenterDialogOnPlayer(window)
        MaterialTheme(colorScheme = c) {
            Column(Modifier.fillMaxSize().background(c.surface).padding(24.dp)) {
                Text(s.artist, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Row {
                    Button({ tracks.firstOrNull()?.let { a.player.play(it, tracks) } }) {
                        Text(t("Слушать"))
                    }
                    TextButton(close) { Text(t("Закрыть")) }
                }
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                LazyColumn {
                    itemsIndexed(tracks) { i, t ->
                        TrackRow(
                            t,
                            i,
                            a.player.song?.id == t.id,
                            a.player.playing,
                            t.id in a.state.liked,
                            { a.player.play(t, tracks) },
                            { add(t) },
                            { a.favorite(t) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RepairDialog(a: Actions, close: () -> Unit) {
    val c = MaterialTheme.colorScheme
    var proposals by remember { mutableStateOf<List<TagFixProposal>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var status by remember {
        mutableStateOf(
            "Найдём повреждённые теги и предложим исправления. Изменения сохраняются только после вашего подтверждения."
        )
    }
    val selected = remember { mutableStateMapOf<String, Boolean>() }
    Dialog(
        onCloseRequest = close,
        title = "Исправление тегов",
        state = rememberDialogState(width = 760.dp, height = 640.dp),
        resizable = true,
    ) {
        CenterDialogOnPlayer(window)
        MaterialTheme(colorScheme = c) {
            Column(
                Modifier.fillMaxSize().background(c.surface).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(t("Исправление тегов"), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(status, fontSize = 13.sp, color = c.onSurfaceVariant)
                Row {
                    Button(
                        {
                            a.scope.launch {
                                scanning = true
                                try {
                                    val candidates =
                                        a.state.library.filter {
                                            it.isLocal && TagFixEngine.isBrokenTrack(it)
                                        }
                                    proposals =
                                        TagFixEngine.analyzeTracks(candidates) { current, total ->
                                            javax.swing.SwingUtilities.invokeLater {
                                                status = "Проверка: $current / $total"
                                            }
                                        }
                                    proposals.forEach { selected[it.originalSong.id] = true }
                                    status =
                                        "Предложений: ${proposals.size}. Проверьте названия и исполнителей."
                                } catch (e: Throwable) {
                                    status = e.message ?: "Ошибка поиска тегов"
                                } finally {
                                    scanning = false
                                }
                            }
                        },
                        enabled = !scanning,
                    ) {
                        Text(t("Проверить медиатеку"))
                    }
                    TextButton(close) { Text(t("Закрыть")) }
                }
                if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth())
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(proposals, key = { it.originalSong.id }) { proposal ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(c.surfaceVariant)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                selected[proposal.originalSong.id] == true,
                                { selected[proposal.originalSong.id] = it },
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${proposal.originalSong.artist} — ${proposal.originalSong.title}",
                                    fontSize = 12.sp,
                                    color = c.onSurfaceVariant,
                                )
                                Text(
                                    "${proposal.proposedArtist} — ${proposal.proposedTitle}",
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(proposal.reason, fontSize = 11.sp, color = c.onSurfaceVariant)
                            }
                        }
                    }
                }
                Button(
                    {
                        a.work("Применяю исправления…") {
                            val chosen = proposals.filter { selected[it.originalSong.id] == true }
                            withContext(Dispatchers.IO) {
                                chosen.forEach { v ->
                                    val original = v.originalSong
                                    val updated =
                                        Library.writeTags(
                                            original.copy(
                                                title = v.proposedTitle,
                                                artist = v.proposedArtist,
                                                album = v.proposedAlbum,
                                                genre = v.proposedGenre,
                                                year = v.proposedYear,
                                                albumArtUri = v.proposedCoverUri,
                                            )
                                        )
                                    withContext(Dispatchers.Main) {
                                        a.state.library.replaceAll {
                                            if (it.id == original.id) updated else it
                                        }
                                        a.state.playlists.forEach { list ->
                                            list.songs.replaceAll {
                                                if (it.id == original.id) updated else it
                                            }
                                        }
                                        a.changed()
                                    }
                                }
                            }
                            close()
                        }
                    },
                    enabled = !scanning && proposals.any { selected[it.originalSong.id] == true },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(t("Применить выбранное"))
                }
            }
        }
    }
}

@Composable
private fun CenterDialogOnPlayer(dialog: java.awt.Window) {
    LaunchedEffect(dialog) {
        delay(120)
        val parent =
            java.awt.Window.getWindows().firstOrNull {
                it.isVisible && it is java.awt.Frame && it.title == "LorMusic"
            }
        dialog.setLocationRelativeTo(parent)
    }
}
