package com.lormusic.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lormusic.app.data.TagFixEngine
import com.lormusic.app.data.YouTubeMusicRepository
import com.lormusic.app.model.LocalStrings
import com.lormusic.app.model.Song
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@Composable
fun IosBackChevron(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Canvas(modifier = modifier.size(width = 11.dp, height = 18.dp)) {
        val strokeWidth = 2.5.dp.toPx()
        val path = Path().apply {
            moveTo(size.width * 0.85f, 0f)
            lineTo(size.width * 0.15f, size.height * 0.5f)
            lineTo(size.width * 0.85f, size.height)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
fun MiniEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val transition = rememberInfiniteTransition(label = "equalizerAnim")
    val h1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq1"
    )
    val h2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(340, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq2"
    )
    val h3 by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq3"
    )
    val h4 by transition.animateFloat(
        initialValue = 0.70f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq4"
    )

    val heights = if (isPlaying) listOf(h1, h2, h3, h4) else listOf(0.35f, 0.60f, 0.40f, 0.55f)

    Row(
        modifier = modifier.size(width = 20.dp, height = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { frac ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction = frac.coerceIn(0.18f, 1f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun ArtistProfileDialog(
    artistName: String,
    allArtistSongs: List<Song>,
    topTracks: List<Song>,
    albums: Map<String, List<Song>>,
    singlesAndEPs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    isFavorite: (String) -> Boolean,
    progress: Float = 0f,
    initialAvatarUrl: String? = null,
    enableBlur: Boolean = true,
    useLiquidGlass: Boolean = false,
    onDismiss: () -> Unit,
    onPlaySongAt: (List<Song>, Int) -> Unit,
    onPlayAll: (List<Song>, Boolean) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit,
    onTogglePlay: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onOpenPlayer: () -> Unit = {}
) {
    val strings = LocalStrings.current
    var avatarUrl by remember(artistName, initialAvatarUrl) { mutableStateOf(initialAvatarUrl) }
    var selectedAlbum by remember { mutableStateOf<Pair<String, List<Song>>?>(null) }
    val albumHazeState = remember { HazeState() }

    LaunchedEffect(artistName) {
        if (avatarUrl.isNullOrBlank()) {
            val remoteAvatar = TagFixEngine.fetchArtistAvatar(artistName)
            if (!remoteAvatar.isNullOrBlank()) {
                avatarUrl = remoteAvatar
            } else {
                avatarUrl = allArtistSongs.firstOrNull { !it.albumArtUri.isNullOrBlank() }?.albumArtUri
            }
        }
    }

    if (selectedAlbum == null) {
        BackHandler { onDismiss() }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = selectedAlbum,
                    transitionSpec = {
                        if (targetState != null) {
                            (slideInHorizontally(tween(260)) { it / 4 } + fadeIn(tween(220)))
                                .togetherWith(slideOutHorizontally(tween(220)) { -it / 4 } + fadeOut(tween(180)))
                        } else {
                            (slideInHorizontally(tween(260)) { -it / 4 } + fadeIn(tween(220)))
                                .togetherWith(slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(180)))
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "albumNav"
                ) { targetAlbum ->
                    if (targetAlbum != null) {
                        BackHandler { selectedAlbum = null }
                        val (albumName, albumSongs) = targetAlbum
                        var albumTrackList by remember(albumName, albumSongs) { mutableStateOf(albumSongs) }

                        LaunchedEffect(albumName) {
                            if (albumTrackList.size < 5) {
                                val extra = YouTubeMusicRepository.fetchAlbumTracks(artistName, albumName)
                                if (extra.isNotEmpty()) {
                                    val combined = (albumTrackList + extra).distinctBy { it.id }
                                    if (combined.size > albumTrackList.size) {
                                        albumTrackList = combined
                                    }
                                }
                            }
                        }

                        val isTrackFromThisAlbumPlaying = isPlaying && albumTrackList.any { it.id == currentSong?.id }

                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                                    .hazeSource(state = albumHazeState)
                                    .appBackdropSource()
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        top = 74.dp,
                                        bottom = if (currentSong != null) 180.dp else 40.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    item(key = "album_section_title") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = strings.tracksInAlbum,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "• ${albumTrackList.size}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                            if (isTrackFromThisAlbumPlaying) {
                                                Spacer(Modifier.weight(1f))
                                                MiniEqualizer(
                                                    isPlaying = true,
                                                    barColor = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    item(key = "album_action_buttons") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 18.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Play button
                                            Button(
                                                onClick = {
                                                    onPlayAll(albumTrackList, false)
                                                    onOpenPlayer()
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp),
                                                shape = RoundedCornerShape(24.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                                enabled = albumTrackList.isNotEmpty()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = strings.artistPlayAll,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                            }

                                            // Shuffle button
                                            OutlinedButton(
                                                onClick = {
                                                    onPlayAll(albumTrackList, true)
                                                    onOpenPlayer()
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp),
                                                shape = RoundedCornerShape(24.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                    contentColor = MaterialTheme.colorScheme.onSurface
                                                ),
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                                ),
                                                enabled = albumTrackList.isNotEmpty()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Shuffle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = strings.artistShuffle,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                            }
                                        }
                                    }

                                    itemsIndexed(albumTrackList, key = { idx, s -> "${s.id}_$idx" }) { index, song ->
                                        val isCurrent = currentSong?.id == song.id
                                        val isFav = isFavorite(song.id)

                                        val cardBackground = if (isCurrent) {
                                            Brush.horizontalGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                                )
                                            )
                                        } else {
                                            Brush.horizontalGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                                                )
                                            )
                                        }

                                        val cardBorderColor = if (isCurrent) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(cardBackground)
                                                .border(
                                                    width = if (isCurrent) 1.dp else 0.5.dp,
                                                    color = cardBorderColor,
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                                .clickable {
                                                    onPlaySongAt(albumTrackList, index)
                                                    onOpenPlayer()
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Track number or visualizer
                                                Box(
                                                    modifier = Modifier.width(34.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isCurrent) {
                                                        MiniEqualizer(
                                                            isPlaying = isPlaying,
                                                            barColor = MaterialTheme.colorScheme.primary
                                                        )
                                                    } else {
                                                        Text(
                                                            text = "${index + 1}",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }

                                                Spacer(Modifier.width(10.dp))

                                                // Title & Artist
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = song.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        text = song.artist.ifBlank { artistName },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                // Duration
                                                if (song.durationMs > 0) {
                                                    val min = song.durationMs / 1000 / 60
                                                    val sec = (song.durationMs / 1000) % 60
                                                    Text(
                                                        text = String.format("%d:%02d", min, sec),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                                        modifier = Modifier.padding(horizontal = 6.dp)
                                                    )
                                                }

                                                // Favorite button
                                                IconButton(
                                                    onClick = { onToggleFavorite(song) },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                                        contentDescription = null,
                                                        tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                }

                                                // More options button
                                                IconButton(
                                                    onClick = { onSongMoreClick(song) },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.MoreVert,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Top header bar with 3 bubbles & blur / vertical dimming
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                            ) {
                                val topScrimColor = MaterialTheme.colorScheme.background
                                if (enableBlur) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .hazeEffect(state = albumHazeState) {
                                                blurEnabled = true
                                                backgroundColor = topScrimColor
                                                blurRadius = 24.dp
                                                tints = listOf(HazeTint(topScrimColor.copy(alpha = 0.65f)))
                                            }
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        topScrimColor.copy(alpha = 0.85f),
                                                        topScrimColor.copy(alpha = 0.50f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        topScrimColor.copy(alpha = 0.96f),
                                                        topScrimColor.copy(alpha = 0.78f),
                                                        topScrimColor.copy(alpha = 0.42f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // 1. Bubble Left: Perfectly centered iOS Back Chevron
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .liquidGlassBubble(CircleShape, albumHazeState, enableBlur, useLiquidGlass)
                                            .clickable { selectedAlbum = null },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IosBackChevron(
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // 2. Bubble Center: Adaptive width Album title
                                    Box(
                                        modifier = Modifier
                                            .height(44.dp)
                                            .weight(1f, fill = false)
                                            .clip(RoundedCornerShape(22.dp))
                                            .liquidGlassBubble(RoundedCornerShape(22.dp), albumHazeState, enableBlur, useLiquidGlass)
                                            .padding(horizontal = 18.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = albumName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // 3. Bubble Right: Album cover avatar
                                    val firstTrack = albumTrackList.firstOrNull()
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .then(
                                                if (isTrackFromThisAlbumPlaying) {
                                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .liquidGlassBubble(CircleShape, albumHazeState, enableBlur, useLiquidGlass)
                                            .padding(3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SongArtwork(
                                            seed = firstTrack?.coverColorSeed ?: kotlin.math.abs(albumName.hashCode()),
                                            albumArtUri = firstTrack?.albumArtUri,
                                            songUri = firstTrack?.uri,
                                            cornerRadius = 22.dp,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                bottom = if (currentSong != null) 180.dp else 40.dp
                            )
                        ) {
                            item(key = "header") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(290.dp)
                                ) {
                                    SongArtwork(
                                        seed = kotlin.math.abs(artistName.hashCode()),
                                        albumArtUri = avatarUrl,
                                        cornerRadius = 0.dp,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.Black.copy(alpha = 0.35f),
                                                        Color.Black.copy(alpha = 0.55f),
                                                        MaterialTheme.colorScheme.background
                                                    )
                                                )
                                            )
                                    )

                                    IconButton(
                                        onClick = onDismiss,
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .align(Alignment.TopStart)
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.45f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                                    ) {
                                        Text(
                                            text = strings.artistProfile.uppercase(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = artistName,
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "${allArtistSongs.size} ${strings.statsTracks.lowercase()} • ${albums.size} ${strings.artistAlbums.lowercase()}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }

                            item(key = "action_buttons") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onPlayAll(allArtistSongs, false)
                                            onOpenPlayer()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp),
                                        shape = RoundedCornerShape(25.dp),
                                        enabled = allArtistSongs.isNotEmpty()
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(strings.artistPlayAll, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            onPlayAll(allArtistSongs, true)
                                            onOpenPlayer()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp),
                                        shape = RoundedCornerShape(25.dp),
                                        enabled = allArtistSongs.isNotEmpty()
                                    ) {
                                        Icon(Icons.Filled.Shuffle, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(strings.artistShuffle, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (topTracks.isNotEmpty()) {
                                item(key = "section_top_tracks_title") {
                                    Text(
                                        text = strings.artistTopTracks,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                                    )
                                }

                                item(key = "top_tracks_carousel") {
                                    val chunkedTracks = topTracks.take(20).chunked(5)
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        itemsIndexed(chunkedTracks, key = { colIdx, _ -> "top_col_$colIdx" }) { _, columnSongs ->
                                            Column(
                                                modifier = Modifier.width(310.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                columnSongs.forEach { song ->
                                                    val isCurrent = currentSong?.id == song.id
                                                    val isFav = isFavorite(song.id)
                                                    Surface(
                                                        shape = RoundedCornerShape(16.dp),
                                                        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                val idx = allArtistSongs.indexOfFirst { it.id == song.id }
                                                                if (idx >= 0) {
                                                                    onPlaySongAt(allArtistSongs, idx)
                                                                } else {
                                                                    onPlaySongAt(topTracks, topTracks.indexOfFirst { it.id == song.id }.coerceAtLeast(0))
                                                                }
                                                                onOpenPlayer()
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            SongArtwork(
                                                                seed = song.coverColorSeed,
                                                                albumArtUri = song.albumArtUri,
                                                                songUri = song.uri,
                                                                cornerRadius = 10.dp,
                                                                modifier = Modifier.size(46.dp)
                                                            )
                                                            Spacer(Modifier.width(10.dp))
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = song.title,
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    text = song.album.ifBlank { song.artist },
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = { onToggleFavorite(song) },
                                                                modifier = Modifier.size(34.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                                                    contentDescription = null,
                                                                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = { onSongMoreClick(song) },
                                                                modifier = Modifier.size(34.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.MoreVert,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (albums.isNotEmpty()) {
                                item(key = "section_albums_title") {
                                    Text(
                                        text = strings.artistAlbums,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 12.dp)
                                    )
                                }

                                item(key = "albums_carousel") {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(albums.entries.toList(), key = { "album_" + it.key }) { (albumName, albumSongs) ->
                                            val firstSong = albumSongs.firstOrNull()
                                            Column(
                                                modifier = Modifier
                                                    .width(140.dp)
                                                    .clickable { selectedAlbum = albumName to albumSongs }
                                            ) {
                                                if (firstSong != null) {
                                                    SongArtwork(
                                                        seed = firstSong.coverColorSeed,
                                                        albumArtUri = firstSong.albumArtUri,
                                                        songUri = firstSong.uri,
                                                        cornerRadius = 18.dp,
                                                        modifier = Modifier.size(140.dp)
                                                    )
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    text = albumName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${albumSongs.size} ${strings.statsTracks.lowercase()}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (singlesAndEPs.isNotEmpty()) {
                                item(key = "section_singles_title") {
                                    Text(
                                        text = strings.artistSinglesAndEPs,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 12.dp)
                                    )
                                }

                                item(key = "singles_carousel") {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(singlesAndEPs, key = { "single_card_" + it.id }) { song ->
                                            Column(
                                                modifier = Modifier
                                                    .width(140.dp)
                                                    .clickable {
                                                        val idx = allArtistSongs.indexOfFirst { it.id == song.id }
                                                        if (idx >= 0) {
                                                            onPlaySongAt(allArtistSongs, idx)
                                                        } else {
                                                            onPlaySongAt(listOf(song), 0)
                                                        }
                                                        onOpenPlayer()
                                                    }
                                            ) {
                                                SongArtwork(
                                                    seed = song.coverColorSeed,
                                                    albumArtUri = song.albumArtUri,
                                                    songUri = song.uri,
                                                    cornerRadius = 18.dp,
                                                    modifier = Modifier.size(140.dp)
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    text = song.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = strings.artistSinglesAndEPs,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // MiniPlayer fixed at the bottom with safe navigation insets and lifted margin
                AnimatedVisibility(
                    visible = currentSong != null,
                    enter = fadeIn(tween(220)) + slideInVertically { it / 2 },
                    exit = fadeOut(tween(180)) + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 12.dp)
                ) {
                    currentSong?.let { song ->
                        MiniPlayer(
                            song = song,
                            isPlaying = isPlaying,
                            isFavorite = isFavorite(song.id),
                            progress = progress,
                            hazeState = albumHazeState,
                            enableBlur = enableBlur,
                            useLiquidGlass = useLiquidGlass,
                            enableSwipe = true,
                            onTogglePlay = onTogglePlay,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onToggleFavorite = { onToggleFavorite(song) },
                            onOpen = onOpenPlayer
                        )
                    }
                }
            }
        }
}
