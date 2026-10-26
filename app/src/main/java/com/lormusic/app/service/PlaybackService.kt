package com.lormusic.app.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.lormusic.app.MainActivity
import com.lormusic.app.R
import com.lormusic.app.data.YouTubeMusicRepository

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val player = SharedPlayerHolder.getOrCreate(this)
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val session = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
        mediaSession = session
        addSession(session)

        val notificationProvider = DefaultMediaNotificationProvider(this)
        notificationProvider.setSmallIcon(R.drawable.ic_launcher_regular_mono)
        setMediaNotificationProvider(notificationProvider)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.let {
            removeSession(it)
            it.release()
        }
        mediaSession = null
        super.onDestroy()
    }
}

object SharedPlayerHolder {
    @Volatile
    private var exoPlayer: ExoPlayer? = null

    @OptIn(UnstableApi::class)
    fun getOrCreate(context: Context): ExoPlayer {
        return exoPlayer ?: synchronized(this) {
            exoPlayer ?: run {
                val appContext = context.applicationContext
                val httpFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent(YouTubeMusicRepository.ANDROID_USER_AGENT)
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(10_000)
                    .setReadTimeoutMs(15_000)

                val baseDataSourceFactory = DefaultDataSource.Factory(appContext, httpFactory)
                val resolvingDataSourceFactory = ResolvingDataSource.Factory(baseDataSourceFactory) { dataSpec ->
                    val resolvedUri = YouTubeMusicRepository.resolveStreamUriForExoPlayer(dataSpec.uri)
                    if (resolvedUri != dataSpec.uri) {
                        dataSpec.withUri(resolvedUri)
                    } else {
                        dataSpec
                    }
                }

                ExoPlayer.Builder(appContext)
                    .setMediaSourceFactory(DefaultMediaSourceFactory(resolvingDataSourceFactory))
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .build(),
                         true
                    )
                    .setHandleAudioBecomingNoisy(true)
                    .setWakeMode(C.WAKE_MODE_NETWORK)
                    .build()
                    .also { exoPlayer = it }
            }
        }
    }
}