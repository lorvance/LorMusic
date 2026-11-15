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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.lormusic.app.data.*
import com.lormusic.app.model.*
import com.lormusic.app.model.RepeatMode
import java.awt.Desktop
import java.io.File
import java.net.URI
import javax.imageio.ImageIO
import kotlinx.coroutines.*
import org.jetbrains.skia.Image

val accents =
    listOf(
        Color(0xFFC3A5FF),
        Color(0xFF9DD9BA),
        Color(0xFFF2BE8D),
        Color(0xFF9EC9FF),
        Color(0xFFF0A9C8),
        Color(0xFFE1D084),
    )

fun clock(ms: Long): String {
    val s = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

fun browse(url: String) {
    runCatching { Desktop.getDesktop().browse(URI(url)) }
}

fun main(args: Array<String>) {
    if (args.contains("--network-test")) {
        NetworkChecks.run()
        return
    }
    if (args.contains("--browser-test")) {
        try {
            val engine = LoginBrowser.engine()
            println("PASS: Chromium initialized: ${engine.version}")
            LoginBrowser.shutdown()
        } catch (e: Throwable) {
            e.printStackTrace()
            kotlin.system.exitProcess(1)
        }
        return
    }

    if (args.contains("--self-test")) {
        DesktopChecks.run()
        return
    }
    if (!args.any { it.startsWith("--screenshot=") } && !InstanceLock.claim(args)) return
    application {
        val ws =
            rememberWindowState(
                position =
                    if (args.any { it.startsWith("--screenshot=") }) WindowPosition(0.dp, 0.dp)
                    else WindowPosition.PlatformDefault,
                width =
                    (args
                            .firstOrNull { it.startsWith("--width=") }
                            ?.substringAfter("=")
                            ?.toIntOrNull() ?: 1340)
                        .dp,
                height =
                    (args
                            .firstOrNull { it.startsWith("--height=") }
                            ?.substringAfter("=")
                            ?.toIntOrNull() ?: 880)
                        .dp,
            )
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        val state = remember { Library.load() }
        val actions = remember {
            Actions(state, scope) { message -> scope.launch { snackbar.showSnackbar(message) } }
        }
        Window(
            onCloseRequest = {
                LoginBrowser.shutdown()
                actions.player.close()
                exitApplication()
            },
            title = "LorMusic",
            icon =
                remember {
                    androidx.compose.ui.graphics.painter.BitmapPainter(
                        Image.makeFromEncoded(
                                object {}
                                    .javaClass
                                    .getResourceAsStream("/lormusic.png")!!
                                    .readBytes()
                            )
                            .toComposeImageBitmap()
                    )
                },
            state = ws,
            onKeyEvent = { e ->
                if (e.type == KeyEventType.KeyDown && e.isCtrlPressed) {
                    when (e.key) {
                        Key.DirectionRight -> {
                            actions.player.next()
                            true
                        }
                        Key.DirectionLeft -> {
                            actions.player.previous()
                            true
                        }
                        Key.Spacebar -> {
                            actions.player.toggle()
                            true
                        }
                        else -> false
                    }
                } else false
            },
        ) {
            LaunchedEffect(Unit) {
                args
                    .firstOrNull { it.startsWith("--screenshot=") }
                    ?.let { arg ->
                        scope.launch {
                            delay(4000)
                            val image =
                                java.awt
                                    .Robot()
                                    .createScreenCapture(
                                        java.awt.Rectangle(window.locationOnScreen, window.size)
                                    )
                            ImageIO.write(image, "png", File(arg.substringAfter("=")))
                            actions.player.close()
                            exitApplication()
                        }
                    }
                window.minimumSize = java.awt.Dimension(850, 620)
                window.dropTarget =
                    object : java.awt.dnd.DropTarget() {
                        override fun drop(event: java.awt.dnd.DropTargetDropEvent) {
                            try {
                                event.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY)
                                @Suppress("UNCHECKED_CAST")
                                val files =
                                    event.transferable.getTransferData(
                                        java.awt.datatransfer.DataFlavor.javaFileListFlavor
                                    ) as List<File>
                                actions.importPaths(files)
                                event.dropComplete(true)
                            } catch (e: Exception) {
                                event.dropComplete(false)
                            }
                        }
                    }
                InstanceLock.handler = { paths ->
                    javax.swing.SwingUtilities.invokeLater {
                        window.isVisible = true
                        window.toFront()
                        window.requestFocus()
                        val files = paths.map { File(it) }.filter { it.exists() }
                        if (files.isNotEmpty()) actions.importPaths(files, true)
                    }
                }
                val opened =
                    args.filter { !it.startsWith("--") }.map { File(it) }.filter { it.exists() }
                if (opened.isNotEmpty()) actions.importPaths(opened, true)
                YouTubeMusicRepository.currentAuthCookie = state.youtubeCookie.ifBlank { null }
                SoundCloudRepository.currentAuthToken = state.soundcloudToken.ifBlank { null }
            }
            LorMusic(actions, snackbar)
        }
    }
}

