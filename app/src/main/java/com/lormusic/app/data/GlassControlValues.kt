package com.lormusic.app.data

object GlassControlValues {
    fun range(range: ClosedFloatingPointRange<Float>): ClosedFloatingPointRange<Float> {
        val start = range.start.takeIf { it.isFinite() } ?: 0f
        val end = range.endInclusive.takeIf { it.isFinite() && it > start } ?: start + 1f
        return start..end
    }

    fun value(value: Float, range: ClosedFloatingPointRange<Float>, steps: Int): Float {
        val safe = GlassControlValues.range(range)
        val v = if (value.isFinite()) value.coerceIn(safe) else safe.start
        if (steps <= 0) return v
        val step = (safe.endInclusive - safe.start) / (steps.toFloat() + 1f)
        return (safe.start + kotlin.math.round((v - safe.start) / step) * step).coerceIn(safe)
    }
}
