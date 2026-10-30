package com.lormusic.app.model

enum class PlaybackContextType {
    ALBUM,
    MY_WAVE,
    RADIO,
    SINGLE,
    PLAYLIST,
    FAVORITES,
    QUEUE
}

data class PlaybackContext(
    val type: PlaybackContextType,
    val title: String? = null
)
