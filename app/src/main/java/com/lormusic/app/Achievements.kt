package com.lormusic.app

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lormusic.app.ui.components.PhysicsSwitch
import androidx.compose.material3.*
import com.lormusic.app.model.Song
import kotlinx.coroutines.channels.Channel
import java.util.Calendar

internal class Achievements private constructor(private val app: Context) {
    companion object {
        @Volatile private var instance: Achievements? = null
        fun of(context: Context): Achievements = instance ?: synchronized(this) {
            instance ?: Achievements(context.applicationContext).also { instance = it }
        }
    }
    val names = listOf("Первый звук","Меломан","Музыкальный архивариус","На повторе","Любимый исполнитель","Машина времени","Музыкальный путешественник","Без границ","Коллекционер","Архивариус","Музыкальный склад","Мой первый плейлист","Куратор","Идеальный порядок","Независимость","Красная сторона","Оранжевая сторона","Двойной агент","Исследователь","Облачный коллекционер","Стекольщик","Стекольный завод","Минималист","Дизайнер","Хамелеон","Олдфаг","Сам себе дизайнер","Двери гнутся","Холодный линукс","Вайбкодер","Кириешки","I don't know what LorMusic is","А где музыка?","Передумал","Кто выключил свет?","Стекло разбилось","Сочный пингвин","Подозрительный пользователь","Зачем тебе вайбкод","Бета-тестер","Охотник за багами","Ветеран LorMusic","У истоков","Создатель")
    val hints = listOf("Прослушать первый трек","Прослушать 100 разных треков","Прослушать 1000 разных треков","Прослушать один трек 5 раз","Послушать 20 разных треков одного исполнителя","Вернуться к треку спустя месяц","Послушать 50 разных исполнителей","Послушать музыку из всех источников","Добавить в избранное 10 треков","Добавить в избранное 100 треков","Собрать 1000 треков в медиатеке","Создать плейлист","Создать 5 плейлистов","Изменить метаданные трека","Послушать локальный трек без интернета","Войти в YouTube Music","Войти в SoundCloud","Войти в оба сервиса","Воспроизвести результат поиска","Собрать треки обоих сервисов в одном плейлисте","Включить Liquid Glass","Выставить мощность стекла на 100%","Отключить декоративные эффекты","Изменить акцентный цвет","Сменить иконку 5 раз","Выбрать классическую иконку","Изменить порядок кнопок навигации")
    private val prefs = app.getSharedPreferences("achievements", Context.MODE_PRIVATE)
    var enabled by mutableStateOf(prefs.getBoolean("enabled",false))
        private set
    var unlocked by mutableStateOf(prefs.getStringSet("awards",emptySet()).orEmpty().map { it.toInt() }.toSet())
        private set
    val notifications = Channel<String>(Channel.UNLIMITED)
    private var searchSelection: String? = null
    fun selectSearchResult(song: Song) {
        searchSelection = if (enabled) song.id else null
    }
    fun enable(value: Boolean) {
        enabled = value
        prefs.edit().putBoolean("enabled",value).apply()
        searchSelection = null
        if (value) installation()
    }
    fun installation() {
        if (!enabled) return
        val installed = app.packageManager.getPackageInfo(app.packageName,0).firstInstallTime
        val yearAgo = Calendar.getInstance().apply { add(Calendar.YEAR,-1) }.timeInMillis
        if (installed in 1 until yearAgo) award(42)
        // First public 1.8.0 beta was published on November 11, 2026.
        if (installed in 1 until 1794355200000L) award(43)
    }
    fun award(id: Int) {
        if (!enabled || id in unlocked) return
        unlocked = unlocked + id
        prefs.edit().putStringSet("awards",unlocked.map { it.toString() }.toSet()).apply()
        notifications.trySend("Достижение открыто · " + names[id-1])
    }
    private fun add(key: String, item: String): Int {
        val current = prefs.getStringSet(key,emptySet()).orEmpty()
        if (item in current) return current.size
        val next = current + item
        prefs.edit().putStringSet(key,next).apply()
        return next.size
    }
    fun count(key: String, threshold: Int, id: Int) {
        if (!enabled || id in unlocked) return
        val n = prefs.getInt(key,0) + 1
        prefs.edit().putInt(key,n).apply()
        if (n >= threshold) award(id)
    }
    fun favorite(id: String) {
        if (!enabled) return
        val n = add("favorites",id)
        if (n >= 10) award(9)
        if (n >= 100) award(10)
    }
    fun played(song: Song, playlist: String?) {
        if (!enabled) return
        award(1)
        if (searchSelection == song.id) award(19)
        searchSelection = null
        val last = prefs.getLong("last/"+song.id,0)
        if (last > 0 && last < Calendar.getInstance().apply { add(Calendar.MONTH,-1) }.timeInMillis) award(6)
        prefs.edit().putLong("last/"+song.id,System.currentTimeMillis()).apply()
        count("plays/"+song.id,5,4)
        val n = add("tracks",song.id)
        if (n >= 100) award(2)
        if (n >= 1000) award(3)
        val artist = song.artist.trim().lowercase()
        if (artist.isNotBlank()) {
            if (add("artist/"+artist,song.id) >= 20) award(5)
            if (add("artists",artist) >= 50) award(7)
        }
        val source = when { song.isRemoteYt -> "yt"; song.isRemoteSc -> "sc"; song.isLocal -> "local"; else -> null }
        if (source != null && add("sources",source) == 3) award(8)
        if (song.isLocal) {
            val network = app.getSystemService(ConnectivityManager::class.java)
            val online = network?.getNetworkCapabilities(network.activeNetwork)
                ?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            if (!online) award(15)
        }
        if (song.title.trim().equals("Линукс",true)) award(29)
        if (playlist?.trim().equals("для вайбкода",true)) award(30)
    }
    fun progress(id: Int): Pair<Int,Int>? {
        fun size(key: String) = prefs.getStringSet(key, emptySet()).orEmpty().size
        return when (id) {
            2 -> size("tracks") to 100
            3 -> size("tracks") to 1000
            5 -> (prefs.all.filterKeys { it.startsWith("artist/") }.values
                .maxOfOrNull { (it as? Set<*>)?.size ?: 0 } ?: 0) to 20
            7 -> size("artists") to 50
            9 -> size("favorites") to 10
            10 -> size("favorites") to 100
            13 -> prefs.getInt("playlists", 0) to 5
            25 -> prefs.getInt("icons", 0) to 5
            else -> null
        }
    }
}

