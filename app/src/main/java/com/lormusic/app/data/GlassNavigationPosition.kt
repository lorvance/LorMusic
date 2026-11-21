package com.lormusic.app.data

import kotlin.math.roundToInt

object GlassNavigationPosition {
    fun finger(x: Float, tabWidth: Float, count: Int, rtl: Boolean): Float {
        if (count <= 1 || !x.isFinite() || !tabWidth.isFinite() || tabWidth <= 0f) return 0f
        val position = x / tabWidth - .5f
        return (if (rtl) count - 1f - position else position).coerceIn(0f, count - 1f)
    }

    fun target(position: Float, count: Int): Int =
        if (count <= 1 || !position.isFinite()) 0 else position.roundToInt().coerceIn(0, count - 1)
}
