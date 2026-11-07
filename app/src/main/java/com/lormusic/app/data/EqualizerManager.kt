package com.lormusic.app.data

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer

data class EqualizerState(
    val enabled: Boolean = false,
    val presetIndex: Int = 0,
    val bandLevels: List<Int> = listOf(0, 0, 0, 0, 0),
    val bassBoost: Int = 0,
    val virtualizer: Int = 0
)

data class EqualizerPreset(
    val name: String,
    val bandLevels: List<Int>
)

object EqualizerManager {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var currentSessionId: Int = 0

    val presets = listOf(
        EqualizerPreset("Обычный", listOf(0, 0, 0, 0, 0)),
        EqualizerPreset("Бас", listOf(600, 400, 100, 0, 0)),
        EqualizerPreset("Рок", listOf(400, 200, -100, 200, 400)),
        EqualizerPreset("Поп", listOf(-100, 200, 400, 200, -100)),
        EqualizerPreset("Джаз", listOf(300, 100, 0, 200, 300)),
        EqualizerPreset("Классика", listOf(400, 300, 0, 200, 300)),
        EqualizerPreset("Электронная", listOf(500, 300, 0, 200, 400)),
        EqualizerPreset("Вокал", listOf(-200, 100, 400, 300, 0))
    )

    fun initAudioSession(sessionId: Int, prefs: PreferencesRepository) {
        if (sessionId <= 0) return
        if (currentSessionId == sessionId && equalizer != null) return
        release()
        currentSessionId = sessionId
        val state = loadState(prefs)
        try {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = state.enabled
                applyLevels(this, state.bandLevels)
            }
        } catch (_: Exception) {}

        try {
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = state.enabled && state.bassBoost > 0
                if (strengthSupported) {
                    setStrength(state.bassBoost.toShort())
                }
            }
        } catch (_: Exception) {}

        try {
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = state.enabled && state.virtualizer > 0
                if (strengthSupported) {
                    setStrength(state.virtualizer.toShort())
                }
            }
        } catch (_: Exception) {}
    }

    private fun applyLevels(eq: Equalizer, levels: List<Int>) {
        try {
            val numBands = eq.numberOfBands.toInt()
            val minLevel = eq.bandLevelRange.getOrNull(0)?.toInt() ?: -1200
            val maxLevel = eq.bandLevelRange.getOrNull(1)?.toInt() ?: 1200
            levels.forEachIndexed { band, lvl ->
                if (band < numBands) {
                    val clamped = lvl.coerceIn(minLevel, maxLevel).toShort()
                    eq.setBandLevel(band.toShort(), clamped)
                }
            }
        } catch (_: Exception) {}
    }

    fun setEnabled(enabled: Boolean, prefs: PreferencesRepository) {
        prefs.saveEqualizerEnabled(enabled)
        try {
            equalizer?.enabled = enabled
            val state = loadState(prefs)
            bassBoost?.enabled = enabled && state.bassBoost > 0
            virtualizer?.enabled = enabled && state.virtualizer > 0
        } catch (_: Exception) {}
    }

    fun setBandLevel(band: Int, level: Int, prefs: PreferencesRepository) {
        val state = loadState(prefs)
        val newLevels = state.bandLevels.toMutableList()
        if (band in newLevels.indices) {
            newLevels[band] = level
        }
        prefs.saveEqualizerBands(newLevels)
        prefs.saveEqualizerPreset(-1)
        try {
            equalizer?.let { applyLevels(it, newLevels) }
        } catch (_: Exception) {}
    }

    fun applyPreset(presetIndex: Int, prefs: PreferencesRepository) {
        val preset = presets.getOrNull(presetIndex) ?: return
        prefs.saveEqualizerPreset(presetIndex)
        prefs.saveEqualizerBands(preset.bandLevels)
        try {
            equalizer?.let { applyLevels(it, preset.bandLevels) }
        } catch (_: Exception) {}
    }

    fun setBassBoost(strength: Int, prefs: PreferencesRepository) {
        val clamped = strength.coerceIn(0, 1000)
        prefs.saveEqualizerBass(clamped)
        try {
            val state = loadState(prefs)
            bassBoost?.let {
                it.enabled = state.enabled && clamped > 0
                if (it.strengthSupported) {
                    it.setStrength(clamped.toShort())
                }
            }
        } catch (_: Exception) {}
    }

    fun setVirtualizer(strength: Int, prefs: PreferencesRepository) {
        val clamped = strength.coerceIn(0, 1000)
        prefs.saveEqualizerVirtualizer(clamped)
        try {
            val state = loadState(prefs)
            virtualizer?.let {
                it.enabled = state.enabled && clamped > 0
                if (it.strengthSupported) {
                    it.setStrength(clamped.toShort())
                }
            }
        } catch (_: Exception) {}
    }

    fun reset(prefs: PreferencesRepository) {
        val flat = listOf(0, 0, 0, 0, 0)
        prefs.saveEqualizerBands(flat)
        prefs.saveEqualizerPreset(0)
        prefs.saveEqualizerBass(0)
        prefs.saveEqualizerVirtualizer(0)
        try {
            equalizer?.let { applyLevels(it, flat) }
            bassBoost?.setStrength(0)
            virtualizer?.setStrength(0)
        } catch (_: Exception) {}
    }

    fun loadState(prefs: PreferencesRepository): EqualizerState {
        return EqualizerState(
            enabled = prefs.getEqualizerEnabled(),
            presetIndex = prefs.getEqualizerPreset(),
            bandLevels = prefs.getEqualizerBands(),
            bassBoost = prefs.getEqualizerBass(),
            virtualizer = prefs.getEqualizerVirtualizer()
        )
    }

    fun release() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        currentSessionId = 0
    }
}
