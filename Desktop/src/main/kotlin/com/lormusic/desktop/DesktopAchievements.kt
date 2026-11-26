package com.lormusic.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal enum class DesktopAward(val label: String, val goal: Int) {
    FIRST("Первый звук", 1),
    TRACKS_100("Меломан", 100),
    TRACKS_1000("Музыкальный архивариус", 1000),
    ARTISTS("Музыкальный путешественник", 50),
    LIBRARY("Музыкальный склад", 1000),
    LIKES_10("Коллекционер", 10),
    LIKES_100("Архивариус", 100),
    PLAYLIST("Мой первый плейлист", 1),
    PLAYLISTS("Куратор", 5);

    fun progress(state: SavedState) = when (this) {
        FIRST, TRACKS_100, TRACKS_1000 -> state.heardTracks.size
        ARTISTS -> state.heardArtists.size
        LIBRARY -> state.library.size
        LIKES_10, LIKES_100 -> state.liked.size
        PLAYLIST, PLAYLISTS -> state.playlists.size
    }
}

@Composable
internal fun DesktopAchievements(actions: Actions) {
    val state = actions.state
    val labels = DesktopAward.entries.associateWith { t(it.label) }
    val prefix = t("Достижение открыто")
    val revision = actions.revision
    val track = actions.player.song.takeIf { actions.player.playing }
    LaunchedEffect(revision, track?.id, state.achievementsEnabled) {
        if (!state.achievementsEnabled) return@LaunchedEffect
        var modified = false
        if (track != null) {
            modified = state.heardTracks.add(track.id)
            val artist = track.artist.trim().lowercase()
            if (artist.isNotBlank() && state.heardArtists.add(artist)) modified = true
        }
        val unlocked = DesktopAward.entries.filter { it.progress(state) >= it.goal && state.earnedAwards.add(it.name) }
        unlocked.forEach { actions.notify("🏆 $prefix · ${labels.getValue(it)}") }
        if (modified || unlocked.isNotEmpty()) actions.changed()
    }
}

@Composable
internal fun DesktopAchievementSettings(actions: Actions) {
    val state = actions.state
    val revision = actions.revision
    SettingsGroup("Достижения") {
        PreferenceSwitch("Собирать награды за активность в приложении", state.achievementsEnabled) {
            state.achievementsEnabled = it
            actions.changed()
        }
        if (state.achievementsEnabled) {
            Text("${t("Получено")} ${state.earnedAwards.size} / ${DesktopAward.entries.size}")
            DesktopAward.entries.forEach { award ->
                ListItem(
                    headlineContent = { Text(t(award.label)) },
                    leadingContent = { Icon(Icons.Rounded.EmojiEvents, null) },
                    trailingContent = { Text("${award.progress(state).coerceAtMost(award.goal)} / ${award.goal}") },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }
        }
    }
}
