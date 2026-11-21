package com.lormusic.app.data

object GlassStrength {
    const val DEFAULT = 15
    const val DEFAULT_TRANSPARENCY = 100 - DEFAULT

    fun resolve(savedStrength: Int?, legacyTransparency: Int?): Int =
        (savedStrength ?: legacyTransparency?.let { 100 - it } ?: DEFAULT).coerceIn(0, 100)

    fun transparency(strength: Int) = 100 - strength.coerceIn(0, 100)
}
