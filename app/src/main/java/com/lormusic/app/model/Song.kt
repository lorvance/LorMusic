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
    val isRemoteYt: Boolean = false,
    val isRemoteSc: Boolean = false
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

const val HIDDEN_TRACKS_PLAYLIST_ID = "sys_hidden_tracks"

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song>,
    val coverColorSeed: Int = name.hashCode(),
    val isSystem: Boolean = false,
    val isUnified: Boolean = false
)

data class SoundCloudUserProfile(
    val username: String,
    val avatarUrl: String? = null,
    val permalinkUrl: String? = null,
    val trackCount: Int = 0,
    val likesCount: Int = 0,
    val followersCount: Int = 0,
    val followingsCount: Int = 0
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
    LIGHT("Светлая");

    fun getDisplayName(strings: AppStrings): String {
        return when (this) {
            SYSTEM -> strings.themeSystem
            DARK -> strings.themeDark
            LIGHT -> strings.themeLight
        }
    }
}

enum class AppAccentColor(
    val baseName: String,
    val primaryHex: Long,
    val isDefault: Boolean = false,
    val lightHex: Long? = null,
    val deepHex: Long? = null
) {
    PIXEL_BLUE("Pixel Blue", 0xFF6489D8, isDefault = true),
    SOFT_LAVENDER("Soft Lavender", 0xFFA18BD4),
    SAGE_GREEN("Sage Green", 0xFF719B83),
    PEACH_CORAL("Peach Coral", 0xFFD98B79),
    GOLDEN_SAND("Golden Sand", 0xFFC6A15D),
    DUSTY_TEAL("Dusty Teal", 0xFF5D9EAA),
    DUSTY_ROSE("Dusty Rose", 0xFFC47FA3),
    CHERRY_WINE("Cherry Wine", 0xFFB65D70, lightHex = 0xFFE8A3B2, deepHex = 0xFF713344),
    SMOKY_INDIGO("Smoky Indigo", 0xFF6469AD, lightHex = 0xFFB2B5EF, deepHex = 0xFF393C76),
    BURNT_AMBER("Burnt Amber", 0xFFC48250, lightHex = 0xFFF0B98F, deepHex = 0xFF80502F);

    fun getDisplayName(strings: AppStrings): String {
        if (!isDefault) return baseName
        val suffix = when (strings) {
            StringsEn -> "Default"
            StringsUk -> "За замовчуванням"
            StringsDe -> "Standard"
            StringsPl -> "Domyślny"
            else -> "По умолчанию"
        }
        return "$baseName | $suffix"
    }
}

enum class AppIconOption(val label: String, val subtitle: String, val aliasClassName: String) {
    PULSE("LorMusic Pulse", "Минималистичная иконка с буквой L и символом воспроизведения в голубых оттенках", "com.lormusic.app.MainActivityAliasPulse"),
    MINIMAL("Минимализм", "Голубой цветок с двойной нотой", "com.lormusic.app.MainActivityAliasMinimal"),
    REGULAR("Стандартная", "Объёмный цветок с эквалайзером", "com.lormusic.app.MainActivityAliasRegular"),
    OLD("Классическая", "Классическая старая иконка", "com.lormusic.app.MainActivityAliasOld");

    fun getDisplayName(strings: AppStrings): String {
        return when (this) {
            PULSE -> "LorMusic Pulse"
            MINIMAL -> strings.iconStyleMinimal
            REGULAR -> strings.iconStyleDefault
            OLD -> strings.iconStyleClassic
        }
    }

    fun getDisplaySubtitle(strings: AppStrings): String {
        return when (this) {
            PULSE -> when(strings) {
                StringsEn -> "Minimalist blue L with a play symbol"
                StringsDe -> "Minimalistisches blaues L mit Wiedergabesymbol"
                StringsUk -> "Мінімалістична блакитна L із символом відтворення"
                StringsPl -> "Minimalistyczna niebieska litera L z symbolem odtwarzania"
                else -> subtitle
            }
            MINIMAL -> when (strings) {
                StringsEn -> "Light blue flower with a double note"
                StringsUk -> "Блакитна квітка з подвійною нотою"
                StringsDe -> "Hellblaue Blume mit Doppelnote"
                StringsPl -> "Błękitny kwiat z podwójną nutą"
                else -> subtitle
            }
            REGULAR -> when (strings) {
                StringsEn -> "3D flower with equalizer"
                StringsUk -> "Об'ємна квітка з еквалайзером"
                StringsDe -> "3D-Blume mit Equalizer"
                StringsPl -> "Kwiat 3D z korektorem"
                else -> subtitle
            }
            OLD -> when (strings) {
                StringsEn -> "Classic original icon"
                StringsUk -> "Класична стара іконка"
                StringsDe -> "Klassisches Originalsymbol"
                StringsPl -> "Klasyczna oryginalna ikona"
                else -> subtitle
            }
        }
    }
}

enum class LibraryFilter(val label: String) {
    ALL("Все"),
    FAVORITES("Избранное"),
    RECENT("Недавние"),
    FOLDERS("По папкам"),
    LOCAL("На устройстве"),
    HIDDEN("Скрытые"),
    DEMO("Демо");

    fun getDisplayName(strings: AppStrings): String {
        return when (this) {
            ALL -> when (strings) {
                StringsEn -> "All"
                StringsUk -> "Всі"
                StringsDe -> "Alle"
                StringsPl -> "Wszystkie"
                else -> "Все"
            }
            FAVORITES -> strings.favorites
            RECENT -> strings.recentsTitle
            FOLDERS -> when (strings) {
                StringsEn -> "Folders"
                StringsUk -> "Папки"
                StringsDe -> "Ordner"
                StringsPl -> "Foldery"
                else -> "По папкам"
            }
            LOCAL -> strings.deviceMusicTitle
            HIDDEN -> strings.hiddenFolderName
            DEMO -> "Demo"
        }
    }
}

enum class SortOption(val label: String) {
    TITLE_ASC("По названию (А–Я)"),
    TITLE_DESC("По названию (Я–А)"),
    DATE_DESC("Сначала новые"),
    ARTIST("По исполнителю"),
    DURATION("По длительности");

    fun getDisplayName(strings: AppStrings): String {
        return when (this) {
            TITLE_ASC -> strings.sortOptionTitleAsc
            TITLE_DESC -> strings.sortOptionTitleDesc
            DATE_DESC -> strings.sortOptionDateDesc
            ARTIST -> strings.sortOptionArtist
            DURATION -> strings.sortOptionDuration
        }
    }
}