@Composable
fun LorMusic(a: Actions, snackbar: SnackbarHostState) {
    val revision = a.revision
    val s = a.state
    val p = a.player
    var accent by remember { mutableStateOf(s.accent) }
    var light by remember { mutableStateOf(s.light) }
    var language by remember { mutableStateOf(s.language) }
    val primary = accents[accent.coerceIn(accents.indices)]
    val scheme =
        if (light)
            lightColorScheme(
                primary =
                    Color(primary.value).copy(alpha = 1f).let {
                        Color((it.red * .52f), (it.green * .52f), (it.blue * .52f))
                    },
                secondary = primary,
                background = Color(0xFFF8F5FC),
                surface = Color(0xFFF8F5FC),
                surfaceVariant = Color(0xFFEDE7F2),
                onSurface = Color(0xFF25212B),
            )
        else
            darkColorScheme(
                primary = primary,
                onPrimary = Color(0xFF302040),
                secondary = primary,
                background = Color(0xFF111015),
                surface = Color(0xFF111015),
                surfaceVariant = Color(0xFF25222C),
                onSurface = Color(0xFFECE6F1),
                onSurfaceVariant = Color(0xFFB9B1C3),
                outline = Color(0xFF514A5C),
            )
    MaterialTheme(colorScheme = scheme, typography = Typography()) {
        CompositionLocalProvider(
            LocalContentColor provides scheme.onSurface,
            LocalLanguage provides language,
        ) {
            var page by remember {
                mutableStateOf(System.getProperty("lormusic.initialPage", "Главная"))
            }
            var query by remember { mutableStateOf("") }
            var panel by remember {
                mutableStateOf(System.getProperty("lormusic.initialPanel", ""))
            }
            var sort by remember { mutableStateOf("Недавно добавленные") }
            var selectedList by remember { mutableStateOf<String?>(null) }
            var menuSong by remember {
                mutableStateOf<Song?>(
                    if (System.getProperty("lormusic.openActions") == "true")
                        s.library.firstOrNull()
                    else null
                )
            }
            var dialog by remember { mutableStateOf("") }
            var target by remember { mutableStateOf<Song?>(null) }
            var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
            var confirmText by remember { mutableStateOf("") }
            fun showSong(song: Song) {
                menuSong = song
            }
            fun go(next: String) {
                page = next
                query = ""
                selectedList = null
                if (next == "YouTube Music" || next == "SoundCloud") a.feed(next)
            }
            val playlist = s.playlists.firstOrNull { it.id == selectedList }
            val all =
                (s.library + s.history + s.playlists.flatMap { it.songs }).distinctBy { it.id }
            val base =
                when {
                    playlist != null -> playlist.songs
                    page == "YouTube Music" || page == "SoundCloud" -> a.remote
                    page == "Мне нравится" -> all.filter { it.id in s.liked }
                    page == "Избранное" -> all.filter { it.id in s.favorites }
                    page == "Загрузки" ->
                        s.library.filter { it.filePath?.startsWith(s.downloadFolder) == true }
                    page == "Скрытые" -> all.filter { it.id in s.hidden }
                    else -> s.library.filter { it.id !in s.hidden }
                }
            val filtered =
                base.filter { "${it.title} ${it.artist} ${it.album}".contains(query, true) }
            val songs =
                when (sort) {
                    "Название" -> filtered.sortedBy { it.title.lowercase() }
                    "Исполнитель" -> filtered.sortedBy { it.artist.lowercase() }
                    "Альбом" -> filtered.sortedBy { it.album.lowercase() }
                    "Длительность" -> filtered.sortedByDescending { it.durationMs }
                    else ->
                        if (playlist != null) filtered
                        else filtered.sortedByDescending { it.dateAddedSec }
                }
            BoxWithConstraints(Modifier.fillMaxSize().background(scheme.background)) {
                val compactPanel = maxWidth < 1100.dp
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.weight(1f)) {
                        Column(
                            Modifier.width(218.dp)
                                .fillMaxHeight()
                                .padding(14.dp, 18.dp)
                                .background(
                                    scheme.surfaceVariant.copy(alpha = .35f),
                                    RoundedCornerShape(30.dp),
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                Modifier.padding(6.dp, 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier.size(38.dp)
                                        .background(primary, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.MusicNote, null, tint = scheme.onPrimary)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        t("LorMusic"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        maxLines = 1,
                                    )
                                    Text(
                                        t("DESKTOP"),
                                        fontSize = 10.sp,
                                        letterSpacing = 2.sp,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            val nav =
                                listOf(
                                    "Главная" to Icons.Rounded.Home,
                                    "Треки" to Icons.Rounded.LibraryMusic,
                                    "YouTube Music" to Icons.Rounded.PlayCircle,
                                    "SoundCloud" to Icons.Rounded.Cloud,
                                    "Плейлисты" to Icons.Rounded.QueueMusic,
                                    "Мне нравится" to Icons.Rounded.FavoriteBorder,
                                    "Избранное" to Icons.Rounded.AutoAwesome,
                                    "Загрузки" to Icons.Rounded.Download,
                                )
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                nav.forEach { (name, icon) ->
                                    NavItem(name, icon, page == name) { go(name) }
                                }
                            }
                            HorizontalDivider(
                                Modifier.padding(10.dp),
                                color = scheme.outline.copy(alpha = .35f),
                            )
                            NavItem("Настройки", Icons.Rounded.Settings, page == "Настройки") {
                                go("Настройки")
                            }
                            Text(
                                t("1.8.0 · Desktop preview"),
                                Modifier.padding(16.dp, 8.dp),
                                fontSize = 10.sp,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        Column(
                            Modifier.weight(1f)
                                .fillMaxHeight()
                                .padding(start = 12.dp, end = 22.dp, top = 26.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (playlist != null) "ПЛЕЙЛИСТ"
                                        else
                                            when (page) {
                                                "Главная" -> "ВАША МУЗЫКА. ВАШ РИТМ."
                                                "YouTube Music",
                                                "SoundCloud" -> "ОТКРЫВАЙТЕ НОВОЕ"
                                                else -> "LORMUSIC"
                                            },
                                        color = scheme.primary,
                                        fontSize = 10.sp,
                                        letterSpacing = 2.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        playlist?.name ?: t(page),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (page != "Настройки") {
                                    OutlinedTextField(
                                        query,
                                        {
                                            query = it
                                            if (page == "YouTube Music" || page == "SoundCloud")
                                                a.search(page, it)
                                        },
                                        Modifier.widthIn(min = 190.dp, max = 320.dp)
                                            .weight(.65f, false),
                                        placeholder = { Text(t("Поиск музыки"), fontSize = 14.sp) },
                                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                                        trailingIcon = {
                                            if (query.isNotEmpty())
                                                IconButton({ query = "" }) {
                                                    Icon(Icons.Rounded.Close, null)
                                                }
                                        },
                                        singleLine = true,
                                        shape = CircleShape,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton({ dialog = "Импорт" }) { Icon(Icons.Rounded.Add, null) }
                                var topMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton({ topMenu = true }) {
                                        Icon(Icons.Rounded.MoreVert, "Ещё")
                                    }
                                    DropdownMenu(topMenu, { topMenu = false }) {
                                        DropdownMenuItem(
                                            { Text(t("Обновить медиатеку")) },
                                            {
                                                topMenu = false
                                                a.rescan()
                                            },
                                        )
                                        DropdownMenuItem(
                                            { Text(t("Исправление тегов")) },
                                            {
                                                topMenu = false
                                                dialog = "Исправление тегов"
                                            },
                                        )
                                        DropdownMenuItem(
                                            { Text(t("Скрытые треки")) },
                                            {
                                                topMenu = false
                                                go("Скрытые")
                                            },
                                        )
                                        DropdownMenuItem(
                                            { Text(t("Подключить аккаунты")) },
                                            {
                                                topMenu = false
                                                dialog = "Аккаунты"
                                            },
                                        )
                                    }
                                }
                            }
                            if (a.busy) {
                                LinearProgressIndicator(
                                    Modifier.fillMaxWidth().padding(top = 12.dp),
                                    color = primary,
                                )
                                Text(
                                    a.progress,
                                    Modifier.padding(top = 5.dp),
                                    fontSize = 11.sp,
                                    color = scheme.onSurfaceVariant,
                                )
                            } else Spacer(Modifier.height(20.dp))
                            when {
                                page == "Настройки" ->
                                    Settings(
                                        a,
                                        accent,
                                        light,
                                        language,
                                        {
                                            language = it
                                            s.language = it
                                            a.changed()
                                        },
                                        {
                                            accent = it
                                            s.accent = it
                                            a.changed()
                                        },
                                        {
                                            light = it
                                            s.light = it
                                            a.changed()
                                        },
                                        { dialog = it },
                                    )
                                page == "Плейлисты" && playlist == null ->
                                    PlaylistPage(
                                        a,
                                        { selectedList = it.id },
                                        {
                                            target = null
                                            dialog = "Создать плейлист"
                                        },
                                        { list ->
                                            confirmText =
                                                "Удалить плейлист «${list.name}»? Аудиофайлы останутся на устройстве."
                                            pending = {
                                                s.playlists.remove(list)
                                                a.changed()
                                            }
                                        },
                                    )
                                page == "Главная" && query.isBlank() ->
                                    Home(
                                        a,
                                        { go(it) },
                                        { p.play(it, s.history) },
                                        { showSong(it) },
                                        { dialog = "Импорт" },
                                    )
                                else -> {
                                    if (page == "YouTube Music" && query.isBlank())
                                        Row(
                                            Modifier.padding(bottom = 10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            YouTubeMusicRepository.WaveMood.entries.forEach { mood
                                                ->
                                                AssistChip(
                                                    onClick = {
                                                        a.work("Готовлю вашу волну…") {
                                                            a.remote =
                                                                YouTubeMusicRepository
                                                                    .loadMyWaveTracks(
                                                                        s.history + s.library,
                                                                        mood,
                                                                        s.youtubeCookie.ifBlank {
                                                                            null
                                                                        },
                                                                    )
                                                        }
                                                    },
                                                    label = { Text(t(mood.label)) },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Rounded.AutoAwesome,
                                                            null,
                                                            Modifier.size(16.dp),
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 14.dp),
                                    ) {
                                        FilledTonalButton(
                                            { songs.firstOrNull()?.let { p.play(it, songs) } },
                                            enabled = songs.isNotEmpty(),
                                            shape = CircleShape,
                                        ) {
                                            Icon(
                                                Icons.Rounded.PlayArrow,
                                                null,
                                                Modifier.size(18.dp),
                                            )
                                            Spacer(Modifier.width(7.dp))
                                            Text(t("Слушать всё"))
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        IconButton(
                                            {
                                                if (songs.isNotEmpty()) {
                                                    p.shuffle = true
                                                    p.play(songs.random(), songs)
                                                }
                                            },
                                            enabled = songs.isNotEmpty(),
                                        ) {
                                            Icon(Icons.Rounded.Shuffle, null)
                                        }
                                        Text(
                                            "${songs.size} треков",
                                            fontSize = 12.sp,
                                            color = scheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (playlist != null) {
                                            IconButton({
                                                target = null
                                                dialog = "Переименовать"
                                            }) {
                                                Icon(Icons.Rounded.Edit, null)
                                            }
                                            IconButton({
                                                val chooser =
                                                    javax.swing.JFileChooser().apply {
                                                        selectedFile = File("${playlist.name}.m3u8")
                                                    }
                                                if (
                                                    chooser.showSaveDialog(null) ==
                                                        javax.swing.JFileChooser.APPROVE_OPTION
                                                )
                                                    Library.exportPlaylist(
                                                        playlist.songs,
                                                        chooser.selectedFile,
                                                    )
                                            }) {
                                                Icon(Icons.Rounded.IosShare, null)
                                            }
                                        }
                                        var expanded by remember { mutableStateOf(false) }
                                        Box {
                                            TextButton({ expanded = true }) {
                                                Icon(Icons.Rounded.Sort, null, Modifier.size(18.dp))
                                                Text(sort, fontSize = 12.sp)
                                            }
                                            DropdownMenu(expanded, { expanded = false }) {
                                                listOf(
                                                        "Недавно добавленные",
                                                        "Название",
                                                        "Исполнитель",
                                                        "Альбом",
                                                        "Длительность",
                                                    )
                                                    .forEach { v ->
                                                        DropdownMenuItem(
                                                            { Text(v) },
                                                            {
                                                                sort = v
                                                                expanded = false
                                                            },
                                                        )
                                                    }
                                            }
                                        }
                                    }
                                    if (songs.isEmpty())
                                        Empty(
                                            if (page == "YouTube Music" || page == "SoundCloud")
                                                "Найдите свою следующую любимую песню"
                                            else "Здесь появится ваша музыка",
                                            if (page == "YouTube Music" || page == "SoundCloud")
                                                "Введите название трека или исполнителя в поиске"
                                            else
                                                "Добавьте файлы или папку. Теги и обложки загрузятся автоматически.",
                                        ) {
                                            dialog = "Импорт"
                                        }
                                    else
                                        TrackList(
                                            songs,
                                            p,
                                            s.liked,
                                            { p.play(it, songs) },
                                            { showSong(it) },
                                            { a.favorite(it) },
                                        )
                                }
                            }
                        }
                        AnimatedVisibility(
                            panel.isNotBlank() && !compactPanel,
                            enter = expandHorizontally(),
                            exit = shrinkHorizontally(),
                        ) {
                            SidePanel(a, panel, { panel = "" }, { showSong(it) })
                        }
                    }
                    PlayerBar(
                        a,
                        panel,
                        { panel = if (panel == it) "" else it },
                        { dialog = "Эквалайзер" },
                    )
                }
                if (compactPanel && panel.isNotBlank()) {
                    Surface(
                        modifier =
                            Modifier.align(Alignment.TopEnd)
                                .width(320.dp)
                                .fillMaxHeight()
                                .padding(bottom = 114.dp),
                        tonalElevation = 8.dp,
                        shadowElevation = 12.dp,
                    ) {
                        SidePanel(a, panel, { panel = "" }, { showSong(it) })
                    }
                }
                SnackbarHost(
                    snackbar,
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 126.dp),
                )
            }
            if (menuSong != null) {
                val song = menuSong!!
                SongActions(
                    a,
                    song,
                    { menuSong = null },
                    { key ->
                        target = song
                        menuSong = null
                        dialog = key
                    },
                    {
                        confirmText = "Удалить «${song.title}» из плейлиста?"
                        pending = {
                            playlist?.songs?.removeAll { it.id == song.id }
                            a.changed()
                        }
                        menuSong = null
                    },
                    playlist != null,
                )
            }
            if (pending != null)
                AlertDialog(
                    onDismissRequest = { pending = null },
                    title = { Text(t("Подтверждение")) },
                    text = { Text(confirmText) },
                    confirmButton = {
                        TextButton({
                            pending?.invoke()
                            pending = null
                        }) {
                            Text(t("Удалить"))
                        }
                    },
                    dismissButton = { TextButton({ pending = null }) { Text(t("Отмена")) } },
                )
            when (dialog) {
                "Импорт" ->
                    AlertDialog(
                        onDismissRequest = { dialog = "" },
                        title = { Text(t("Добавить музыку")) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                DialogAction("Аудиофайлы", Icons.Rounded.AudioFile) {
                                    dialog = ""
                                    a.importFiles()
                                }
                                DialogAction("Папка с музыкой", Icons.Rounded.Folder) {
                                    dialog = ""
                                    a.importFiles(true)
                                }
                                DialogAction("Плейлист M3U / M3U8", Icons.Rounded.QueueMusic) {
                                    dialog = ""
                                    a.importPlaylist()
                                }
                            }
                        },
                        confirmButton = { TextButton({ dialog = "" }) { Text(t("Закрыть")) } },
                    )
                "Создать плейлист",
                "Переименовать" ->
                    TextDialog(
                        if (dialog == "Переименовать") "Название плейлиста" else "Новый плейлист",
                        if (dialog == "Переименовать") playlist?.name.orEmpty() else "",
                        { dialog = "" },
                    ) { name ->
                        if (name.isNotBlank()) {
                            if (dialog == "Переименовать") playlist?.name = name
                            else
                                s.playlists.add(
                                    UserList(
                                        name = name,
                                        songs = target?.let { mutableListOf(it) } ?: mutableListOf(),
                                    )
                                )
                            target = null
                            a.changed()
                            dialog = ""
                        }
                    }
                "В плейлист" ->
                    AlertDialog(
                        onDismissRequest = { dialog = "" },
                        title = { Text(t("Добавить в плейлист")) },
                        text = {
                            Column(
                                Modifier.heightIn(max = 340.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                s.playlists.forEach { list ->
                                    TextButton({
                                        target?.let { song ->
                                            if (list.songs.none { it.id == song.id })
                                                list.songs.add(song)
                                        }
                                        a.changed()
                                        dialog = ""
                                    }) {
                                        Text(list.name)
                                    }
                                }
                                TextButton({ dialog = "Создать плейлист" }) {
                                    Text(t("+ Создать плейлист"))
                                }
                            }
                        },
                        confirmButton = { TextButton({ dialog = "" }) { Text(t("Отмена")) } },
                    )
                "Редактор тегов" -> target?.let { EditTags(a, it) { dialog = "" } }
                "Сведения" ->
                    target?.let { song ->
                        AlertDialog(
                            onDismissRequest = { dialog = "" },
                            title = { Text(song.title) },
                            text = {
                                Text(
                                    "Исполнитель: ${song.artist}\nАльбом: ${song.album}\nДлительность: ${clock(song.durationMs)}\nЖанр: ${song.genre}\nГод: ${song.year}\n${song.filePath ?: song.uri}"
                                )
                            },
                            confirmButton = { TextButton({ dialog = "" }) { Text(t("Закрыть")) } },
                        )
                    }
                "Исполнитель" ->
                    target?.let { song ->
                        ArtistDialog(
                            a,
                            song,
                            { dialog = "" },
                            {
                                target = it
                                dialog = "В плейлист"
                            },
                        )
                    }
                "Исправление тегов" -> RepairDialog(a) { dialog = "" }
                "Эквалайзер" -> EqualizerDialog(a) { dialog = "" }
                "Аккаунты" -> AccountsDialog(a) { dialog = "" }
                "Таймер сна" ->
                    AlertDialog(
                        onDismissRequest = { dialog = "" },
                        title = { Text(t("Таймер сна")) },
                        text = {
                            Column {
                                listOf(0, 15, 30, 45, 60, 90).forEach { m ->
                                    TextButton({
                                        p.timer(m)
                                        dialog = ""
                                    }) {
                                        Text(if (m == 0) "Выключить" else "Через $m мин")
                                    }
                                }
                            }
                        },
                        confirmButton = { TextButton({ dialog = "" }) { Text(t("Отмена")) } },
                    )
                "Восстановить" ->
                    AlertDialog(
                        onDismissRequest = { dialog = "" },
                        title = { Text(t("Восстановить библиотеку?")) },
                        text = {
                            Text(
                                t(
                                    "Текущие плейлисты и настройки будут заменены выбранной резервной копией."
                                )
                            )
                        },
                        confirmButton = {
                            TextButton({
                                dialog = ""
                                val f = javax.swing.JFileChooser()
                                if (
                                    f.showOpenDialog(null) ==
                                        javax.swing.JFileChooser.APPROVE_OPTION
                                )
                                    a.work("Восстанавливаю…") {
                                        val backup =
                                            gson.fromJson(
                                                f.selectedFile.readText(),
                                                SavedState::class.java,
                                            )
                                        s.library = backup.library
                                        s.playlists = backup.playlists
                                        s.liked = backup.liked
                                        s.favorites = backup.favorites
                                        s.history = backup.history
                                        s.folders = backup.folders
                                        s.hidden = backup.hidden
                                        accent = backup.accent.coerceIn(accents.indices)
                                        s.accent = accent
                                        light = backup.light
                                        s.light = light
                                        language = backup.language
                                        s.language = language
                                        s.downloadFolder = backup.downloadFolder
                                        s.equalizer =
                                            backup.equalizer
                                                .take(10)
                                                .map { it.coerceIn(-20f, 20f) }
                                                .toMutableList()
                                                .apply { while (size < 10) add(0f) }
                                        s.eqEnabled = backup.eqEnabled
                                        p.changeVolume(backup.volume)
                                        p.changeSpeed(backup.speed.coerceIn(.5f, 2f))
                                        p.applyEqualizer()
                                        p.restoreQueue(
                                            backup.queue,
                                            backup.lastSong,
                                            backup.lastPosition,
                                        )
                                        a.changed()
                                    }
                            }) {
                                Text(t("Выбрать копию"))
                            }
                        },
                        dismissButton = { TextButton({ dialog = "" }) { Text(t("Отмена")) } },
                    )
            }
        }
    }
}

@Composable
fun NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) c.primary.copy(alpha = .20f) else Color.Transparent)
    Row(
        Modifier.fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(14.dp, 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, label, Modifier.size(21.dp), if (selected) c.primary else c.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            t(label),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) c.primary else c.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
fun Art(song: Song?, size: Dp = 48.dp) {
    var bitmap by remember(song?.albumArtUri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(song?.albumArtUri) {
        bitmap =
            withContext(Dispatchers.IO) {
                runCatching {
                        val uri = song?.albumArtUri ?: return@runCatching null
                        val bytes =
                            if (uri.startsWith("http")) {
                                val c = java.net.URL(uri).openConnection()
                                c.connectTimeout = 5000
                                c.readTimeout = 6000
                                c.getInputStream().use { it.readBytes() }
                            } else if (uri.startsWith("file:")) File(URI(uri)).readBytes()
                            else File(uri).readBytes()
                        Image.makeFromEncoded(bytes).toComposeImageBitmap()
                    }
                    .getOrNull()
            }
    }
    val c = MaterialTheme.colorScheme
    Box(
        Modifier.size(size)
            .clip(RoundedCornerShape(size * .26f))
            .background(
                Brush.linearGradient(listOf(c.primary.copy(alpha = .3f), c.surfaceVariant))
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null)
            androidx.compose.foundation.Image(
                bitmap!!,
                null,
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        else Icon(Icons.Rounded.MusicNote, null, Modifier.size(size * .42f), c.primary)
    }
}

@Composable
fun Empty(title: String, subtitle: String, action: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(100.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = .12f),
                    RoundedCornerShape(36.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.LibraryMusic,
                null,
                Modifier.size(42.dp),
                MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(t(title), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilledTonalButton(action) { Text(t("Добавить музыку")) }
    }
}

@Composable
fun TrackList(
    songs: List<Song>,
    p: Player,
    liked: Set<String>,
    play: (Song) -> Unit,
    menu: (Song) -> Unit,
    like: (Song) -> Unit,
) {
    val ls = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = ls, contentPadding = PaddingValues(bottom = 24.dp)) {
            itemsIndexed(songs, key = { i, s -> "${s.id}_$i" }) { i, s ->
                TrackRow(
                    s,
                    i,
                    p.song?.id == s.id,
                    p.playing,
                    s.id in liked,
                    { play(s) },
                    { menu(s) },
                    { like(s) },
                )
            }
        }
        VerticalScrollbar(
            rememberScrollbarAdapter(ls),
            Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )
    }
}

@Composable
fun TrackRow(
    s: Song,
    index: Int,
    selected: Boolean,
    playing: Boolean,
    liked: Boolean,
    play: () -> Unit,
    menu: () -> Unit,
    like: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth()
            .padding(top = 3.dp, bottom = 3.dp, end = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) c.primary.copy(alpha = .14f) else Color.Transparent)
            .combinedClickable(onClick = play, onDoubleClick = play)
            .padding(12.dp, 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (selected && playing) "Ⅱ" else "${index+1}",
            Modifier.width(28.dp),
            fontSize = 12.sp,
            color = if (selected) c.primary else c.onSurfaceVariant,
        )
        Art(s, 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                s.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (selected) c.primary else c.onSurface,
            )
            Text(
                s.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                color = c.onSurfaceVariant,
            )
        }
        Text(
            clock(s.durationMs),
            fontSize = 12.sp,
            color = c.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        IconButton(like, Modifier.size(34.dp)) {
            Icon(
                if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                "Нравится",
                Modifier.size(19.dp),
                if (liked) c.primary else c.onSurfaceVariant,
            )
        }
        IconButton(menu, Modifier.size(34.dp)) {
            Icon(Icons.Rounded.MoreVert, "Действия", Modifier.size(21.dp))
        }
    }
}

@Composable
fun Home(
    a: Actions,
    go: (String) -> Unit,
    play: (Song) -> Unit,
    menu: (Song) -> Unit,
    add: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val ls = rememberScrollState()
    Column(
        Modifier.fillMaxSize().verticalScroll(ls).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(
            Modifier.fillMaxWidth()
                .heightIn(min = 200.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(
                    Brush.linearGradient(listOf(c.primary.copy(alpha = .22f), c.surfaceVariant))
                )
        ) {
            androidx.compose.foundation.Canvas(Modifier.align(Alignment.CenterEnd).size(250.dp)) {
                drawCircle(
                    c.primary.copy(alpha = .10f),
                    size.minDimension * .48f,
                    center =
                        androidx.compose.ui.geometry.Offset(size.width * .8f, size.height * .25f),
                )
                drawCircle(
                    c.primary.copy(alpha = .14f),
                    size.minDimension * .35f,
                    center =
                        androidx.compose.ui.geometry.Offset(size.width * .6f, size.height * .72f),
                )
            }
            Column(Modifier.padding(28.dp)) {
                Text(t("Хороший день для музыки"), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    t("Вся ваша коллекция. Новые открытия. Один плеер."),
                    Modifier.padding(top = 8.dp, bottom = 20.dp),
                    color = c.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        {
                            a.state.library.firstOrNull()?.let {
                                a.player.play(it, a.state.library)
                            } ?: add()
                        },
                        shape = CircleShape,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null)
                        Text(t("Моя музыка"))
                    }
                    OutlinedButton(add, shape = CircleShape) { Text(t("Добавить папку")) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Discovery(
                "YouTube Music",
                "Поиск, радио и ваша коллекция",
                Icons.Rounded.PlayCircle,
                Modifier.weight(1f),
            ) {
                go("YouTube Music")
            }
            Discovery(
                "SoundCloud",
                "Независимая музыка и открытия",
                Icons.Rounded.Cloud,
                Modifier.weight(1f),
            ) {
                go("SoundCloud")
            }
        }
        val recent = a.state.history.take(8)
        if (recent.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    t("Недавно слушали"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton({ a.player.play(recent.first(), recent) }) { Text(t("Слушать всё")) }
            }
            recent.forEachIndexed { i, s ->
                TrackRow(
                    s,
                    i,
                    a.player.song?.id == s.id,
                    a.player.playing,
                    s.id in a.state.liked,
                    { play(s) },
                    { menu(s) },
                    { a.favorite(s) },
                )
            }
        } else {
            Text(t("Начните со своей коллекции"), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                t(
                    "Добавьте папку с компьютера или найдите музыку в YouTube Music и SoundCloud. Здесь появятся недавно прослушанные треки."
                ),
                color = c.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun Discovery(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier,
    click: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(25.dp))
            .background(c.surfaceVariant.copy(alpha = .65f))
            .clickable(onClick = click)
            .padding(22.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = c.primary, modifier = Modifier.size(30.dp))
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.ArrowOutward, null, tint = c.onSurfaceVariant)
        }
        Text(title, Modifier.padding(top = 14.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, Modifier.padding(top = 6.dp), fontSize = 12.sp, color = c.onSurfaceVariant)
    }
}

@Composable
fun PlayerBar(a: Actions, panel: String, showPanel: (String) -> Unit, eq: () -> Unit) {
    val p = a.player
    val c = MaterialTheme.colorScheme
    val song = p.song
    Surface(
        color = c.surfaceVariant.copy(alpha = .85f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.fillMaxWidth().height(114.dp),
    ) {
        Row(
            Modifier.fillMaxSize().padding(20.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(.9f), verticalAlignment = Alignment.CenterVertically) {
                Art(song, 60.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song?.title ?: "Музыка начинается здесь",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                    Text(
                        song?.artist ?: "Добавьте трек в очередь",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = c.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
                IconButton({ song?.let { a.favorite(it) } }, enabled = song != null) {
                    Icon(
                        if (song?.id in a.state.liked) Icons.Rounded.Favorite
                        else Icons.Rounded.FavoriteBorder,
                        "Нравится",
                        Modifier.size(20.dp),
                        if (song?.id in a.state.liked) c.primary else c.onSurfaceVariant,
                    )
                }
            }
            Column(
                Modifier.weight(1.35f).padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    IconButton({ p.shuffle = !p.shuffle }) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            "Перемешать",
                            Modifier.size(19.dp),
                            if (p.shuffle) c.primary else c.onSurfaceVariant,
                        )
                    }
                    IconButton({ p.previous() }) { Icon(Icons.Rounded.SkipPrevious, "Предыдущий") }
                    FilledIconButton(
                        { p.toggle() },
                        enabled = song != null,
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(if (p.playing) 16.dp else 23.dp),
                    ) {
                        if (p.loading)
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else
                            Icon(
                                if (p.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                "Воспроизведение",
                                Modifier.size(28.dp),
                            )
                    }
                    IconButton({ p.next() }) { Icon(Icons.Rounded.SkipNext, "Следующий") }
                    IconButton({ p.repeat = RepeatMode.entries[(p.repeat.ordinal + 1) % 3] }) {
                        Icon(
                            if (p.repeat == RepeatMode.ONE) Icons.Rounded.RepeatOne
                            else Icons.Rounded.Repeat,
                            "Повтор",
                            Modifier.size(19.dp),
                            if (p.repeat != RepeatMode.OFF) c.primary else c.onSurfaceVariant,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        clock(p.position),
                        fontSize = 10.sp,
                        color = c.onSurfaceVariant,
                        modifier = Modifier.width(34.dp),
                    )
                    var seeking by remember { mutableStateOf<Float?>(null) }
                    Slider(
                        seeking
                            ?: p.position
                                .toFloat()
                                .coerceIn(0f, p.duration.toFloat().coerceAtLeast(1f)),
                        { seeking = it },
                        onValueChangeFinished = {
                            seeking?.let { p.seek(it.toLong()) }
                            seeking = null
                        },
                        valueRange = 0f..p.duration.toFloat().coerceAtLeast(1f),
                        enabled = song != null,
                        modifier = Modifier.weight(1f).height(26.dp),
                    )
                    Text(
                        clock(p.duration),
                        fontSize = 10.sp,
                        color = c.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Row(
                Modifier.weight(.8f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(eq, Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.GraphicEq, "Эквалайзер", Modifier.size(20.dp))
                }
                IconButton({ showPanel("Текст") }, Modifier.size(34.dp)) {
                    Icon(
                        Icons.Rounded.Lyrics,
                        "Текст песни",
                        Modifier.size(20.dp),
                        if (panel == "Текст") c.primary else c.onSurfaceVariant,
                    )
                }
                IconButton({ showPanel("Очередь") }, Modifier.size(34.dp)) {
                    Icon(
                        Icons.Rounded.QueueMusic,
                        "Очередь",
                        Modifier.size(22.dp),
                        if (panel == "Очередь") c.primary else c.onSurfaceVariant,
                    )
                }
                IconButton({ p.changeVolume(if (p.volume == 0) 75 else 0) }, Modifier.size(32.dp)) {
                    Icon(
                        if (p.volume == 0) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                        "Громкость",
                        Modifier.size(20.dp),
                    )
                }
                Slider(
                    p.volume.toFloat(),
                    { p.changeVolume(it.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.widthIn(max = 95.dp).weight(1f).height(24.dp),
                )
            }
        }
    }
}

@Composable
fun SidePanel(a: Actions, tab: String, close: () -> Unit, menu: (Song) -> Unit) {
    val p = a.player
    val c = MaterialTheme.colorScheme
    var lyrics by remember(p.song?.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var translate by remember { mutableStateOf(false) }
    LaunchedEffect(p.song?.id, tab, translate) {
        if (tab == "Текст") {
            loading = true
            lyrics =
                runCatching { p.song?.let { LyricsRepository.getLyricsForSong(it) } ?: emptyList() }
                    .getOrDefault(emptyList())
            if (translate && lyrics.isNotEmpty())
                lyrics =
                    runCatching { LyricsRepository.translateLyricLines(lyrics, a.state.language) }
                        .getOrDefault(lyrics)
            loading = false
        }
    }
    Column(
        Modifier.width(300.dp)
            .fillMaxHeight()
            .padding(top = 24.dp, end = 12.dp)
            .background(
                c.surfaceVariant.copy(alpha = .35f),
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                t(tab),
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(close) { Icon(Icons.Rounded.Close, "Закрыть") }
        }
        if (tab == "Очередь") {
            TextButton({ p.clearQueue() }) { Text(t("Очистить очередь")) }
            LazyColumn {
                itemsIndexed(p.queue) { i, s ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (s.id == p.song?.id) c.primary.copy(alpha = .12f)
                                    else Color.Transparent
                                )
                                .clickable { p.play(s) }
                                .padding(8.dp),
                    ) {
                        Art(s, 36.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 9.dp)) {
                            Text(
                                s.title,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                s.artist,
                                fontSize = 10.sp,
                                maxLines = 1,
                                color = c.onSurfaceVariant,
                            )
                        }
                        IconButton({ p.removeQueue(i) }, Modifier.size(25.dp)) {
                            Icon(Icons.Rounded.Close, "Убрать", Modifier.size(16.dp))
                        }
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("Перевод"), modifier = Modifier.weight(1f), fontSize = 12.sp)
                Switch(translate, { translate = it })
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            else if (lyrics.isEmpty())
                Text(
                    t("Текст пока не найден. Можно добавить свой текст в редакторе тегов."),
                    color = c.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 30.dp),
                )
            else {
                val list = rememberLazyListState()
                val active = lyrics.indexOfLast { it.timeStampMs <= p.position }
                LaunchedEffect(active) {
                    if (active >= 0 && lyrics.any { it.timeStampMs > 0 })
                        list.animateScrollToItem((active - 2).coerceAtLeast(0))
                }
                LazyColumn(
                    state = list,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(vertical = 32.dp),
                ) {
                    itemsIndexed(lyrics) { i, line ->
                        Text(
                            line.content,
                            fontSize = if (i == active) 23.sp else 20.sp,
                            fontWeight = FontWeight.Bold,
                            color =
                                if (i == active) c.primary
                                else c.onSurfaceVariant.copy(alpha = .7f),
                            modifier =
                                Modifier.clickable {
                                    if (line.timeStampMs > 0) p.seek(line.timeStampMs)
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistPage(
    a: Actions,
    open: (UserList) -> Unit,
    create: () -> Unit,
    delete: (UserList) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column {
        Row {
            Button(create) {
                Icon(Icons.Rounded.Add, null)
                Text(t("Создать плейлист"))
            }
            Spacer(Modifier.width(10.dp))
            TextButton({ a.importPlaylist() }) { Text(t("Импорт M3U")) }
        }
        Spacer(Modifier.height(18.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(a.state.playlists, key = { it.id }) { list ->
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.surfaceVariant.copy(alpha = .65f))
                        .clickable { open(list) }
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Art(list.songs.firstOrNull(), 62.dp)
                    Column(Modifier.padding(horizontal = 18.dp).weight(1f)) {
                        Text(
                            list.name,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        )
                        Text(
                            "${list.songs.size} треков · ${clock(list.songs.sumOf{it.durationMs})}",
                            color = c.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                    IconButton({
                        list.songs.firstOrNull()?.let { a.player.play(it, list.songs) }
                    }) {
                        Icon(Icons.Rounded.PlayArrow, "Слушать")
                    }
                    IconButton({ delete(list) }) { Icon(Icons.Rounded.DeleteOutline, "Удалить") }
                }
            }
        }
    }
}

@Composable
fun DialogAction(label: String, icon: ImageVector, click: () -> Unit) {
    FilledTonalButton(
        click,
        Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(icon, null, Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label)
    }
}

@Composable
fun TextDialog(title: String, value: String, close: () -> Unit, save: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it }, singleLine = true) },
        confirmButton = {
            TextButton({ save(text.trim()) }, enabled = text.isNotBlank()) { Text(t("Сохранить")) }
        },
        dismissButton = { TextButton(close) { Text(t("Отмена")) } },
    )
}
