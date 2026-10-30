package com.lormusic.app.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.collection.LruCache
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.BiasAlignment
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
import com.lormusic.app.data.SoundCloudRepository
import com.lormusic.app.data.YouTubeMusicRepository
import com.lormusic.app.model.AppIconOption
import com.lormusic.app.model.AppStrings
import com.lormusic.app.model.LyricLine
import com.lormusic.app.model.PlaybackContext
import com.lormusic.app.model.PlaybackContextType
import com.lormusic.app.model.Playlist
import com.lormusic.app.model.RepeatMode
import com.lormusic.app.model.Song
import com.lormusic.app.model.LocalStrings
import com.lormusic.app.ui.components.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    playbackSpeed: Float = 1.0f,
    sleepTimerRemainingSec: Int?,
    queue: List<Song>,
    lyrics: List<LyricLine>,
    isLoadingLyrics: Boolean,
    useSquigglySlider: Boolean,
    useAlbumArtGradient: Boolean = false,
    useDynamicPlayerColors: Boolean = true,
    enableSwipe: Boolean = true,
    betaDynamicPlaybackContext: Boolean = false,
    playbackContext: PlaybackContext? = null,
    playlists: List<Playlist> = emptyList(),
    isDownloading: Boolean = false,
    downloadProgressFraction: Float = 0f,
    onDownloadTrack: (() -> Unit)? = null,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleSpeed: () -> Unit = {},
    onAddToPlaylist: (String, Song) -> Unit = { _, _ -> },
    onRemoveFromPlaylist: (String, String) -> Unit = { _, _ -> },
    onCreatePlaylist: (String) -> Unit = {},
    onSelectSleepTimer: (Int?) -> Unit,
    onPlayFromQueue: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onRefreshLyrics: () -> Unit,
    onOpenSongActions: () -> Unit,
    onOpenTagEditor: () -> Unit,
    onOpenArtistProfile: ((String) -> Unit)? = null,
    onTranslateLyrics: (() -> Unit)? = null,
    halloweenCandies: Boolean = false,
    onEatCandies: () -> Unit = {},
    onClose: () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val strings = LocalStrings.current
    val areCandiesActive = halloweenCandies && !AppIconOption.isHalloweenExpired()
    val ytShareUrl = remember(song) { YouTubeMusicRepository.getShareUrl(song) }
    val scShareUrl = remember(song) { SoundCloudRepository.getShareUrl(song) }
    val shareUrl = ytShareUrl ?: scShareUrl
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
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }

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
    val cachedDominant = remember(song.id, gradientColors) {
        coverAccentCache.get(song.id) ?: gradientColors.firstOrNull()?.copy(alpha = 1.0f) ?: accentColor
    }
    val effectiveAccentColor = if (useDynamicPlayerColors) ensureReadableAccent(cachedDominant) else accentColor
    val playButtonContentColor = if (useDynamicPlayerColors) getContrastingContentColor(effectiveAccentColor) else MaterialTheme.colorScheme.onPrimary

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
                    onTranslate = onTranslateLyrics,
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
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = strings.collapse)
                        }
                        if (betaDynamicPlaybackContext) {
                            val contextLabels = remember(song, playbackContext, strings) {
                                getDynamicPlaybackContextLabels(song, playbackContext, strings)
                            }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 56.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = contextLabels.first.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    letterSpacing = 1.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = contextLabels.second,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Text(
                                text = strings.nowPlayingTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 56.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenSongActions,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = strings.songActions)
                        }
                    }

                    AnimatedVisibility(
                        visible = isDownloading,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = null,
                                        tint = effectiveAccentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "${strings.downloadingSong}: ${song.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (downloadProgressFraction > 0f) {
                                        Text(
                                            text = "${(downloadProgressFraction * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = effectiveAccentColor
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                WavyLinearProgressIndicator(
                                    progress = if (downloadProgressFraction > 0f) downloadProgressFraction else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(18.dp),
                                    activeColor = effectiveAccentColor
                                )
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

                                        if (totalDrag < -threshold) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                            onNext()
                                        } else if (totalDrag > threshold) {
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
                            if (areCandiesActive) {
                                HalloweenCoverCandies(
                                    onEatAllCandies = onEatCandies,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 10.dp)
                                )
                            }
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

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AnimatedContent(
                                targetState = song,
                                transitionSpec = {
                                    (fadeIn(tween(240)) + slideInVertically { it / 3 })
                                        .togetherWith(fadeOut(tween(180)) + slideOutVertically { -it / 3 })
                                },
                                label = "songTitleAnim"
                            ) { targetSong ->
                                Text(
                                    text = targetSong.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                        onOpenArtistProfile?.invoke(song.artist)
                                    }
                                    .padding(vertical = 2.dp)
                            )
                        }

                        val isSavedAnywhere = isFavorite || playlists.any { pl -> !pl.isSystem && pl.songs.any { it.id == song.id } }
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                showAddToPlaylistSheet = true
                            }
                        ) {
                            Icon(
                                imageVector = if (isSavedAnywhere) Icons.Filled.CheckCircle else Icons.Filled.AddCircleOutline,
                                contentDescription = strings.addToPlaylist,
                                tint = if (isSavedAnywhere) effectiveAccentColor else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    if (useSquigglySlider) {
                        SquigglySlider(
                            value = currentSliderValue,
                            valueRange = 0f..effectiveDuration.toFloat(),
                            isPlaying = isPlaying,
                            activeColor = effectiveAccentColor,
                            inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
                            thumbColor = effectiveAccentColor,
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
                                contentDescription = strings.shuffle,
                                tint = if (shuffleOn) effectiveAccentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                onPrevious()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = strings.previousTrack,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        WavyPlayPauseButton(
                            isPlaying = isPlaying,
                            onClick = onTogglePlay,
                            size = 76.dp,
                            containerColor = effectiveAccentColor,
                            contentColor = playButtonContentColor
                        )

                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                onNext()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = strings.nextTrack,
                                modifier = Modifier.size(34.dp)
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
                                contentDescription = strings.repeatMode,
                                tint = if (repeatMode != RepeatMode.OFF) {
                                    effectiveAccentColor
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                showSleepDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bedtime,
                                contentDescription = strings.sleepTimerDialogTitle,
                                tint = if (sleepTimerRemainingSec != null) effectiveAccentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }

                        val isOnlineTrack = song.isRemoteYt || song.isRemoteSc || song.id.startsWith("sc_") || song.uri.startsWith("http") || song.uri.startsWith("yt://") || song.uri.startsWith("sc://")
                        if (isOnlineTrack) {
                            if (onDownloadTrack != null) {
                                IconButton(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                        onDownloadTrack()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = strings.download,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    onOpenTagEditor()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = strings.editTags,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                )
                            }
                        }

                        if (shareUrl != null) {
                            IconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    if (scShareUrl != null) {
                                        SoundCloudRepository.shareSongLink(context, song)
                                    } else {
                                        YouTubeMusicRepository.shareSongLink(context, song)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = strings.share,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                showQueueSheet = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = strings.queueTitle,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    LiveBottomLyricBanner(
                        activeLine = activeLyricLine,
                        isLoadingLyrics = isLoadingLyrics,
                        isPlaying = isPlaying,
                        accentColor = effectiveAccentColor,
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
                    text = "${strings.queueTitle} (${queue.size})",
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
                                    contentDescription = strings.removeFromQueue,
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
            title = { Text(strings.sleepTimerDialogTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        null to strings.sleepTimerOff,
                        5 to strings.minutesFormat.format(5),
                        15 to strings.minutesFormat.format(15),
                        30 to strings.minutesFormat.format(30),
                        60 to strings.minutesFormat.format(60)
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
                    Text(strings.close)
                }
            }
        )
    }

    if (showAddToPlaylistSheet) {
        AddToPlaylistOrFavoritesSheet(
            song = song,
            isFavorite = isFavorite,
            playlists = playlists,
            onToggleFavorite = onToggleFavorite,
            onAddToPlaylist = onAddToPlaylist,
            onRemoveFromPlaylist = onRemoveFromPlaylist,
            onCreatePlaylist = onCreatePlaylist,
            onDismiss = { showAddToPlaylistSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToPlaylistOrFavoritesSheet(
    song: Song,
    isFavorite: Boolean,
    playlists: List<Playlist>,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: (String, Song) -> Unit,
    onRemoveFromPlaylist: (String, String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SongArtwork(
                    seed = song.coverColorSeed,
                    albumArtUri = song.albumArtUri,
                    songUri = song.uri,
                    cornerRadius = 14.dp,
                    modifier = Modifier.size(52.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))

            ListItem(
                headlineContent = { Text(strings.favorites) },
                supportingContent = {
                    Text(if (isFavorite) strings.inFavorites else strings.addToFavorites)
                },
                leadingContent = {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                trailingContent = {
                    if (isFavorite) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier.clickable {
                    onToggleFavorite()
                }
            )

            val unifiedPl = playlists.find { it.id == "sys_unified_favorites" }
            val isInUnified = unifiedPl?.songs?.any { it.id == song.id } == true
            ListItem(
                headlineContent = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(strings.unifiedFavorites)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = strings.unifiedPlaylist,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                supportingContent = {
                    Text(if (isInUnified) strings.inFavorites else strings.addToPlaylist)
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = if (isInUnified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                trailingContent = {
                    if (isInUnified) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier.clickable {
                    if (isInUnified) {
                        onRemoveFromPlaylist("sys_unified_favorites", song.id)
                    } else {
                        onAddToPlaylist("sys_unified_favorites", song)
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.navPlaylists,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { showCreateDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(strings.newPlaylist)
                }
            }

            val userPlaylists = remember(playlists) { playlists.filter { !it.isSystem } }
            if (userPlaylists.isEmpty()) {
                Text(
                    text = strings.emptyPlaylistHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                    items(userPlaylists, key = { it.id }) { pl ->
                        val containsSong = pl.songs.any { it.id == song.id }
                        ListItem(
                            headlineContent = { Text(pl.name) },
                            supportingContent = { Text("${pl.songs.size} ${strings.tracksCount}") },
                            leadingContent = {
                                SongArtwork(
                                    seed = pl.coverColorSeed,
                                    cornerRadius = 10.dp,
                                    modifier = Modifier.size(40.dp)
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = if (containsSong) Icons.Filled.CheckCircle else Icons.Filled.AddCircleOutline,
                                    contentDescription = null,
                                    tint = if (containsSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            },
                            modifier = Modifier.clickable {
                                if (containsSong) {
                                    onRemoveFromPlaylist(pl.id, song.id)
                                } else {
                                    onAddToPlaylist(pl.id, song)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newPlaylistName = ""
            },
            title = { Text(strings.newPlaylistDialogTitle) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text(strings.playlistNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newPlaylistName.trim()
                        if (name.isNotBlank()) {
                            onCreatePlaylist(name)
                        }
                        showCreateDialog = false
                        newPlaylistName = ""
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        newPlaylistName = ""
                    }
                ) {
                    Text(strings.cancel)
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
    val strings = LocalStrings.current
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
                            text = strings.loadingLyrics,
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
                            text = strings.tapToOpenLyrics,
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
    onTranslate: (() -> Unit)? = null,
    onBackToCover: () -> Unit
) {
    var displayMode by remember { mutableStateOf(LyricsDisplayMode.SYNCED) }
    val strings = LocalStrings.current
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
                        contentDescription = strings.previousTrack,
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
                        contentDescription = strings.nextTrack,
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
                                    text = strings.searchingLyrics,
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
                                    text = strings.lyricsNotFound,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = strings.lyricsHint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.height(14.dp))
                                FilledTonalButton(onClick = onRefresh) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.retrySearch)
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
                        contentDescription = strings.refreshLyrics,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (onTranslate != null) {
                    val hasTranslations = lyrics.any { it.isTranslation }
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
                            .clickable { onTranslate() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Translate,
                            contentDescription = strings.translateLyrics,
                            tint = if (hasTranslations) accentColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                val syncedSelected = displayMode == LyricsDisplayMode.SYNCED
                val animatedBias by animateFloatAsState(
                    targetValue = if (syncedSelected) -1f else 1f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                    label = "lyricsPillBias"
                )

                Box(
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
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxHeight()
                            .align(BiasAlignment(animatedBias, 0f))
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                    )

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .clickable { displayMode = LyricsDisplayMode.SYNCED },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.syncedLyrics,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (syncedSelected) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .clickable { displayMode = LyricsDisplayMode.PLAIN },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.plainLyrics,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (!syncedSelected) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
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
            value = listOf(
                extracted.first,
                extracted.second,
                extracted.second.copy(alpha = 0.22f),
                surfaceColor
            )
        }
    }.value
}

private val coverGradientsCache = LruCache<String, Pair<Color, Color>>(100)
val coverAccentCache = LruCache<String, Color>(100)

fun getContrastingContentColor(backgroundColor: Color): Color {
    val luminance = 0.299f * backgroundColor.red + 0.587f * backgroundColor.green + 0.114f * backgroundColor.blue
    return if (luminance > 0.52f) Color.Black else Color.White
}

fun ensureReadableAccent(color: Color, minLuminance: Float = 0.52f): Color {
    val lum = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    if (lum >= minLuminance) return color
    val factor = ((minLuminance - lum) / (1f - lum.coerceAtMost(0.9f))).coerceIn(0f, 0.85f)
    return Color(
        red = (color.red + (1f - color.red) * factor).coerceIn(0f, 1f),
        green = (color.green + (1f - color.green) * factor).coerceIn(0f, 1f),
        blue = (color.blue + (1f - color.blue) * factor).coerceIn(0f, 1f),
        alpha = 1.0f
    )
}

private fun extractSongGradientColors(
    context: android.content.Context,
    song: Song,
    fallback: Color
): Pair<Color, Color> {
    val cacheKey = song.albumArtUri?.takeIf { it.isNotBlank() } ?: song.id
    coverGradientsCache.get(cacheKey)?.let { return it }

    val art = song.albumArtUri
    if (!art.isNullOrBlank()) {
        try {
            val bmp = if (art.startsWith("content://") || art.startsWith("file://")) {
                val stream = context.contentResolver.openInputStream(android.net.Uri.parse(art))
                val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 16 }
                val b = android.graphics.BitmapFactory.decodeStream(stream, null, opts)
                stream?.close()
                b
            } else if (art.startsWith("http://") || art.startsWith("https://")) {
                val conn = java.net.URL(art).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 2500
                conn.readTimeout = 2500
                val stream = conn.inputStream
                val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 8 }
                val b = android.graphics.BitmapFactory.decodeStream(stream, null, opts)
                stream.close()
                conn.disconnect()
                b
            } else null

            if (bmp != null) {
                val p1 = bmp.getPixel(bmp.width / 4, bmp.height / 4)
                val p2 = bmp.getPixel((bmp.width * 3) / 4, (bmp.height * 3) / 4)
                bmp.recycle()
                val color1 = Color(p1)
                val color2 = Color(p2)
                val pair = Pair(
                    color1.copy(alpha = 0.55f),
                    color2.copy(alpha = 0.40f)
                )
                coverGradientsCache.put(cacheKey, pair)
                coverAccentCache.put(song.id, color1.copy(alpha = 1.0f))
                return pair
            }
        } catch (_: Exception) {
        }
    }
    val hash = kotlin.math.abs((song.title + song.artist).hashCode())
    val hue1 = (hash % 360).toFloat()
    val hue2 = ((hash + 130) % 360).toFloat()
    val c1 = Color.hsv(hue1, 0.70f, 0.50f)
    val c2 = Color.hsv(hue2, 0.60f, 0.40f)
    val pair = Pair(
        c1.copy(alpha = 0.50f),
        c2.copy(alpha = 0.40f)
    )
    coverGradientsCache.put(cacheKey, pair)
    coverAccentCache.put(song.id, c1.copy(alpha = 1.0f))
    return pair
}

@Composable
fun HalloweenCoverCandies(
    onEatAllCandies: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val eatenStates = remember { mutableStateListOf(false, false, false, false, false, false) }
    val initialCandies = remember {
        listOf(
            Triple("🍬", -18f, 0.95f),
            Triple("🍭", 12f, 1.05f),
            Triple("🍫", -8f, 0.90f),
            Triple("🎃", 16f, 1.10f),
            Triple("🍬", -14f, 0.95f),
            Triple("🍭", 8f, 1.0f)
        )
    }

    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        initialCandies.forEachIndexed { index, (emoji, rot, sc) ->
            val isEaten = eatenStates.getOrNull(index) == true
            val animScale by animateFloatAsState(
                targetValue = if (isEaten) 0f else sc,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "candyScale$index"
            )
            val animAlpha by animateFloatAsState(
                targetValue = if (isEaten) 0f else 1f,
                animationSpec = tween(200),
                label = "candyAlpha$index"
            )

            if (!isEaten || animScale > 0.05f) {
                Box(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            eatenStates[index] = true
                            if (eatenStates.all { it }) {
                                coroutineScope.launch {
                                    delay(280)
                                    onEatAllCandies()
                                }
                            }
                        }
                        .graphicsLayer {
                            rotationZ = rot
                            scaleX = animScale
                            scaleY = animScale
                            alpha = animAlpha
                            shadowElevation = 8f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 24.sp)
                }
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }
        }
    }
}

private fun getDynamicPlaybackContextLabels(
    song: Song,
    playbackContext: PlaybackContext?,
    strings: AppStrings
): Pair<String, String> {
    val type = playbackContext?.type
    val title = playbackContext?.title?.trim()

    return when {
        type == PlaybackContextType.MY_WAVE -> {
            strings.fromMyWave to (title?.takeIf { it.isNotBlank() } ?: song.artist)
        }
        type == PlaybackContextType.RADIO -> {
            strings.fromRadio to (title?.takeIf { it.isNotBlank() } ?: strings.radioMixesTitle)
        }
        type == PlaybackContextType.PLAYLIST -> {
            strings.fromPlaylist to (title?.takeIf { it.isNotBlank() } ?: (song.folderName ?: "LorMusic"))
        }
        type == PlaybackContextType.FAVORITES -> {
            strings.fromFavorites to (title?.takeIf { it.isNotBlank() } ?: song.artist)
        }
        type == PlaybackContextType.ALBUM -> {
            val albumName = title?.takeIf { it.isNotBlank() } ?: song.album.takeIf {
                it.isNotBlank() && !it.equals("LorMusic", ignoreCase = true) && !it.equals("SoundCloud", ignoreCase = true)
            } ?: song.title
            strings.fromAlbum to albumName
        }
        type == PlaybackContextType.SINGLE -> {
            strings.singleTrack to (title?.takeIf { it.isNotBlank() } ?: song.title)
        }
        else -> {
            val album = song.album.trim()
            val isKnownAlbum = album.isNotBlank() &&
                !album.equals("LorMusic", ignoreCase = true) &&
                !album.equals("SoundCloud", ignoreCase = true) &&
                !album.equals("Unknown Album", ignoreCase = true) &&
                !album.equals("<unknown>", ignoreCase = true)
            if (isKnownAlbum) {
                if (album.contains("Single", ignoreCase = true) || song.title.contains(" - Single", ignoreCase = true)) {
                    strings.singleTrack to song.title
                } else {
                    strings.fromAlbum to album
                }
            } else {
                strings.singleTrack to song.title
            }
        }
    }
}
