package com.lormusic.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import kotlin.math.abs
import kotlin.math.max

private val MinimalistSurfaceTints = listOf(
    Color(0xFF1F1A30),
    Color(0xFF1B1D30),
    Color(0xFF251B2E),
    Color(0xFF1B232B),
    Color(0xFF221D34),
    Color(0xFF1E2033),
    Color(0xFF271D2B)
)

private val MinimalistAccentTints = listOf(
    Color(0xFFB9A2FF),
    Color(0xFFE5B6F7),
    Color(0xFF8CE7DF),
    Color(0xFF9FB8FF),
    Color(0xFFD0A8FF),
    Color(0xFF88D9E6),
    Color(0xFFFFB2D1)
)

@Composable
fun rememberSmoothFlingBehavior(): FlingBehavior {
    val decaySpec = remember {
        exponentialDecay<Float>(
            frictionMultiplier = 0.72f,
            absVelocityThreshold = 0.5f
        )
    }
    return remember(decaySpec) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                if (abs(initialVelocity) <= 1f) return initialVelocity
                var lastValue = 0f
                var velocityLeft = initialVelocity * 1.08f
                AnimationState(
                    initialValue = 0f,
                    initialVelocity = velocityLeft
                ).animateDecay(decaySpec) {
                    val delta = value - lastValue
                    val consumed = scrollBy(delta)
                    lastValue = value
                    velocityLeft = this.velocity
                    if (abs(delta - consumed) > 0.5f) {
                        this.cancelAnimation()
                    }
                }
                return velocityLeft
            }
        }
    }
}

object ArtworkCache {
    private val bitmapCache = LruCache<String, ImageBitmap>(260)
    private val noCoverKeys: MutableSet<String> = Collections.synchronizedSet(HashSet())

    fun getBitmap(key: String): ImageBitmap? = bitmapCache.get(key)
    fun hasNoCover(key: String): Boolean = noCoverKeys.contains(key)

    fun putBitmap(key: String, bmp: ImageBitmap?) {
        if (bmp != null) {
            bitmapCache.put(key, bmp)
            noCoverKeys.remove(key)
        } else {
            noCoverKeys.add(key)
        }
    }

    fun clearAll() {
        bitmapCache.evictAll()
        noCoverKeys.clear()
    }
}

private fun scaleDownBitmapIfNeeded(source: Bitmap, maxDimPx: Int = 420): Bitmap {
    val w = source.width
    val h = source.height
    val longest = max(w, h)
    if (longest <= maxDimPx || longest <= 0) return source
    val ratio = maxDimPx.toFloat() / longest.toFloat()
    val targetW = (w * ratio).toInt().coerceAtLeast(1)
    val targetH = (h * ratio).toInt().coerceAtLeast(1)
    return runCatching {
        Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }.getOrDefault(source)
}

suspend fun loadSongBitmap(
    context: Context,
    cacheKey: String,
    albumArtUri: String?,
    songUri: String?
): ImageBitmap? = withContext(Dispatchers.IO) {
    ArtworkCache.getBitmap(cacheKey)?.let { return@withContext it }
    if (ArtworkCache.hasNoCover(cacheKey)) return@withContext null

    var rawBitmap: Bitmap? = null

    if (!albumArtUri.isNullOrBlank() && (
            albumArtUri.startsWith("http://") ||
                albumArtUri.startsWith("https://") ||
                albumArtUri.startsWith("file://") ||
                albumArtUri.startsWith("/")
            )
    ) {
        rawBitmap = runCatching {
            when {
                albumArtUri.startsWith("http://") || albumArtUri.startsWith("https://") -> {
                    val conn = (URL(albumArtUri).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 5000
                        readTimeout = 5000
                        setRequestProperty("User-Agent", "LorMusic/1.15.3")
                    }
                    conn.inputStream.use { BitmapFactory.decodeStream(it) }
                }
                albumArtUri.startsWith("file://") -> {
                    val path = Uri.parse(albumArtUri).path ?: albumArtUri.removePrefix("file://")
                    BitmapFactory.decodeFile(path)
                }
                else -> BitmapFactory.decodeFile(File(albumArtUri).absolutePath)
            }
        }.getOrNull()
    }

    if (rawBitmap == null && !songUri.isNullOrBlank() && (songUri.startsWith("content://") || songUri.startsWith("file://"))) {
        rawBitmap = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(songUri))
                retriever.embeddedPicture?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrNull()
    }

    if (rawBitmap == null && !albumArtUri.isNullOrBlank() && albumArtUri.startsWith("content://")) {
        rawBitmap = runCatching {
            context.contentResolver.openInputStream(Uri.parse(albumArtUri))?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

    val imgBmp = rawBitmap?.let { scaleDownBitmapIfNeeded(it, 420).asImageBitmap() }
    ArtworkCache.putBitmap(cacheKey, imgBmp)
    imgBmp
}

@Composable
fun SongArtwork(
    seed: Int,
    modifier: Modifier = Modifier,
    albumArtUri: String? = null,
    songUri: String? = null,
    cornerRadius: Dp = 16.dp,
    showEqualizerOverlay: Boolean = false,
    isPlaying: Boolean = false,
    showCenterIcon: Boolean = true
) {
    val context = LocalContext.current
    val cacheKey = remember(songUri, albumArtUri, seed) {
        "art_${songUri.orEmpty()}_${albumArtUri.orEmpty()}_$seed"
    }

    var loadedBitmap by remember(cacheKey) { mutableStateOf(ArtworkCache.getBitmap(cacheKey)) }
    val surfaceColor = remember(seed) { MinimalistSurfaceTints[abs(seed) % MinimalistSurfaceTints.size] }
    val subtleAccent = remember(seed) { MinimalistAccentTints[abs(seed) % MinimalistAccentTints.size] }
    val primaryAccent = MaterialTheme.colorScheme.primary

    LaunchedEffect(cacheKey) {
        if (loadedBitmap == null && !ArtworkCache.hasNoCover(cacheKey)) {
            loadedBitmap = loadSongBitmap(context, cacheKey, albumArtUri, songUri)
        }
    }

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        surfaceColor,
                        surfaceColor.copy(alpha = 0.90f)
                    )
                )
            )
            .border(
                width = 0.8.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        val bmp = loadedBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val minDim = size.minDimension
                    drawCircle(
                        color = subtleAccent.copy(alpha = 0.08f),
                        radius = minDim * 0.34f,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = minDim * 0.34f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                if (showCenterIcon && !showEqualizerOverlay) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = subtleAccent.copy(alpha = 0.82f),
                        modifier = Modifier.fillMaxSize(0.38f)
                    )
                }
            }
        }

        if (showEqualizerOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.48f)),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    AnimatedEqualizerBars(
                        modifier = Modifier.fillMaxSize(0.42f),
                        color = primaryAccent
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = primaryAccent,
                        modifier = Modifier.fillMaxSize(0.44f)
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedEqualizerBars(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    barCount: Int = 4
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val heights = List(barCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.22f,
            targetValue = 0.96f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 360 + index * 110,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { animHeight ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(animHeight.value)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}
