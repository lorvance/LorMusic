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

enum class AppIconOption(val label: String, val subtitle: String, val aliasClassName: String) {
    MINIMAL("Минимализм", "Лавандовый цветок с нотой", "com.lormusic.app.MainActivityAliasMinimal"),
    REGULAR("Стандартная", "Объёмный цветок с эквалайзером", "com.lormusic.app.MainActivityAliasRegular"),
    OLD("Классическая", "Классическая старая иконка", "com.lormusic.app.MainActivityAliasOld"),
    HALLOWEEN("Хэллоуин", "Ивентовая хэллоуинская иконка", "com.lormusic.app.MainActivityAliasHalloween");

    fun getDisplayName(strings: AppStrings): String {
        return when (this) {
            MINIMAL -> strings.iconStyleMinimal
            REGULAR -> strings.iconStyleDefault
            OLD -> strings.iconStyleClassic
            HALLOWEEN -> strings.halloweenIconTitle
        }
    }

    fun getDisplaySubtitle(strings: AppStrings): String {
        return when (this) {
            MINIMAL -> when (strings) {
                StringsEn -> "Lavender flower with note"
                StringsUk -> "Лавандова квітка з нотою"
                else -> subtitle
            }
            REGULAR -> when (strings) {
                StringsEn -> "3D flower with equalizer"
                StringsUk -> "Об'ємна квітка з еквалайзером"
                else -> subtitle
            }
            OLD -> when (strings) {
                StringsEn -> "Classic original icon"
                StringsUk -> "Класична стара іконка"
                else -> subtitle
            }
            HALLOWEEN -> strings.halloweenIconDesc
        }
    }

    companion object {
        fun isHalloweenExpired(): Boolean {
            return try {
                val now = java.time.LocalDateTime.now()
                val deadline = java.time.LocalDateTime.of(now.year, java.time.Month.NOVEMBER, 1, 13, 0)
                now.isAfter(deadline)
            } catch (_: Exception) {
                false
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
                else -> "Все"
            }
            FAVORITES -> strings.favorites
            RECENT -> strings.recentsTitle
            FOLDERS -> when (strings) {
                StringsEn -> "Folders"
                StringsUk -> "Папки"
                else -> "По папкам"
            }
            LOCAL -> strings.deviceMusicTitle
            HIDDEN -> strings.hiddenFolderName
            DEMO -> "Demo"
        }
    }
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
    );

    fun getDisplayName(strings: AppStrings): String {
        return when (this) {
            LOCAL_TOP_YT_BOTTOM -> when (strings) {
                StringsEn -> "Top in Library, bottom in YT Music"
                StringsUk -> "Зверху в медіатеці, знизу в YT Music"
                else -> label
            }
            LOCAL_BOTTOM_YT_TOP -> when (strings) {
                StringsEn -> "Bottom in Library, top in YT Music"
                StringsUk -> "Знизу в медіатеці, зверху в YT Music"
                else -> label
            }
            ALWAYS_BOTTOM -> strings.searchPlacementBottom
            ALWAYS_TOP -> strings.searchPlacementTop
        }
    }

    fun getDisplaySubtitle(strings: AppStrings): String {
        return when (this) {
            LOCAL_TOP_YT_BOTTOM -> when (strings) {
                StringsEn -> "In header of local library and separate bottom button in YT Music (default)"
                StringsUk -> "У шапці локальної медіатеки та окремою кнопкою знизу в YT Music (за замовчуванням)"
                else -> subtitle
            }
            LOCAL_BOTTOM_YT_TOP -> when (strings) {
                StringsEn -> "Separate bottom button in local music and in top header of YT Music"
                StringsUk -> "Окремою кнопкою знизу в локальній музиці та у верхній шапці YT Music"
                else -> subtitle
            }
            ALWAYS_BOTTOM -> when (strings) {
                StringsEn -> "Round search button next to bottom navigation bar in all modes"
                StringsUk -> "Кругла кнопка пошуку праворуч від нижньої панелі в усіх режимах"
                else -> subtitle
            }
            ALWAYS_TOP -> when (strings) {
                StringsEn -> "Search bar is always located in top header"
                StringsUk -> "Рядок пошуку завжди знаходиться у верхній шапці"
                else -> subtitle
            }
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
