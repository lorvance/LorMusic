package com.lormusic.app

import android.app.Application
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.lormusic.app.data.PreferencesRepository
import com.lormusic.app.model.ThemeMode
import com.lormusic.app.service.ExternalPlaybackSession
import com.lormusic.app.service.PlaybackService
import com.lormusic.app.service.SharedPlayerHolder
import com.lormusic.app.ui.components.WavyPlayPauseButton
import com.lormusic.app.ui.theme.LorMusicTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExternalAudioActivity : ComponentActivity() {
    private val model: ExternalAudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null || !model.hasInput) model.open(intent)
        setContent {
            val prefs = remember { PreferencesRepository(this) }
            val dark = when (prefs.getThemeMode()) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            LorMusicTheme(darkTheme = dark, dynamicColor = prefs.getUseDynamicColor()) {
                ExternalAudioCard(model) { model.close(); finish() }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        model.open(intent)
    }
}

class ExternalAudioViewModel(application: Application) : AndroidViewModel(application) {
    private val player = SharedPlayerHolder.getOrCreate(application)
    var title by mutableStateOf("Аудиофайл"); private set
    var artist by mutableStateOf("Внешний аудиофайл"); private set
    var error by mutableStateOf<String?>(null); private set
    var position by mutableLongStateOf(0L); private set
    var duration by mutableLongStateOf(0L); private set
    var playing by mutableStateOf(false); private set
    var loading by mutableStateOf(false); private set
    var artwork by mutableStateOf<android.graphics.Bitmap?>(null); private set
    var hasInput = false; private set
    private var ownsSession = false
    private var loadJob: Job? = null
    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying && ExternalPlaybackSession.active }
        override fun onPlaybackStateChanged(state: Int) {
            if (!ownsSession || !ExternalPlaybackSession.active) return
            loading = state == Player.STATE_BUFFERING
            duration = player.duration.coerceAtLeast(0)
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (ownsSession && !ExternalPlaybackSession.active) {
                ownsSession = false
                playing = false
                error = "Воспроизведение продолжено в основном плеере"
            }
        }
        override fun onMediaMetadataChanged(metadata: MediaMetadata) {
            if (!ownsSession || !ExternalPlaybackSession.active) return
            metadata.title?.toString()?.takeIf { it.isNotBlank() }?.let { title = it }
            metadata.artist?.toString()?.takeIf { it.isNotBlank() }?.let { artist = it }
        }
        override fun onPlayerError(exception: PlaybackException) {
            if (!ownsSession || !ExternalPlaybackSession.active) return
            loading = false
            error = "Не удалось воспроизвести файл. Проверьте доступ и формат аудио."
        }
    }
    init {
        player.addListener(listener)
        viewModelScope.launch {
            while (true) {
                if (ownsSession && !ExternalPlaybackSession.active) {
                    ownsSession = false
                    playing = false
                    error = "Воспроизведение продолжено в основном плеере"
                }
                if (ownsSession && ExternalPlaybackSession.active) {
                    position = player.currentPosition.coerceAtLeast(0)
                    duration = player.duration.coerceAtLeast(0)
                    playing = player.isPlaying
                }
                delay(200)
            }
        }
    }
    @Suppress("DEPRECATION")
    fun open(intent: Intent) {
        hasInput = true
        loadJob?.cancel()
        val uri = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> (intent.getParcelableExtra<android.os.Parcelable>(Intent.EXTRA_STREAM) as? Uri)
                ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            else -> null
        }
        if (ownsSession && ExternalPlaybackSession.active) player.pause()
        if (!ExternalPlaybackSession.active) ownsSession = false
        error = null; loading = true; position = 0; duration = 0; artwork = null
        title = "Аудиофайл"; artist = "Внешний аудиофайл"
        if (uri == null || uri.scheme !in listOf("content", "file")) {
            error = "Приложение не передало доступный аудиофайл"; loading = false; return
        }
        loadJob = viewModelScope.launch {
            val info = runCatching {
                withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    resolver.openAssetFileDescriptor(uri, "r")?.use { } ?: error("Нет доступа к файлу")
                    var name = uri.lastPathSegment ?: "Аудиофайл"
                    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() }?.let { name = it }
                    }
                    var performer = "Внешний аудиофайл"
                    var cover: android.graphics.Bitmap? = null
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(getApplication(), uri)
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }?.let { name = it }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }?.let { performer = it }
                        retriever.embeddedPicture?.let { bytes ->
                            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                            val options = android.graphics.BitmapFactory.Options().apply {
                                inSampleSize = (maxOf(bounds.outWidth, bounds.outHeight) / 512).coerceAtLeast(1)
                            }
                            cover = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        }
                    } catch (_: Exception) {
                        // ExoPlayer may still support formats the platform retriever cannot read.
                    } finally { retriever.release() }
                    Triple(name, performer, cover)
                }
            }
            info.onSuccess { (name, performer, cover) ->
                title = name; artist = performer; artwork = cover
                if (!ownsSession) { ExternalPlaybackSession.begin(player); ownsSession = true }
                val item = MediaItem.Builder().setUri(uri).setMediaId("external:$uri")
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(name).setArtist(performer).build()).build()
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.shuffleModeEnabled = false
                player.setPlaybackSpeed(1f)
                player.setMediaItem(item)
                player.prepare()
                runCatching { getApplication<Application>().startService(Intent(getApplication(), PlaybackService::class.java)) }
                player.play()
            }.onFailure {
                if (it is kotlinx.coroutines.CancellationException) throw it
                loading = false; error = "Файл недоступен или отсутствует разрешение на чтение"
            }
        }
    }
    fun toggle() { if (!ownsSession || !ExternalPlaybackSession.active || error != null) return; if (player.isPlaying) player.pause() else player.play() }
    fun seek(value: Long) { if (ownsSession && ExternalPlaybackSession.active && player.isCurrentMediaItemSeekable && duration > 0) player.seekTo(value.coerceIn(0, duration)) }
    fun close() {
        loadJob?.cancel()
        if (ownsSession) { ownsSession = false; ExternalPlaybackSession.end(player) }
    }
    override fun onCleared() { close(); player.removeListener(listener); super.onCleared() }
}

