package com.lormusic.app.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lormusic.app.data.YouTubeMusicRepository
import com.lormusic.app.model.LyricLine
import com.lormusic.app.model.RepeatMode
import com.lormusic.app.model.Song
import com.lormusic.app.ui.components.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

private enum class LyricsDisplayMode {
    SYNCED,
    PLAIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    song: Song,
    isPlaying: Boolean,
    isFavorite: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    playbackSpeed: Float,
    sleepTimerRemainingSec: Int?,
    queue: List<Song>,
    lyrics: List<LyricLine>,
    isLoadingLyrics: Boolean,
    useSquigglySlider: Boolean,
    useAlbumArtGradient: Boolean = false,
    enableSwipe: Boolean = true,
    onDownloadTrack: (() -> Unit)? = null,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleSpeed: () -> Unit,
    onSelectSleepTimer: (Int?) -> Unit,
    onPlayFromQueue: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onRefreshLyrics: () -> Unit,
    onOpenSongActions: () -> Unit,
    onOpenTagEditor: () -> Unit,
    onClose: () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val ytShareUrl = remember(song) { YouTubeMusicRepository.getShareUrl(song) }
    var showLyrics by remember { mutableStateOf(false) }

    BackHandler {
        if (showLyrics) {
            showLyrics = false
        } else {
            onClose()
        }
    }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragValue by remember { mutableFloatStateOf(0f) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }

    val accentColor = MaterialTheme.colorScheme.primary

