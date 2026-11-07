package com.lormusic.app.data

import android.content.Context
import com.lormusic.app.model.Song
import java.io.File

object DemoSynthGenerator {

    fun ensureDemoTracksReady(context: Context, demoSongs: List<Song>): List<Song> {
        val dir = File(context.cacheDir, "demo_tracks")
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        return emptyList()
    }
}
