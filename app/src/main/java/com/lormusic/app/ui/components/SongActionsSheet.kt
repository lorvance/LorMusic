package com.lormusic.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lormusic.app.data.YouTubeMusicRepository
import com.lormusic.app.model.Playlist
import com.lormusic.app.model.Song

@Composable
fun SongRow(
    song: Song,
    isCurrentSong: Boolean = false,
    isPlaying: Boolean = false,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null
) {
    val view = LocalView.current
    val containerColor = if (isCurrentSong) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onClick()
            },
        color = containerColor,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongArtwork(
                seed = song.coverColorSeed,
                albumArtUri = song.albumArtUri,
                songUri = song.uri,
                cornerRadius = 14.dp,
                showEqualizerOverlay = isCurrentSong,
                isPlaying = isCurrentSong && isPlaying,
                modifier = Modifier.size(50.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.album} • ${formatDurationShort(song.durationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onToggleFavorite != null) {
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onToggleFavorite()
                    }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Избранное",
                        tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
            if (onMoreClick != null) {
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Ещё",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionsBottomSheet(
    song: Song,
    isFavorite: Boolean,
    isHidden: Boolean = false,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onCreateNewPlaylistWithSong: (String) -> Unit,
    onOpenTagEditor: (() -> Unit)? = null,
    onDownloadSong: (() -> Unit)? = null,
    onToggleHidden: (() -> Unit)? = null,
    onDeleteFromDevice: (() -> Unit)? = null,
    onOpenArtistProfile: ((String) -> Unit)? = null
) {
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showTrackDetails by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val ytShareUrl = remember(song) { YouTubeMusicRepository.getShareUrl(song) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongArtwork(
                    seed = song.coverColorSeed,
                    albumArtUri = song.albumArtUri,
                    songUri = song.uri,
                    cornerRadius = 14.dp,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${song.artist} • ${song.album}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            when {
                showDeleteConfirm -> {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(
                            text = "Удалить трек с устройства?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Файл «${song.title}» будет безвозвратно удалён из памяти вашего смартфона. Если вы хотите просто убрать его из общего списка, используйте функцию «Скрыть из потока».",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDeleteConfirm = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Отмена")
                            }
                            Button(
                                onClick = {
                                    onDeleteFromDevice?.invoke()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Удалить")
                            }
                        }
                    }
                }

                showTrackDetails -> {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text("Сведения о файле", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Название: ${song.title}", style = MaterialTheme.typography.bodyMedium)
                        Text("Исполнитель: ${song.artist}", style = MaterialTheme.typography.bodyMedium)
                        Text("Альбом: ${song.album}", style = MaterialTheme.typography.bodyMedium)
                        if (song.genre.isNotBlank()) {
                            Text("Жанр: ${song.genre}", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (song.year.isNotBlank()) {
                            Text("Год: ${song.year}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("Папка: ${if (isHidden) "Скрытые (${song.folderName})" else song.folderName}", style = MaterialTheme.typography.bodyMedium)
                        Text("Длительность: ${formatDurationShort(song.durationMs)}", style = MaterialTheme.typography.bodyMedium)
                        if (!song.filePath.isNullOrBlank()) {
                            Text(
                                "Путь: ${song.filePath}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        FilledTonalButton(onClick = { showTrackDetails = false }) {
                            Text("Назад")
                        }
                    }
                }

                !showPlaylistPicker -> {
                    ListItem(
                        headlineContent = { Text("Воспроизвести сейчас") },
                        leadingContent = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                        modifier = Modifier.clickable {
                            onPlayNow()
                            onDismiss()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Сыграть следующим в очереди") },
                        leadingContent = { Icon(Icons.Filled.QueuePlayNext, contentDescription = null) },
                        modifier = Modifier.clickable {
                            onPlayNext()
                            onDismiss()
                        }
                    )
                    if (onOpenArtistProfile != null && song.artist.isNotBlank()) {
                        ListItem(
                            headlineContent = { Text("Исполнитель: ${song.artist}") },
                            leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                            modifier = Modifier.clickable {
                                onDismiss()
                                onOpenArtistProfile(song.artist)
                            }
                        )
                    }
                    if (onOpenTagEditor != null) {
                        ListItem(
                            headlineContent = { Text("Редактор тегов и обложки") },
                            supportingContent = { Text("Изменить название, артиста, обложку, жанр, текст") },
                            leadingContent = {
                                Icon(
                                    Icons.Filled.EditNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable {
                                onDismiss()
                                onOpenTagEditor()
                            }
                        )
                    }
                    if (onDownloadSong != null && (song.isRemoteYt || song.uri.startsWith("http") || song.uri.startsWith("yt://"))) {
                        ListItem(
                            headlineContent = { Text("Скачать на телефон (со всеми тегами)") },
                            supportingContent = { Text("Сохранить аудио и обложку в локальную память") },
                            leadingContent = {
                                Icon(
                                    Icons.Filled.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable {
                                onDownloadSong()
                                onDismiss()
                            }
                        )
                    }
                    if (ytShareUrl != null) {
                        ListItem(
                            headlineContent = { Text("Поделиться песней") },
                            supportingContent = { Text("Отправить ссылку: $ytShareUrl") },
                            leadingContent = {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable {
                                YouTubeMusicRepository.shareSongLink(context, song)
                                onDismiss()
                            }
                        )
                    }
                    ListItem(
                        headlineContent = {
                            Text(if (isFavorite) "Убрать из «Мне нравится»" else "Добавить в «Мне нравится»")
                        },
                        leadingContent = {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) MaterialTheme.colorScheme.secondary else LocalContentColor.current
                            )
                        },
                        modifier = Modifier.clickable {
                            onToggleFavorite()
                            onDismiss()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Добавить в плейлист…") },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showPlaylistPicker = true
                        }
                    )
                    if (onToggleHidden != null) {
                        ListItem(
                            headlineContent = {
                                Text(if (isHidden) "Вернуть в основной поток" else "Скрыть из потока")
                            },
                            supportingContent = {
                                Text(
                                    if (isHidden) "Снова показывать трек во всех списках"
                                    else "Переместить в папку «Скрытые» (останется на устройстве)"
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = if (isHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable {
                                onToggleHidden()
                                onDismiss()
                            }
                        )
                    }
                    ListItem(
                        headlineContent = { Text("Сведения о треке") },
                        leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showTrackDetails = true
                        }
                    )
                    if (onDeleteFromDevice != null && !song.isRemoteYt && !song.uri.startsWith("http") && !song.uri.startsWith("yt://")) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = "Удалить с устройства",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            supportingContent = {
                                Text("Удалить аудиофайл из памяти телефона")
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.DeleteForever,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            modifier = Modifier.clickable {
                                showDeleteConfirm = true
                            }
                        )
                    }
                }

                else -> {
                    Text(
                        text = "Выберите плейлист",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(playlists.filter { !it.isSystem }) { pl ->
                            ListItem(
                                headlineContent = { Text(pl.name) },
                                supportingContent = { Text("${pl.songs.size} треков") },
                                leadingContent = {
                                    SongArtwork(
                                        seed = pl.coverColorSeed,
                                        cornerRadius = 10.dp,
                                        modifier = Modifier.size(40.dp)
                                    )
                                },
                                modifier = Modifier.clickable {
                                    onAddToPlaylist(pl.id)
                                    onDismiss()
                                }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            placeholder = { Text("Новый плейлист…") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(
                            enabled = newPlaylistName.isNotBlank(),
                            onClick = {
                                onCreateNewPlaylistWithSong(newPlaylistName.trim())
                                onDismiss()
                            }
                        ) {
                            Text("Создать")
                        }
                    }
                }
            }
        }
    }
}

fun formatDurationShort(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0L)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
