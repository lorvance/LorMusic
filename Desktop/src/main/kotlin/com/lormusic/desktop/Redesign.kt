@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lormusic.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.lormusic.app.model.*
import com.lormusic.app.model.RepeatMode

@Composable
fun ThinSlider(
    value: Float,
    change: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    finish: () -> Unit = {},
) {
    val c = MaterialTheme.colorScheme
    Slider(
        value,
        change,
        onValueChangeFinished = finish,
        valueRange = range,
        modifier = modifier,
        thumb = { Box(Modifier.size(12.dp).background(c.primary, CircleShape)) },
        track = { state -> SliderDefaults.Track(state, modifier = Modifier.height(4.dp)) },
    )
}

@Composable
fun SeekControl(a: Actions, modifier: Modifier = Modifier) {
    val p = a.player
    var seek by remember { mutableStateOf<Float?>(null) }
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            clock(p.position),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ThinSlider(
            seek ?: p.position.toFloat().coerceIn(0f, p.duration.toFloat().coerceAtLeast(1f)),
            { seek = it },
            0f..p.duration.toFloat().coerceAtLeast(1f),
            Modifier.weight(1f).height(28.dp),
        ) {
            seek?.let { p.seek(it.toLong()) }
            seek = null
        }
        Text(
            clock(p.duration),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun Transport(a: Actions, large: Boolean = false) {
    val p = a.player
    val c = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (large) 14.dp else 5.dp),
    ) {
        IconButton({ p.shuffle = !p.shuffle }) {
            Icon(
                Icons.Rounded.Shuffle,
                "Перемешать",
                tint = if (p.shuffle) c.primary else c.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton({ p.previous() }) {
            Icon(
                Icons.Rounded.SkipPrevious,
                "Предыдущий",
                Modifier.size(if (large) 30.dp else 24.dp),
            )
        }
        FilledIconButton(
            { p.toggle() },
            enabled = p.song != null,
            modifier = Modifier.size(if (large) 76.dp else 48.dp),
            shape = RoundedCornerShape(if (large) 28.dp else 18.dp),
        ) {
            if (p.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else
                Icon(
                    if (p.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    "Воспроизведение",
                    Modifier.size(if (large) 38.dp else 28.dp),
                )
        }
        IconButton({ p.next() }) {
            Icon(Icons.Rounded.SkipNext, "Следующий", Modifier.size(if (large) 30.dp else 24.dp))
        }
        IconButton({ p.repeat = RepeatMode.entries[(p.repeat.ordinal + 1) % 3] }) {
            Icon(
                if (p.repeat == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                "Повтор",
                tint = if (p.repeat != RepeatMode.OFF) c.primary else c.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun PlayerBar(
    a: Actions,
    panel: String,
    showPanel: (String) -> Unit,
    eq: () -> Unit,
    expand: () -> Unit = {},
) {
    val p = a.player
    val c = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            Modifier.widthIn(max = 1280.dp).fillMaxWidth().height(108.dp),
            shape = RoundedCornerShape(28.dp),
            color = c.surfaceVariant,
            border = BorderStroke(1.dp, c.outline.copy(alpha = .22f)),
            shadowElevation = 12.dp,
        ) {
            BoxWithConstraints(
                Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                val narrow = maxWidth < 1000.dp
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = expand),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Art(p.song, 60.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                p.song?.title ?: t("Выберите музыку"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                p.song?.artist ?: t("Ваша коллекция рядом"),
                                fontSize = 12.sp,
                                color = c.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!narrow)
                            IconButton({ p.song?.let { a.favorite(it) } }) {
                                Icon(
                                    if (p.song?.id in a.state.liked) Icons.Rounded.Favorite
                                    else Icons.Rounded.FavoriteBorder,
                                    "Мне нравится",
                                    Modifier.size(22.dp),
                                )
                            }
                    }
                    Column(
                        Modifier.weight(if (narrow) 1.35f else 1.55f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Transport(a)
                        SeekControl(a, Modifier.fillMaxWidth())
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(expand) {
                            Icon(Icons.Rounded.UnfoldMore, "Развернуть плеер", Modifier.size(22.dp))
                        }
                        if (!narrow) {
                            IconButton(eq) {
                                Icon(Icons.Rounded.GraphicEq, "Эквалайзер", Modifier.size(21.dp))
                            }
                            IconButton({ showPanel("Текст") }) {
                                Icon(Icons.Rounded.Lyrics, "Текст", Modifier.size(21.dp))
                            }
                        }
                        IconButton({ showPanel("Очередь") }) {
                            Icon(Icons.Rounded.QueueMusic, "Очередь", Modifier.size(23.dp))
                        }
                        if (!narrow) {
                            IconButton({ p.changeVolume(if (p.volume == 0) 75 else 0) }) {
                                Icon(
                                    if (p.volume == 0) Icons.Rounded.VolumeOff
                                    else Icons.Rounded.VolumeUp,
                                    "Громкость",
                                    Modifier.size(21.dp),
                                )
                            }
                            ThinSlider(
                                p.volume.toFloat(),
                                { p.changeVolume(it.toInt()) },
                                0f..100f,
                                Modifier.width(86.dp),
                            )
                        }
                    }
                }
            }
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
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 1400.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            BoxWithConstraints(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF263A64), Color(0xFF26334F))))
            ) {
                val small = maxWidth < 780.dp
                if (!small)
                    ExpressiveWaves(
                        Modifier.align(Alignment.CenterEnd).width(340.dp).height(230.dp)
                    )
                Column(Modifier.padding(if (small) 24.dp else 32.dp).widthIn(max = 740.dp)) {
                    Text(
                        t("МУЗЫКА НА КАЖДЫЙ ДЕНЬ"),
                        color = Color(0xFFA5CAFF),
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        t("Всё, что хочется слушать"),
                        Modifier.padding(top = 12.dp),
                        fontSize = if (small) 28.sp else 36.sp,
                        lineHeight = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        t("Ваша коллекция и новые открытия — в одном месте."),
                        Modifier.padding(top = 10.dp, bottom = 22.dp),
                        fontSize = 15.sp,
                        color = Color(0xFFD1DCEF),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            {
                                a.state.library.firstOrNull()?.let {
                                    a.player.play(it, a.state.library)
                                } ?: add()
                            },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 13.dp),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(t("Моя музыка"))
                        }
                        FilledTonalButton(
                            add,
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 13.dp),
                        ) {
                            Icon(Icons.Rounded.CreateNewFolder, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(t("Добавить папку"))
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Discovery(
                    "YouTube Music",
                    "Поиск, рекомендации и моя волна",
                    Icons.Rounded.PlayCircle,
                    Modifier.weight(1f),
                ) {
                    go("YouTube Music")
                }
                Discovery(
                    "SoundCloud",
                    "Независимая музыка и новые имена",
                    Icons.Rounded.Cloud,
                    Modifier.weight(1f),
                ) {
                    go("SoundCloud")
                }
            }
            val recent = a.state.history.take(12)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (recent.isEmpty()) "Ваша медиатека" else "Недавно слушали",
                    Modifier.weight(1f),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (recent.isNotEmpty())
                    TextButton({ a.player.play(recent.first(), recent) }) {
                        Text(t("Слушать всё"))
                        Icon(
                            Icons.Rounded.ArrowForward,
                            null,
                            Modifier.padding(start = 8.dp).size(18.dp),
                        )
                    }
            }
            if (recent.isNotEmpty())
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                }
            else
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = c.surfaceVariant.copy(alpha = .5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(26.dp)) {
                        Text(
                            t("Начните с любимого"),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            t("Добавьте музыку с компьютера или найдите трек в сервисах выше."),
                            Modifier.padding(top = 8.dp),
                            color = c.onSurfaceVariant,
                        )
                        TextButton(add) { Text(t("Добавить музыку")) }
                    }
                }
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
    val brand = if (title == "SoundCloud") Color(0xFFFF9853) else Color(0xFFFF6B89)
    Surface(
        modifier.height(128.dp).clickable(onClick = click),
        shape = RoundedCornerShape(24.dp),
        color = c.surfaceVariant.copy(alpha = .66f),
        border = BorderStroke(1.dp, c.outline.copy(alpha = .12f)),
    ) {
        Row(
            Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(52.dp)
                    .background(brand.copy(alpha = .16f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(30.dp), tint = brand)
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    Modifier.padding(top = 5.dp),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = c.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Rounded.ArrowOutward, null, Modifier.size(22.dp), tint = c.onSurfaceVariant)
        }
    }
}

@Composable
fun FullscreenPlayer(a: Actions, close: () -> Unit, menu: (Song) -> Unit) {
    val p = a.player
    val c = MaterialTheme.colorScheme
    var side by remember { mutableStateOf(System.getProperty("lormusic.playerPanel", "")) }
    var dominant by remember { mutableStateOf(c.primary) }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(c.background).onPreviewKeyEvent {
            if (it.key == Key.Escape && it.type == KeyEventType.KeyDown) {
                close()
                true
            } else false
        }
    ) {
        val small = maxWidth < 1000.dp
        val artSize =
            minOf(
                    if (side.isNotEmpty()) maxWidth * .32f else maxWidth * .38f,
                    (if (maxHeight < 700.dp) maxHeight * .32f else maxHeight * .48f),
                    460.dp,
                )
                .coerceAtLeast(180.dp)
        ArtworkAtmosphere(p.song, a.state.playerGradient) { dominant = it }
        MaterialTheme(
            colorScheme = c.copy(primary = if (a.state.dynamicColors) dominant else c.primary)
        ) {
            Column(
                Modifier.fillMaxSize()
                    .padding(horizontal = if (small) 24.dp else 40.dp, vertical = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(close) {
                        Icon(Icons.Rounded.KeyboardArrowDown, "Свернуть плеер")
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t("Сейчас играет"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            p.song?.album?.takeIf { it.isNotBlank() } ?: t("Ваша музыка"),
                            fontSize = 12.sp,
                            color = c.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    FilterChip(
                        side == "Текст",
                        { side = if (side == "Текст") "" else "Текст" },
                        label = { Text(t("Текст")) },
                        leadingIcon = { Icon(Icons.Rounded.Lyrics, null, Modifier.size(18.dp)) },
                    )
                    Spacer(Modifier.width(10.dp))
                    FilterChip(
                        side == "Очередь",
                        { side = if (side == "Очередь") "" else "Очередь" },
                        label = { Text(t("Очередь")) },
                        leadingIcon = { Icon(Icons.Rounded.QueueMusic, null, Modifier.size(18.dp)) },
                    )
                    IconButton({ p.song?.let(menu) }, enabled = p.song != null) {
                        Icon(Icons.Rounded.MoreVert, "Действия с треком")
                    }
                }
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            shadowElevation = 24.dp,
                            color = c.surfaceVariant,
                        ) {
                            Art(p.song, artSize)
                        }
                        Row(
                            Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(top = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    p.song?.title ?: t("Выберите музыку"),
                                    fontSize = if (small) 26.sp else 32.sp,
                                    lineHeight = 38.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    p.song?.artist ?: t("Ваша коллекция рядом"),
                                    Modifier.padding(top = 8.dp),
                                    fontSize = 18.sp,
                                    color = c.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(
                                { p.song?.let { a.favorite(it) } },
                                enabled = p.song != null,
                            ) {
                                Icon(
                                    if (p.song?.id in a.state.liked) Icons.Rounded.Favorite
                                    else Icons.Rounded.FavoriteBorder,
                                    "Мне нравится",
                                    Modifier.size(26.dp),
                                    tint = c.primary,
                                )
                            }
                        }
                        SeekControl(
                            a,
                            Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(top = 18.dp),
                        )
                        Transport(a, true)
                    }
                    if (side.isNotEmpty() && !small)
                        Surface(
                            Modifier.weight(1f).fillMaxHeight().padding(vertical = 20.dp),
                            shape = RoundedCornerShape(28.dp),
                            color = c.surfaceVariant.copy(alpha = .35f),
                        ) {
                            SidePanel(a, side, { side = "" }, menu, Modifier.fillMaxSize())
                        }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton({ p.changeSpeed(if (p.speed >= 2f) .75f else p.speed + .25f) }) {
                        Text("${p.speed}×")
                    }
                    Spacer(Modifier.width(24.dp))
                    Icon(
                        Icons.Rounded.VolumeUp,
                        "Громкость",
                        Modifier.size(20.dp),
                        tint = c.onSurfaceVariant,
                    )
                    ThinSlider(
                        p.volume.toFloat(),
                        { p.changeVolume(it.toInt()) },
                        0f..100f,
                        Modifier.width(140.dp),
                    )
                }
            }
            if (side.isNotEmpty() && small)
                Surface(
                    Modifier.align(Alignment.CenterEnd)
                        .width(360.dp)
                        .fillMaxHeight()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 16.dp,
                ) {
                    SidePanel(a, side, { side = "" }, menu, Modifier.fillMaxSize())
                }
        }
    }
}
