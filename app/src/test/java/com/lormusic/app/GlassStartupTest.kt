package com.lormusic.app

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Size
import org.junit.Assert.*
import org.junit.Test

class GlassStartupTest {
    @Test
    fun ntvienEffectsWaitForLayoutAndSurviveDetachReattach() {
        val scope = object : io.github.ntvien.liquidglass.internal.backdrop.BackdropEffectScopeImpl() {
            override val shape = CircleShape
        }
        var calls = 0
        val effects: io.github.ntvien.liquidglass.internal.backdrop.BackdropEffectScope.() -> Unit = {
            calls++
            assertEquals(32f, size.minDimension / 2f, .001f)
            padding = 8f
        }
        scope.apply(effects) // onAttach, before a draw/measurement
        assertEquals(0, calls)
        for (invalid in listOf(Size.Zero, Size(0f, 64f), Size(Float.POSITIVE_INFINITY, 64f))) {
            scope.size = invalid
            scope.apply(effects)
            assertEquals(0, calls)
        }
        scope.size = Size(128f, 64f)
        scope.apply(effects)
        assertEquals(1, calls)
        assertEquals(8f, scope.padding, .001f)
        scope.reset()
        scope.apply(effects)
        assertEquals(1, calls)
        assertEquals(0f, scope.padding, .001f)
        assertNull(scope.renderEffect)
        scope.size = Size(64f, 128f)
        scope.apply(effects)
        assertEquals(2, calls)
    }

    @Test
    fun kyantEffectsWaitForLayoutAndSurviveDetachReattach() {
        val scope = object : com.kyant.backdrop.BackdropEffectScopeImpl() {
            override val shape = CircleShape
        }
        var calls = 0
        val effects: com.kyant.backdrop.BackdropEffectScope.() -> Unit = {
            calls++
            assertEquals(32f, size.minDimension / 2f, .001f)
            padding = 8f
        }
        scope.apply(effects)
        assertEquals(0, calls)
        for (invalid in listOf(Size.Zero, Size(64f, 0f), Size(64f, Float.POSITIVE_INFINITY))) {
            scope.size = invalid
            scope.apply(effects)
            assertEquals(0, calls)
        }
        scope.size = Size(128f, 64f)
        scope.apply(effects)
        assertEquals(1, calls)
        scope.reset()
        scope.apply(effects)
        assertEquals(1, calls)
        assertEquals(0f, scope.padding, .001f)
        assertNull(scope.renderEffect)
        scope.size = Size(64f, 128f)
        scope.apply(effects)
        assertEquals(2, calls)
    }
}