@Composable
private fun ExternalAudioCard(model: ExternalAudioViewModel, onClosed: () -> Unit) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    fun close() { visible.targetState = false }
    BackHandler { close() }
    LaunchedEffect(visible.isIdle, visible.currentState) {
        if (visible.isIdle && !visible.currentState && !visible.targetState) onClosed()
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.24f))) {
        Box(Modifier.matchParentSize().clickable { close() })
        AnimatedVisibility(visibleState = visible, modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.navigationBarsPadding().padding(12.dp).fillMaxWidth()) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val cover = model.artwork
                        if (cover != null) Image(cover.asImageBitmap(), null, Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                        else Icon(Icons.Filled.MusicNote, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(model.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                            Text(model.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { close() }) { Icon(Icons.Filled.Close, "Закрыть проигрыватель") }
                    }
                    model.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (model.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    var drag by remember { mutableStateOf<Float?>(null) }
                    Slider(value = drag ?: if (model.duration > 0) model.position.toFloat() / model.duration else 0f,
                        onValueChange = { drag = it }, onValueChangeFinished = {
                            drag?.let { model.seek((it * model.duration).toLong()) }; drag = null
                        }, enabled = model.duration > 0 && model.error == null)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(audioTime(model.position), style = MaterialTheme.typography.labelMedium)
                        Text(if (model.duration > 0) audioTime(model.duration) else "—:—", style = MaterialTheme.typography.labelMedium)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(onClick = { model.seek(model.position - 30_000) }) { Icon(Icons.Filled.Replay30, "Назад на 30 секунд") }
                        WavyPlayPauseButton(isPlaying = model.playing, onClick = model::toggle)
                        IconButton(onClick = { model.seek(model.position + 30_000) }) { Icon(Icons.Filled.Forward30, "Вперёд на 30 секунд") }
                    }
                }
            }
        }
    }
}
private fun audioTime(ms: Long): String = "%d:%02d".format(ms / 60000, ms / 1000 % 60)