    val effectiveDuration = (if (durationMs > 0) durationMs else song.durationMs).coerceAtLeast(1000L)
    val currentSliderValue = if (isDraggingSlider) {
        sliderDragValue
    } else {
        positionMs.toFloat().coerceIn(0f, effectiveDuration.toFloat())
    }

    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.93f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "coverScale"
    )

    val activeLyricLine = remember(lyrics, positionMs) {
        if (lyrics.isEmpty()) null
        else {
            lyrics.lastOrNull { !it.isTranslation && it.timeStampMs <= positionMs + 180L }
                ?: lyrics.firstOrNull { !it.isTranslation }
        }
    }

    val gradientColors = rememberAlbumArtGradient(song, useAlbumArtGradient, accentColor)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (useAlbumArtGradient) {
                    Modifier.background(
                        Brush.verticalGradient(gradientColors)
                    )
                } else {
                    Modifier.background(MaterialTheme.colorScheme.background)
                }
            )
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .align(Alignment.TopCenter)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (showLyrics) 0.14f else 0.24f),
                            Color.Transparent
                        )
                    )
                )
        )

        AnimatedContent(
            targetState = showLyrics,
            transitionSpec = {
                (fadeIn(tween(260)) + scaleIn(initialScale = 0.96f, animationSpec = tween(260)))
                    .togetherWith(fadeOut(tween(200)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200)))
            },
            modifier = Modifier.fillMaxSize(),
            label = "nowPlayingModeTransition"
        ) { isFullScreenLyrics ->
            if (isFullScreenLyrics) {
                FullScreenLyricsLayout(
                    song = song,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = effectiveDuration,
                    lyrics = lyrics,
                    isLoading = isLoadingLyrics,
                    accentColor = accentColor,
                    onTogglePlay = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onTogglePlay()
                    },
                    onPrevious = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onPrevious()
                    },
                    onNext = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onNext()
                    },
                    onSeekToLine = { timestampMs ->
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onSeek(timestampMs)
                    },
                    onRefresh = onRefreshLyrics,
                    onBackToCover = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        showLyrics = false
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilledTonalIconButton(
                            onClick = onClose,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Свернуть")
                        }
                        Text(
                            text = "Сейчас играет",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 72.dp)
                        )
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onOpenTagEditor) {
                                Icon(Icons.Filled.Edit, contentDescription = "Редактор тегов")
                            }
                            IconButton(onClick = onOpenSongActions) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Действия с треком")
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    var coverDragX by remember { mutableFloatStateOf(0f) }
                    val animatedCoverDragX by animateFloatAsState(
                        targetValue = coverDragX,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "nowPlayingCoverSwipe"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .pointerInput(enableSwipe) {
                                if (!enableSwipe) return@pointerInput
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { totalDrag = 0f },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        totalDrag += dragAmount
                                        coverDragX = totalDrag
                                    },
                                    onDragCancel = {
                                        coverDragX = 0f
                                        totalDrag = 0f
                                    },
                                    onDragEnd = {
                                        val threshold = 90f

                                        if (totalDrag > threshold) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                            onNext()
                                        } else if (totalDrag < -threshold) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                            onPrevious()
                                        }
                                        coverDragX = 0f
                                        totalDrag = 0f
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.92f)
                                .aspectRatio(1f)
                                .graphicsLayer {
                                    translationX = (animatedCoverDragX * 0.45f).coerceIn(-90f, 90f)
                                    rotationZ = (animatedCoverDragX * 0.025f).coerceIn(-6f, 6f)
                                }
                                .scale(coverScale)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    showLyrics = true
                                }
                        ) {
                            SongArtwork(
                                seed = song.coverColorSeed,
                                albumArtUri = song.albumArtUri,
                                songUri = song.uri,
                                cornerRadius = 30.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isPlaying) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp)
                                ) {
                                    AnimatedEqualizerBars(
                                        modifier = Modifier
                                            .padding(horizontal = 10.dp, vertical = 7.dp)
                                            .size(width = 24.dp, height = 18.dp),
                                        color = accentColor,
                                        barCount = 4
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(
                            targetState = song,
                            transitionSpec = {
                                (fadeIn(tween(240)) + slideInVertically { it / 3 })
                                    .togetherWith(fadeOut(tween(180)) + slideOutVertically { -it / 3 })
                            },
                            modifier = Modifier.weight(1f),
                            label = "songTitleAnim"
                        ) { targetSong ->
                            Column {
                                Text(
                                    text = targetSong.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = targetSong.artist,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (ytShareUrl != null) {
                                FilledTonalIconButton(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                        YouTubeMusicRepository.shareSongLink(context, song)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Поделиться ссылкой на песню"
                                    )
                                }
                            }
                            if (onDownloadTrack != null) {
                                FilledTonalIconButton(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                        onDownloadTrack()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = "Скачать трек"
                                    )
                                }
                            }
                            FilledTonalIconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    onToggleFavorite()
                                }
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Избранное",
                                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (useSquigglySlider) {
                        SquigglySlider(
                            value = currentSliderValue,
                            valueRange = 0f..effectiveDuration.toFloat(),
                            isPlaying = isPlaying,
                            activeColor = accentColor,
                            inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
                            thumbColor = accentColor,
                            onValueChange = {
                                isDraggingSlider = true
                                sliderDragValue = it
                            },
                            onValueChangeFinished = {
                                onSeek(sliderDragValue.toLong())
                                isDraggingSlider = false
                            }
                        )
                    } else {
                        Slider(
                            value = currentSliderValue,
                            onValueChange = {
                                isDraggingSlider = true
                                sliderDragValue = it
                            },
                            onValueChangeFinished = {
                                onSeek(sliderDragValue.toLong())
                                isDraggingSlider = false
                            },
                            valueRange = 0f..effectiveDuration.toFloat()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDurationShort(currentSliderValue.toLong()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                        )
                        Text(
                            text = formatDurationShort(effectiveDuration),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                onToggleShuffle()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Перемешать",
                                tint = if (shuffleOn) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                        FilledTonalIconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                onPrevious()
                            },
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Предыдущий",
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        WavyPlayPauseButton(
                            isPlaying = isPlaying,
                            onClick = onTogglePlay,
                            size = 78.dp
                        )

                        FilledTonalIconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                onNext()
                            },
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Следующий",
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                onCycleRepeat()
                            }
                        ) {
                            Icon(
                                imageVector = when (repeatMode) {
                                    RepeatMode.ONE -> Icons.Filled.RepeatOne
                                    else -> Icons.Filled.Repeat
                                },
                                contentDescription = "Режим повтора",
                                tint = if (repeatMode != RepeatMode.OFF) {
                                    accentColor
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = showLyrics,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                showLyrics = true
                            },
                            label = { Text("Текст") },
                            leadingIcon = { Icon(Icons.Filled.Lyrics, contentDescription = null, modifier = Modifier.size(17.dp)) },
                            shape = RoundedCornerShape(16.dp)
                        )
                        AssistChip(
                            onClick = onCycleSpeed,
                            label = { Text("${playbackSpeed}x") },
                            leadingIcon = { Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(17.dp)) },
                            shape = RoundedCornerShape(16.dp)
                        )
                        AssistChip(
                            onClick = { showSleepDialog = true },
                            label = {
                                val timerText = sleepTimerRemainingSec?.let { formatDurationShort(it * 1000L) } ?: "Таймер"
                                Text(timerText)
                            },
                            leadingIcon = { Icon(Icons.Filled.Bedtime, contentDescription = null, modifier = Modifier.size(17.dp)) },
                            shape = RoundedCornerShape(16.dp)
                        )
                        AssistChip(
                            onClick = { showQueueSheet = true },
                            label = { Text("${queue.size}") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, modifier = Modifier.size(17.dp)) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    LiveBottomLyricBanner(
                        activeLine = activeLyricLine,
                        isLoadingLyrics = isLoadingLyrics,
                        isPlaying = isPlaying,
                        accentColor = accentColor,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            showLyrics = true
                        }
                    )
                }
            }
        }
    }

    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Очередь воспроизведения (${queue.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    itemsIndexed(queue, key = { idx, s -> "${s.id}_$idx" }) { index, qSong ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                SongRow(
                                    song = qSong,
                                    isCurrentSong = qSong.id == song.id,
                                    isPlaying = isPlaying,
                                    onClick = {
                                        onPlayFromQueue(index)
                                        showQueueSheet = false
                                    }
                                )
                            }
                            IconButton(onClick = { onRemoveFromQueue(index) }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Убрать из очереди",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text("Таймер сна") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        null to "Выключить",
                        5 to "5 минут",
                        15 to "15 минут",
                        30 to "30 минут",
                        60 to "60 минут"
                    ).forEach { (mins, label) ->
                        TextButton(
                            onClick = {
                                onSelectSleepTimer(mins)
                                showSleepDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Composable
private fun LiveBottomLyricBanner(
    activeLine: LyricLine?,
    isLoadingLyrics: Boolean,
    isPlaying: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lyrics,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))

            AnimatedContent(
                targetState = Triple(isLoadingLyrics, activeLine?.timeStampMs, activeLine?.content),
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically { it / 2 })
                        .togetherWith(fadeOut(tween(160)) + slideOutVertically { -it / 2 })
                },
                modifier = Modifier.weight(1f),
                label = "bottomLiveLyricAnim"
            ) { (loading, _, content) ->
                when {
                    loading -> {
                        Text(
                            text = "Загрузка синхронного текста…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    activeLine == null -> {
                        Text(
                            text = "Нажми, чтобы открыть текст песни",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    activeLine.isInstrumental || content.isNullOrBlank() -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            InstrumentalDotsIndicatorCompact(
                                isPlaying = isPlaying,
                                accentColor = accentColor
                            )
                        }
                    }

                    else -> {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstrumentalDotsIndicatorCompact(
    isPlaying: Boolean,
    accentColor: Color
) {
    val transition = rememberInfiniteTransition(label = "compactDots")
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 3) {
            val bounce by transition.animateFloat(
                initialValue = -3f,
                targetValue = 3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(480, easing = FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                    initialStartOffset = StartOffset(i * 140)
                ),
                label = "compactDot_$i"
            )
            Box(
                modifier = Modifier
                    .size(if (i == 2) 7.dp else 9.dp)
                    .offset(y = if (isPlaying) bounce.dp else 0.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }
    }
}

@Composable
private fun FullScreenLyricsLayout(
    song: Song,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    lyrics: List<LyricLine>,
    isLoading: Boolean,
    accentColor: Color,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekToLine: (Long) -> Unit,
    onRefresh: () -> Unit,
    onBackToCover: () -> Unit
) {
    var displayMode by remember { mutableStateOf(LyricsDisplayMode.SYNCED) }
    val listState = rememberLazyListState()
    val lyricsHazeState = remember { HazeState() }

    val activeIndex = remember(lyrics, positionMs) {
        if (lyrics.isEmpty()) -1
        else {
            lyrics.indexOfLast { !it.isTranslation && it.timeStampMs <= positionMs + 150L }
                .coerceAtLeast(0)
        }
    }

    LaunchedEffect(activeIndex, displayMode) {
        if (displayMode == LyricsDisplayMode.SYNCED && activeIndex >= 0 && lyrics.isNotEmpty()) {
            runCatching {
                listState.animateScrollToItem(
                    index = (activeIndex - 1).coerceAtLeast(0)
                )
            }
        }
    }

    val progress = (positionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    val cardBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp, bottom = 10.dp)
                .width(40.dp)
                .height(4.5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                .clickable { onBackToCover() }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .clickable { onBackToCover() }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongArtwork(
                    seed = song.coverColorSeed,
                    albumArtUri = song.albumArtUri,
                    songUri = song.uri,
                    cornerRadius = 12.dp,
                    modifier = Modifier.size(50.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Предыдущий",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
                WavyPlayPauseButton(
                    isPlaying = isPlaying,
                    onClick = onTogglePlay,
                    size = 44.dp
                )
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Следующий",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .clip(CircleShape),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(cardBgColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
                    .hazeSource(state = lyricsHazeState)
            ) {
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ExpressiveLoadingIndicator(size = 58.dp, color = accentColor)
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = "Ищем синхронный текст по названию…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    lyrics.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Lyrics,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Синхронный текст для этого трека не найден",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Поддерживается поиск по названию (LRCLIB), пользовательский текст из редактора тегов и .lrc",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.height(14.dp))
                                FilledTonalButton(onClick = onRefresh) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Повторить поиск")
                                }
                            }
                        }
                    }

                    else -> {
                        val visibleLines = remember(lyrics, displayMode) {
                            if (displayMode == LyricsDisplayMode.PLAIN) {
                                lyrics.filter { !it.isInstrumental }
                            } else {
                                lyrics
                            }
                        }

                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 36.dp,
                                bottom = 104.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (displayMode == LyricsDisplayMode.SYNCED && visibleLines.firstOrNull()?.isInstrumental != true) {
                                item(key = "intro_dots") {
                                    val introActive = activeIndex <= 0 && positionMs < (visibleLines.firstOrNull()?.timeStampMs ?: 3000L)
                                    InstrumentalDotsIndicator(
                                        isActive = introActive,
                                        isPlaying = isPlaying,
                                        accentColor = accentColor,
                                        inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f),
                                        onClick = { onSeekToLine(0L) }
                                    )
                                }
                            }

                            itemsIndexed(visibleLines) { index, line ->
                                val isActive = displayMode == LyricsDisplayMode.SYNCED && (
                                    index == activeIndex ||
                                        (line.isTranslation && index - 1 == activeIndex)
                                    )

                                val targetColor = when {
                                    displayMode == LyricsDisplayMode.PLAIN -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                                    isActive -> accentColor
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
                                }
                                val textColor by animateColorAsState(
                                    targetValue = targetColor,
                                    animationSpec = tween(240),
                                    label = "lyricColor"
                                )
                                val lineScale by animateFloatAsState(
                                    targetValue = if (isActive) 1.04f else 0.96f,
                                    animationSpec = tween(240),
                                    label = "lyricScale"
                                )

                                if (line.isInstrumental) {
                                    InstrumentalDotsIndicator(
                                        isActive = isActive,
                                        isPlaying = isPlaying,
                                        accentColor = accentColor,
                                        inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f),
                                        onClick = { onSeekToLine(line.timeStampMs) }
                                    )
                                } else {
                                    Text(
                                        text = line.content,
                                        fontSize = if (line.isTranslation) 18.sp else 25.sp,
                                        lineHeight = if (line.isTranslation) 24.sp else 33.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center,
                                        color = textColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .scale(lineScale)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { onSeekToLine(line.timeStampMs) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val controlsSurface = MaterialTheme.colorScheme.surfaceVariant
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .hazeEffect(state = lyricsHazeState) {
                            blurEnabled = true
                            backgroundColor = controlsSurface
                            blurRadius = 24.dp
                            tints = listOf(HazeTint(controlsSurface.copy(alpha = 0.55f)))
                        }
                        .clickable { onRefresh() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Обновить текст",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(CircleShape)
                        .hazeEffect(state = lyricsHazeState) {
                            blurEnabled = true
                            backgroundColor = controlsSurface
                            blurRadius = 24.dp
                            tints = listOf(HazeTint(controlsSurface.copy(alpha = 0.55f)))
                        }
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val syncedSelected = displayMode == LyricsDisplayMode.SYNCED
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                if (syncedSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                else Color.Transparent
                            )
                            .clickable { displayMode = LyricsDisplayMode.SYNCED },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Синхронизирован",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (syncedSelected) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }

                    val plainSelected = displayMode == LyricsDisplayMode.PLAIN
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                if (plainSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                else Color.Transparent
                            )
                            .clickable { displayMode = LyricsDisplayMode.PLAIN },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Простой",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (plainSelected) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstrumentalDotsIndicator(
    isActive: Boolean,
    isPlaying: Boolean,
    accentColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "instrumentalDots")
    val animateNow = isActive && isPlaying

    val dotColor by animateColorAsState(
        targetValue = if (isActive) accentColor else inactiveColor,
        animationSpec = tween(250),
        label = "dotColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val baseSizes = listOf(16.dp, 16.dp, 10.dp)
        for (i in 0 until 3) {
            val bounceOffset by transition.animateFloat(
                initialValue = -6f,
                targetValue = 6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 520,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                    initialStartOffset = StartOffset(i * 150)
                ),
                label = "dotBounce_$i"
            )
            val pulseAlpha by transition.animateFloat(
                initialValue = if (i == 2) 0.45f else 0.75f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 520, easing = FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                    initialStartOffset = StartOffset(i * 150)
                ),
                label = "dotAlpha_$i"
            )
            val effectiveOffsetY = if (animateNow) bounceOffset.dp else 0.dp
            val effectiveAlpha = if (isActive) pulseAlpha else (if (i == 2) 0.4f else 0.75f)

            Box(
                modifier = Modifier
                    .size(baseSizes[i])
                    .offset(y = effectiveOffsetY)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = effectiveAlpha))
            )
        }
    }
}

@Composable
private fun rememberAlbumArtGradient(
    song: Song,
    enabled: Boolean,
    fallbackColor: Color
): List<Color> {
    val context = LocalContext.current
    val surfaceColor = MaterialTheme.colorScheme.background
    return produceState(
        initialValue = listOf(surfaceColor, surfaceColor),
        key1 = song.id,
        key2 = song.albumArtUri,
        key3 = enabled
    ) {
        if (!enabled) {
            value = listOf(surfaceColor, surfaceColor)
            return@produceState
        }
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val extracted = extractSongGradientColors(context, song, fallbackColor)
            value = listOf(extracted.first, extracted.second, surfaceColor)
        }
    }.value
}

private fun extractSongGradientColors(
    context: android.content.Context,
    song: Song,
    fallback: Color
): Pair<Color, Color> {
    val art = song.albumArtUri
    if (!art.isNullOrBlank()) {
        try {
            val bmp = if (art.startsWith("content://") || art.startsWith("file://")) {
                val stream = context.contentResolver.openInputStream(android.net.Uri.parse(art))
                val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 8 }
                val b = android.graphics.BitmapFactory.decodeStream(stream, null, opts)
                stream?.close()
                b
            } else if (art.startsWith("http://") || art.startsWith("https://")) {
                val conn = java.net.URL(art).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 2500
                conn.readTimeout = 2500
                val stream = conn.inputStream
                val b = android.graphics.BitmapFactory.decodeStream(stream)
                stream.close()
                conn.disconnect()
                b
            } else null

            if (bmp != null) {
                val p1 = bmp.getPixel(bmp.width / 4, bmp.height / 4)
                val p2 = bmp.getPixel((bmp.width * 3) / 4, (bmp.height * 3) / 4)
                bmp.recycle()
                return Pair(
                    Color(p1).copy(alpha = 0.55f),
                    Color(p2).copy(alpha = 0.40f)
                )
            }
        } catch (_: Exception) {
        }
    }
    val hash = kotlin.math.abs((song.title + song.artist).hashCode())
    val hue1 = (hash % 360).toFloat()
    val hue2 = ((hash + 130) % 360).toFloat()
    return Pair(
        Color.hsv(hue1, 0.70f, 0.50f).copy(alpha = 0.50f),
        Color.hsv(hue2, 0.60f, 0.40f).copy(alpha = 0.40f)
    )
}
