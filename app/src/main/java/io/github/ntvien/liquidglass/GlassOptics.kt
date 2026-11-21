package io.github.ntvien.liquidglass

import kotlin.math.abs
import kotlin.math.ceil

internal fun isCircularGlassBounds(
    width: Float,
    height: Float,
    topStart: Float,
    topEnd: Float,
    bottomEnd: Float,
    bottomStart: Float,
): Boolean {
    val diameter = minOf(width, height)
    if (!diameter.isFinite() || diameter <= 1f || abs(width - height) > 1f) return false
    // Layout rounds dp to pixels, while a fixed corner radius retains fractional pixels.
    val minRadius = (diameter - 1f) / 2f
    return minOf(topStart, topEnd, bottomEnd, bottomStart) >= minRadius
}

internal data class GlassOptics(val blur: Float, val height: Float, val amount: Float, val padding: Float) {
    companion object {
        fun forSize(
            shortSide: Float,
            requestedBlur: Float,
            requestedHeight: Float,
            requestedAmount: Float,
            dispersion: Boolean,
        ): GlassOptics {
            if (!shortSide.isFinite() || shortSide <= 0f) return GlassOptics(0f, 0f, 0f, 0f)

            val blur = requestedBlur.coerceIn(0f, shortSide * .55f)
            val height = requestedHeight.coerceIn(0f, shortSide * .45f)
            val amount = requestedAmount.coerceIn(0f, shortSide * .38f)
            val padding = if (height > 0f && amount > 0f) {
                // Capture pixels past the lens so displaced samples stay clear of the recorded edge.
                // Chromatic dispersion can double the offset.
                ceil(blur + amount * (if (dispersion) 2f else 1f) + 1f)
            } else {
                0f
            }
            return GlassOptics(blur, height, amount, padding)
        }
    }
}
