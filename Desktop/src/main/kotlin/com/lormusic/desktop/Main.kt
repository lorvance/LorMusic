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
        Color(0xFFB65D70),
        Color(0xFF6469AD),
        Color(0xFFC48250),
    )

fun clock(ms: Long): String {
    val s = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

fun browse(url: String) {
    runCatching { Desktop.getDesktop().browse(URI(url)) }
}

fun main(args: Array<String>) {
    if(args.contains("--soundcloud-test")){SoundCloudChecks.run();return}
    if(args.contains("--default-browser-test")){val choice=DefaultBrowser.detect();println("Default browser: ${choice?.name ?: "unavailable"}; automatic bridge=${choice?.chromium==true || choice?.name?.contains("firefox")==true}");return}
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
                onPrimary = Color(0xFF092346),
                secondary = primary,
                background = Color(0xFF10131B),
                surface = Color(0xFF10131B),
                surfaceVariant = Color(0xFF202736),
                onSurface = Color(0xFFECE6F1),
                onSurfaceVariant = Color(0xFFAEB9CC),
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
            var fullPlayer by remember {
                mutableStateOf(System.getProperty("lormusic.fullPlayer") == "true")
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
                            Modifier.width(if (compactPanel) 232.dp else 244.dp)
                                .fillMaxHeight()
                                .padding(14.dp, 18.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            scheme.surfaceVariant.copy(alpha = .55f),
                                            scheme.primary.copy(alpha = .16f),
                                        )
                                    ),
                                    RoundedCornerShape(34.dp),
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                Modifier.padding(6.dp, 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BrandIcon(if(compactPanel)36.dp else 48.dp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        t("LorMusic"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if(compactPanel)17.sp else 19.sp,
                                        maxLines = 1,
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
                                DESKTOP_VERSION,
                                Modifier.padding(16.dp, 8.dp),
                                fontSize = 10.sp,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        Column(
                            Modifier.weight(1f)
                                .fillMaxHeight()
                                .padding(
                                    start = if (compactPanel) 12.dp else 30.dp,
                                    end = if (compactPanel) 18.dp else 36.dp,
                                    top = 28.dp,
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.widthIn(max = 1400.dp).fillMaxWidth(),
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
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        playlist?.name ?: t(page),
                                        fontSize = 36.sp,
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
                                        placeholder = {
                                            Text(
                                                t("Поиск музыки"),
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
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
                                        DropdownMenuItem({ Text(t("Видео в аудио")) }, {
                                            topMenu = false
                                            dialog = "Видео в аудио"
                                        })
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
                        { fullPlayer = true },
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
                if (fullPlayer) FullscreenPlayer(a, { fullPlayer = false }, { showSong(it) })
                UpdateNotice(a)
                SnackbarHost(
                    snackbar,
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 126.dp),
                )
            }
            DesktopAchievements(a)
            if (dialog == "Видео в аудио") VideoTools(a) { dialog = "" }
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
    val bg by animateColorAsState(if (selected) c.primary else Color.Transparent)
    Row(
        Modifier.fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(14.dp, 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, label, Modifier.size(21.dp), if (selected) c.onPrimary else c.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            t(label),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) c.onPrimary else c.onSurfaceVariant,
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
        else {
            ExpressiveWaves(Modifier.fillMaxSize())
            Icon(Icons.Rounded.MusicNote, null, Modifier.size(size * .5f), Color(0xFFF6F7FF))
        }
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
            fontSize = 13.sp,
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
                fontSize = 16.sp,
                color = if (selected) c.primary else c.onSurface,
            )
            Text(
                s.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                color = c.onSurfaceVariant,
            )
        }
        Text(
            clock(s.durationMs),
            fontSize = 13.sp,
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
fun SidePanel(
    a: Actions,
    tab: String,
    close: () -> Unit,
    menu: (Song) -> Unit,
    modifier: Modifier = Modifier.width(300.dp),
) {
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
        modifier
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
                        IconButton(
                            { p.moveQueue(i, -1) },
                            enabled = i > 0,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                Icons.Rounded.KeyboardArrowUp,
                                "Выше в очереди",
                                Modifier.size(18.dp),
                            )
                        }
                        IconButton(
                            { p.moveQueue(i, 1) },
                            enabled = i < p.queue.lastIndex,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                Icons.Rounded.KeyboardArrowDown,
                                "Ниже в очереди",
                                Modifier.size(18.dp),
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
