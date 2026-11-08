package com.lormusic.app.service

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer

/** Temporarily borrows the existing engine and MediaSession, preserving the library queue. Main thread only. */
object ExternalPlaybackSession {
    var active: Boolean = false
        private set
    private data class Snapshot(val items: List<MediaItem>, val index: Int, val position: Long,
        val play: Boolean, val repeat: Int, val shuffle: Boolean, val parameters: PlaybackParameters)
    private var saved: Snapshot? = null
    fun begin(player: ExoPlayer) {
        check(!active)
        saved = Snapshot((0 until player.mediaItemCount).map(player::getMediaItemAt),
            player.currentMediaItemIndex.coerceAtLeast(0), player.currentPosition.coerceAtLeast(0),
            player.playWhenReady, player.repeatMode, player.shuffleModeEnabled, player.playbackParameters)
        active = true
        player.pause()
    }
    fun end(player: ExoPlayer) {
        val old = saved ?: return
        player.pause()
        player.repeatMode = old.repeat
        player.shuffleModeEnabled = old.shuffle
        player.playbackParameters = old.parameters
        // Keep library observers suspended while replacing the temporary media item.
        if (old.items.isEmpty()) player.clearMediaItems()
        else {
            player.setMediaItems(old.items, old.index.coerceAtMost(old.items.lastIndex), old.position)
            player.prepare()
        }
        saved = null
        active = false
        player.playWhenReady = old.play && old.items.isNotEmpty()
    }
}
