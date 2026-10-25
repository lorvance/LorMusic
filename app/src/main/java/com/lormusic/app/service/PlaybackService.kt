package com.lormusic.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
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
import com.lormusic.app.data.YouTubeMusicRepository

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "lormusic_playback"
    }

    override fun onCreate() {
        super.onCreate()
        val player = SharedPlayerHolder.getOrCreate(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "LorMusic Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "LorMusic background playback and controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .build()
        )

        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LorMusic:PlaybackWakeLock")

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    if (wakeLock?.isHeld == false) {
                        try {
                            wakeLock?.acquire(24 * 60 * 60 * 1000L)
                        } catch (_: Exception) {}
                    }
                } else {
                    if (wakeLock?.isHeld == true) {
                        try {
                            wakeLock?.release()
                        } catch (_: Exception) {}
                    }
                }
            }
        })
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
        if (wakeLock?.isHeld == true) {
            try {
                wakeLock?.release()
            } catch (_: Exception) {}
        }
        mediaSession?.release()
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