@Composable
internal fun AchievementSettings(store: Achievements) {
    val shapes = RoundedCornerShape(20.dp)
    val colors = MaterialTheme.colorScheme
    val visibleIds = (1..44).filterNot { it in setOf(40, 41, 44) }
    val sections = listOf(
        Triple("Прослушивание", "Прослушивание музыки", 1..8),
        Triple("Медиатека", "Медиатека", 9..15),
        Triple("Сервисы", "Музыкальные сервисы", 16..20),
        Triple("Кастомизация", "Кастомизация", 21..27),
        Triple("Секретные", "Секретные достижения", 28..39),
        Triple("Эксклюзивные", "Эксклюзивные достижения", 42..43),
    )
    var expanded by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = shapes, color = colors.surfaceContainerLow) {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text("Достижения") },
                supportingContent = { Text("Собирать награды за активность в приложении") },
                leadingContent = { AwardBadge(Icons.Filled.EmojiEvents, true) },
                trailingContent = { PhysicsSwitch(store.enabled, store::enable) }
            )
        }
        if (store.enabled) {
            val earned = visibleIds.count { it in store.unlocked }
            val ratio = earned.toFloat() / visibleIds.size
            Surface(shape = shapes, color = colors.surfaceContainerLow) {
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$earned", style = MaterialTheme.typography.titleLarge); Text("Получено") }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${visibleIds.size}", style = MaterialTheme.typography.titleLarge); Text("Всего") }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(progress = { ratio }, modifier = Modifier.size(23.dp), strokeWidth = 3.dp)
                            Text("${(ratio * 100).toInt()}%", style = MaterialTheme.typography.titleLarge)
                        }
                        Text("Прогресс")
                    }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                sections.forEachIndexed { i, section ->
                    FilterChip(selected = expanded == i, onClick = { expanded = i }, label = { Text(section.first) })
                }
            }
            val icons = listOf(Icons.Filled.MusicNote, Icons.Filled.Layers, Icons.Filled.Cloud, Icons.Filled.Palette, Icons.Filled.Flag, Icons.Filled.EmojiEvents)
            sections.forEachIndexed { index, section ->
                Surface(onClick = { expanded = if (expanded == index) -1 else index }, shape = shapes, color = colors.surfaceContainerLow) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(section.second) },
                        leadingContent = { Icon(icons[index], null) },
                        trailingContent = { Icon(if (expanded == index) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null) }
                    )
                }
                if (expanded == index) section.third.forEach { id ->
                    val done = id in store.unlocked
                    val hidden = !done && id in 28..39
                    val progress = if (done || hidden) null else store.progress(id)
                    Surface(shape = shapes, color = if (done) colors.primaryContainer.copy(alpha = .34f) else colors.surfaceContainerLow,
                        border = if (done) BorderStroke(1.dp, colors.primary.copy(alpha = .65f)) else null) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(if (hidden) "Секретное достижение" else store.names[id - 1]) },
                            supportingContent = { Text(store.hints.getOrNull(id - 1) ?: when (id) {
                                42 -> "Пользоваться приложением больше года"
                                43 -> "Пользоваться LorMusic до версии 1.8.0"
                                else -> if (done) "Открыто" else "Условие скрыто"
                            }) },
                            leadingContent = { AwardBadge(if (hidden) Icons.Filled.Lock else icons[index], done) },
                            trailingContent = {
                                if (done) Icon(Icons.Filled.CheckCircle, "Получено", tint = colors.primary)
                                else if (progress != null) {
                                    val (current, target) = progress
                                    Column(Modifier.width(72.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text("$current / $target", style = MaterialTheme.typography.labelSmall)
                                        LinearProgressIndicator(progress = { (current.toFloat()/target).coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth())
                                    }
                                } else Icon(Icons.Filled.Lock, "Закрыто", tint = colors.onSurfaceVariant)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AwardBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, highlighted: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(14.dp), color = if (highlighted) colors.primaryContainer else colors.surfaceContainerHighest) {
        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (highlighted) colors.onPrimaryContainer else colors.onSurfaceVariant)
        }
    }
}
