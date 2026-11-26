package com.lormusic.app

import android.os.Bundle
import android.provider.OpenableColumns
import android.view.TextureView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import com.lormusic.app.data.PreferencesRepository
import com.lormusic.app.model.ThemeMode
import com.lormusic.app.ui.components.WavyPlayPauseButton
import com.lormusic.app.ui.theme.LorMusicTheme
import kotlinx.coroutines.delay

class ExternalVideoActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private var resumePlayback = true
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val uri = intent.data
        if (uri == null || uri.scheme !in setOf("content","file")) { finish(); return }
        val title = runCatching {
            contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment.orEmpty()
        resumePlayback = state?.getBoolean("playing",true) ?: true
        var playing by mutableStateOf(false)
        var aspect by mutableFloatStateOf(16f / 9)
        var error by mutableStateOf(false)
        val video = ExoPlayer.Builder(this).build()
        player = video
        video.setAudioAttributes(AudioAttributes.DEFAULT, true)
        video.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0) aspect = size.width * size.pixelWidthHeightRatio / size.height
            }
            override fun onPlayerError(e: PlaybackException) { error = true }
        })
        video.setMediaItem(MediaItem.fromUri(uri),state?.getLong("position") ?: 0L)
        video.prepare()
        setContent {
            val prefs = remember { PreferencesRepository(this) }
            val dark = when (prefs.getThemeMode()) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            LorMusicTheme(darkTheme = dark, dynamicColor = prefs.getUseDynamicColor()) {
                BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.55f)).safeDrawingPadding().padding(16.dp), contentAlignment=Alignment.Center) {
                    val heightLimit = maxHeight * .60f
                    Surface(Modifier.widthIn(max=640.dp), shape=RoundedCornerShape(28.dp),color=MaterialTheme.colorScheme.surfaceContainer) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Text(title,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.titleMedium)
                                IconButton({ finish() }) { Icon(Icons.Filled.Close,"Закрыть") }
                            }
                            if (error) Text("Не удалось воспроизвести видео") else {
                                AndroidView(factory={ ctx -> TextureView(ctx).also(video::setVideoTextureView) },
                                    modifier=Modifier.heightIn(max=heightLimit).aspectRatio(aspect).background(Color.Black))
                                var position by remember { mutableLongStateOf(0L) }
                                var duration by remember { mutableLongStateOf(0L) }
                                var drag by remember { mutableStateOf<Float?>(null) }
                                LaunchedEffect(video) { while (true) { position=video.currentPosition; duration=video.duration.coerceAtLeast(0); delay(250) } }
                                Slider(value=drag ?: (position.toFloat()/duration.coerceAtLeast(1)).coerceIn(0f,1f),
                                    onValueChange={ drag=it }, onValueChangeFinished={ drag?.let { video.seekTo((it*duration).toLong()) }; drag=null }, enabled=duration>0)
                                WavyPlayPauseButton(isPlaying=playing,onClick={
                                    if (video.playWhenReady && video.playbackState != Player.STATE_ENDED) video.pause()
                                    else { if (video.playbackState == Player.STATE_ENDED) video.seekTo(0); video.play() }
                                })
                            }
                        }
                    }
                }
            }
        }
    }
    override fun onStart() { super.onStart(); if (resumePlayback) player?.play() }
    override fun onStop() { player?.let { resumePlayback=it.playWhenReady; it.pause() }; super.onStop() }
    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        player?.let { out.putLong("position",it.currentPosition); out.putBoolean("playing",if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) it.playWhenReady else resumePlayback) }
    }
    override fun onDestroy() { player?.release(); super.onDestroy() }
}
