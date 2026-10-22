package com.lormusic.app.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val coverColorSeed: Int,
    val albumArtUri: String? = null,
    val isLocal: Boolean = false,
    val filePath: String? = null,
    val folderName: String = "Музыка",
    val dateAddedSec: Long = 0L,
    val genre: String = "",
    val year: String = "",
    val trackNumber: String = "",
    val customLyrics: String? = null,
    val isRemoteYt: Boolean = false
) {
    fun withOverride(ov: SongTagOverride?): Song {
        if (ov == null) return this
        return copy(
            title = ov.title.ifBlank { title },
            artist = ov.artist.ifBlank { artist },
            album = ov.album.ifBlank { album },
            genre = ov.genre.ifBlank { genre },
            year = ov.year.ifBlank { year },
            trackNumber = ov.trackNumber.ifBlank { trackNumber },
            albumArtUri = ov.albumArtUri ?: albumArtUri,
            customLyrics = ov.customLyrics ?: customLyrics
        )
    }
}

data class SongTagOverride(
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String = "",
    val year: String = "",
    val trackNumber: String = "",
    val albumArtUri: String? = null,
    val customLyrics: String? = null
)

data class TagFixProposal(
    val originalSong: Song,
    val proposedTitle: String,
    val proposedArtist: String,
    val proposedAlbum: String,
    val proposedYear: String = "",
    val proposedGenre: String = "",
    val proposedCoverUri: String? = null,
    val isAudioRecognized: Boolean = false,
    val reason: String = "",
    val isSelected: Boolean = true
)

data class LyricLine(
    val timeStampMs: Long = 0L,
    val content: String = "",
    val isTranslation: Boolean = false,
    val isInstrumental: Boolean = false
)

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song>,
    val coverColorSeed: Int = name.hashCode(),
    val isSystem: Boolean = false
)

data class GoogleAccountProfile(
    val displayName: String,
    val email: String,
    val avatarUrl: String? = null
)

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class ThemeMode(val label: String) {
    SYSTEM("Системная"),
    DARK("Тёмная"),
    LIGHT("Светлая")
}

enum class AppIconOption(val label: String, val subtitle: String, val aliasClassName: String) {
    MINIMAL("Минимализм", "Лавандовый цветок с нотой", "com.lormusic.app.MainActivityAliasMinimal"),
    REGULAR("Стандартная", "Объёмный цветок с эквалайзером", "com.lormusic.app.MainActivityAliasRegular"),
    OLD("Классическая", "Классическая старая иконка", "com.lormusic.app.MainActivityAliasOld")
}

enum class LibraryFilter(val label: String) {
    ALL("Все"),
    FAVORITES("Избранное"),
    RECENT("Недавние"),
    FOLDERS("По папкам"),
    LOCAL("На устройстве"),
    HIDDEN("Скрытые"),
    DEMO("Демо")
}

enum class SearchBarPlacement(
    val label: String,
    val subtitle: String,
    val localAtTop: Boolean,
    val ytAtTop: Boolean
) {
    LOCAL_TOP_YT_BOTTOM(
        label = "Сверху в локальной, снизу в YT Music",
        subtitle = "В шапке локальной медиатеки и отдельной кнопкой снизу в YT Music (по умолчанию)",
        localAtTop = true,
        ytAtTop = false
    ),
    LOCAL_BOTTOM_YT_TOP(
        label = "Снизу в локальной, сверху в YT Music",
        subtitle = "Отдельной кнопкой снизу в локальной музыке и в верхней шапке YT Music",
        localAtTop = false,
        ytAtTop = true
    ),
    ALWAYS_BOTTOM(
        label = "Везде снизу (отдельно от навбара)",
        subtitle = "Круглая кнопка поиска справа от нижней панели во всех режимах",
        localAtTop = false,
        ytAtTop = false
    ),
    ALWAYS_TOP(
        label = "Везде сверху (в шапке экрана)",
        subtitle = "Строка поиска всегда находится в верхней шапке",
        localAtTop = true,
        ytAtTop = true
    )
}

enum class SortOption(val label: String) {
    TITLE_ASC("По названию (А–Я)"),
    TITLE_DESC("По названию (Я–А)"),
    DATE_DESC("Сначала новые"),
    ARTIST("По исполнителю"),
    DURATION("По длительности")
}
