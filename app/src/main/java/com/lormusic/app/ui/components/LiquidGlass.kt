package com.lormusic.app.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun isLiquidGlassSupported(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

fun Modifier.liquidGlassSurface(
    shape: Shape = RoundedCornerShape(24.dp),
    isLiquidGlass: Boolean = false,
    borderWidth: Dp = 1.2.dp,
    tintColor: Color = Color.Unspecified
): Modifier {
    if (!isLiquidGlass || !isLiquidGlassSupported()) {
        return this
    }

    val glassBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.65f),
            Color.White.copy(alpha = 0.22f),
            Color.White.copy(alpha = 0.05f),
            Color.White.copy(alpha = 0.38f)
        )
    )

    val effectiveTint = if (tintColor != Color.Unspecified) {
        tintColor.copy(alpha = 0.12f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }

    val glassSheenBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.18f),
            effectiveTint,
            Color.Black.copy(alpha = 0.10f)
        )
    )

    return this
        .clip(shape)
        .background(glassSheenBrush)
        .border(width = borderWidth, brush = glassBorderBrush, shape = shape)
}
