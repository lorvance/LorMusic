package com.lormusic.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.lormusic.app.model.Song
import java.io.File
import kotlin.math.abs

object LocalMusicScanner {

    private val genericFolderAlbumNames = setOf(
        "telegram",
        "telegram audio",
        "telegram documents",
        "download",
        "downloads",
        "music",
        "музыка",
        "audio",
        "notifications",
        "ringtones",
        "tiktok",
        "whatsapp audio",
        "records",
        "recordings"
    )

    fun scanDeviceMusic(context: Context, minDurationSec: Int = 3): List<Song> {
        val result = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val minDurationMs = (minDurationSec * 1000).coerceAtLeast(1000)
        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.MIME_TYPE} IN ('audio/x-wav','audio/ogg','audio/aac','audio/mpeg','audio/mp4','audio/x-m4a')) AND ${MediaStore.Audio.Media.DURATION} >= $minDurationMs"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

                val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol)?.trim().orEmpty()
                    val rawArtist = cursor.getString(artistCol)?.trim().orEmpty()
                    val rawAlbum = cursor.getString(albumCol)?.trim().orEmpty()
                    val duration = cursor.getLong(durationCol).coerceAtLeast(1000L)
                    val albumId = cursor.getLong(albumIdCol)
                    val filePath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val dateAdded = if (dateAddedCol >= 0) cursor.getLong(dateAddedCol) else 0L

                    val folderName = filePath
                        ?.let { File(it).parentFile?.name }
                        ?.takeIf { it.isNotBlank() }
                        ?: rawAlbum.ifBlank { "Музыка" }

                    val title = if (rawTitle.isBlank() || rawTitle == "<unknown>") {
                        filePath?.let { File(it).nameWithoutExtension } ?: "Без названия #$id"
                    } else rawTitle

                    val artist = if (rawArtist.isBlank() || rawArtist == "<unknown>") "Неизвестный исполнитель" else rawArtist
                    val album = if (rawAlbum.isBlank() || rawAlbum == "<unknown>") folderName else rawAlbum

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val isGenericFolderAlbum = rawAlbum.isBlank() ||
                        rawAlbum == "<unknown>" ||
                        rawAlbum.equals(folderName, ignoreCase = true) ||
                        genericFolderAlbumNames.contains(rawAlbum.lowercase())

                    val cachedCoverFile = File(File(context.filesDir, "tagfix_covers"), "cover_local_${id}.jpg")
                    val artUri = when {
                        cachedCoverFile.exists() && cachedCoverFile.length() > 128L -> Uri.fromFile(cachedCoverFile).toString()
                        !isGenericFolderAlbum && albumId > 0 -> ContentUris.withAppendedId(albumArtBaseUri, albumId).toString()
                        else -> null
                    }

                    result.add(
                        Song(
                            id = "local_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            uri = contentUri.toString(),
                            coverColorSeed = abs((title + artist + id).hashCode() % 100) + 1,
                            albumArtUri = artUri,
                            isLocal = true,
                            filePath = filePath,
                            folderName = folderName,
                            dateAddedSec = dateAdded
                        )
                    )
                }
            }
        } catch (_: SecurityException) {

        } catch (_: Exception) {

        }

        val comparator = AlphaNumericComparator()
        return result.sortedWith { a, b -> comparator.compare(a.title, b.title) }
    }
}
