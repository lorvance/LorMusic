package com.lormusic.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale

/** Original vector interpretation of the six supplied wave-cover references. */
@Composable
fun DefaultSongCover(seed: Int, modifier: Modifier = Modifier, showNote: Boolean = true) {
    val palettes =
        listOf(
            0xFF381A17L to 0xFFF2C993L,
            0xFF09152CL to 0xFF78ACF9L,
            0xFF202027L to 0xFFFFD857L,
            0xFF2C1248L to 0xFFB59BF1L,
            0xFF4C001CL to 0xFFF76C7AL,
            0xFF00392FL to 0xFF8CE0C3L,
        )
    val (dark, bright) = palettes[Math.floorMod(seed, palettes.size)]
    Canvas(modifier) {
        scale(
            size.width / 100f,
            size.height / 100f,
            pivot = androidx.compose.ui.geometry.Offset.Zero,
        ) {
            drawRect(Color(dark), size = androidx.compose.ui.geometry.Size(100f, 100f))
            val top =
                Path().apply {
                    moveTo(0f, 0f)
                    lineTo(66f, 0f)
                    cubicTo(61f, 17f, 53f, 22f, 36f, 23f)
                    cubicTo(17f, 25f, 12f, 39f, 0f, 49f)
                    close()
                }
            val bottom =
                Path().apply {
                    moveTo(29f, 100f)
                    cubicTo(36f, 85f, 45f, 82f, 63f, 81f)
                    cubicTo(84f, 79f, 83f, 62f, 100f, 57f)
                    lineTo(100f, 100f)
                    close()
                }
            drawPath(top, Color(bright))
            drawPath(bottom, Color(bright))
            if (showNote) {
                val note =
                    Path().apply {
                        moveTo(50f, 37f)
                        cubicTo(50f, 31f, 54f, 29f, 59f, 29f)
                        lineTo(63f, 29f)
                        cubicTo(74f, 29f, 74f, 40f, 64f, 40f)
                        lineTo(59f, 40f)
                        cubicTo(57f, 40f, 56f, 41f, 56f, 44f)
                        lineTo(56f, 64f)
                        cubicTo(56f, 75f, 44f, 80f, 36f, 74f)
                        cubicTo(27f, 68f, 30f, 56f, 40f, 54f)
                        cubicTo(44f, 53f, 47f, 54f, 50f, 56f)
                        close()
                    }
                drawPath(note, Color(0xFFFFFAF3))
            }
        }
    }
}
