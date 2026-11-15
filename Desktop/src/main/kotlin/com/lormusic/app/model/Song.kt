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
    val isRemoteSc: Boolean = false,
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
            customLyrics = ov.customLyrics ?: customLyrics,
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
    val customLyrics: String? = null,
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
    val isSelected: Boolean = true,
)

data class LyricLine(
    val timeStampMs: Long = 0L,
    val content: String = "",
    val isTranslation: Boolean = false,
    val isInstrumental: Boolean = false,
)

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song>,
    val coverColorSeed: Int = name.hashCode(),
    val isSystem: Boolean = false,
    val isUnified: Boolean = false,
)

data class SoundCloudUserProfile(
    val username: String,
    val avatarUrl: String? = null,
    val permalinkUrl: String? = null,
    val trackCount: Int = 0,
    val likesCount: Int = 0,
    val followersCount: Int = 0,
    val followingsCount: Int = 0,
)

data class GoogleAccountProfile(
    val displayName: String,
    val email: String,
    val avatarUrl: String? = null,
)

enum class RepeatMode {
    OFF,
    ALL,
    ONE,
}
