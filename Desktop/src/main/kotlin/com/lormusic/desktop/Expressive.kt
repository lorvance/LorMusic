@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lormusic.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.*
import com.lormusic.app.model.Song
import java.io.File
import java.net.URI
import kotlinx.coroutines.*
import org.jetbrains.skia.Image

@Composable
fun BrandIcon(size: Dp) {
    val image = remember {
        Image.makeFromEncoded(
                object {}.javaClass.getResourceAsStream("/lormusic.png")!!.readBytes()
            )
            .toComposeImageBitmap()
    }
    Image(BitmapPainter(image), "LorMusic", Modifier.size(size))
}

@Composable
fun ExpressiveWaves(modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path =
            Path().apply {
                moveTo(w * .15f, h)
                cubicTo(-w * .2f, h * .3f, w * .8f, h * .8f, w * .55f, 0f)
                lineTo(w, 0f)
                lineTo(w, h)
                close()
            }
        drawPath(path, Brush.linearGradient(listOf(c.primary, Color(0xFF4C63F4))))
        val path2 =
            Path().apply {
                moveTo(w * .55f, h)
                cubicTo(w * .25f, h * .3f, w * .85f, h * .6f, w, 0f)
                lineTo(w, h)
                close()
            }
        drawPath(path2, Brush.linearGradient(listOf(Color(0xFFABCFFF), c.primary)))
    }
}

@Composable
fun ExpressiveBackdrop(modifier: Modifier) {
    Box(modifier) {
        ExpressiveWaves(Modifier.align(Alignment.BottomEnd).width(520.dp).height(180.dp))
    }
}

@Composable
fun PreferenceSwitch(label: String, value: Boolean, change: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(t(label), Modifier.weight(1f))
        Switch(value, change)
    }
}

@Composable
fun ArtworkAtmosphere(song: Song?, enabled: Boolean, onColor: (Color) -> Unit) {
    val fallback = MaterialTheme.colorScheme.primary
    val art by
        produceState<Pair<ImageBitmap, Color>?>(null, song?.albumArtUri) {
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                            val path = song?.albumArtUri ?: return@runCatching null
                            val url =
                                if (path.startsWith("http")) URI(path).toURL()
                                else
                                    File(if (path.startsWith("file:")) URI(path).path else path)
                                        .toURI()
                                        .toURL()
                            val connection =
                                url.openConnection().apply {
                                    connectTimeout = 4000
                                    readTimeout = 4000
                                }
                            val bytes = connection.getInputStream().use { it.readBytes() }
                            val image =
                                javax.imageio.ImageIO.read(bytes.inputStream())
                                    ?: return@runCatching null
                            var best = fallback
                            var score = 0f
                            for (x in 0 until image.width step maxOf(1, image.width / 16)) for (y in
                                0 until image.height step maxOf(1, image.height / 16)) {
                                val rgb = java.awt.Color(image.getRGB(x, y))
                                val hsv =
                                    java.awt.Color.RGBtoHSB(rgb.red, rgb.green, rgb.blue, null)
                                val weight = hsv[1] * hsv[2]
                                if (weight > score && hsv[2] > .28f) {
                                    score = weight
                                    best = Color.hsv(hsv[0] * 360f, hsv[1].coerceIn(.25f, .5f), .9f)
                                }
                            }
                            Pair(Image.makeFromEncoded(bytes).toComposeImageBitmap(), best)
                        }
                        .getOrNull()
                }
        }
    LaunchedEffect(art) { onColor(art?.second ?: fallback) }
    if (enabled && art != null)
        androidx.compose.foundation.Image(
            art!!.first,
            null,
            Modifier.fillMaxSize()
                .then(Modifier.blur(90.dp))
                .then(Modifier.graphicsLayer(alpha = .16f)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
}
