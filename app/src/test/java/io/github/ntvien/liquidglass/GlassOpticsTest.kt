package io.github.ntvien.liquidglass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassOpticsTest {
    @Test
    fun onlyCircularBoundsStretch() {
        assertTrue(isCircularGlassBounds(44f, 44f, 22f, 22f, 22f, 22f))
        assertTrue(isCircularGlassBounds(44f, 45f, 22f, 22f, 22f, 22f))
        assertFalse(isCircularGlassBounds(110f, 44f, 22f, 22f, 22f, 22f))
        assertFalse(isCircularGlassBounds(44f, 44f, 16f, 22f, 22f, 22f))
    }

    @Test
    fun refractionOnSmallBubblesLeavesRoomForBlurAndDispersion() {
        val glass = GlassOptics.forSize(40f, 36f, 24f, 64f, dispersion = true)

        assertTrue(glass.height < 20f)
        assertTrue(glass.amount < 20f)
        assertTrue(glass.padding >= glass.blur + 2f * glass.amount + 1f)
    }

    @Test
    fun unmeasuredGlassDoesNotAllocateABackdrop() {
        assertEquals(GlassOptics(0f, 0f, 0f, 0f), GlassOptics.forSize(0f, 36f, 24f, 64f, dispersion = true))
    }
}
