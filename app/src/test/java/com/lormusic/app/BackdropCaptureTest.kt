package com.lormusic.app

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import org.junit.Assert.*
import org.junit.Test

/** Covers the archive's nested optical capture without needing an Android GPU in JVM tests. */
class BackdropCaptureTest {
    private class KyantSource : com.kyant.backdrop.Backdrop {
        var recording = false
        override val isBeingRecorded get() = recording
        override val isCoordinatesDependent = false
        override fun DrawScope.drawBackdrop(density: Density, coordinates: LayoutCoordinates?, layerBlock: (GraphicsLayerScope.() -> Unit)?) {}
    }
    private class NtSource : io.github.ntvien.liquidglass.internal.backdrop.Backdrop {
        var recording = false
        override val isBeingRecorded get() = recording
        override val isCoordinatesDependent = false
        override fun DrawScope.drawBackdrop(density: Density, coordinates: LayoutCoordinates?, layerBlock: (GraphicsLayerScope.() -> Unit)?) {}
    }
    // Upstream factories use Compose remember; instantiate their actual backing class for this JVM regression.
    private fun combined(name: String, first: Any, second: Any): Any =
        Class.forName(name).declaredConstructors.single { it.parameterCount == 2 }
            .apply { isAccessible = true }.newInstance(first, second)

    @Test fun archiveIconCaptureCanStillSampleThePageButCannotSampleItself() {
        val page = KyantSource()
        val icons = KyantSource()
        val combined = combined("com.kyant.backdrop.backdrops.Combined2Backdrops", page, icons) as com.kyant.backdrop.Backdrop
        icons.recording = true
        assertFalse("Recording icons must not disable their independent page source", page.isBeingRecorded)
        assertTrue("A combined source containing the recording icons must reject self-capture", combined.isBeingRecorded)
        icons.recording = false
        assertFalse(combined.isBeingRecorded)
        page.recording = true
        assertTrue(combined.isBeingRecorded)
        assertFalse(icons.isBeingRecorded)
    }
    @Test fun ntvienUsesSourceSpecificCaptureStateToo() {
        val page = NtSource()
        val child = NtSource()
        val combined = combined("io.github.ntvien.liquidglass.internal.backdrop.backdrops.Combined2Backdrops", page, child) as io.github.ntvien.liquidglass.internal.backdrop.Backdrop
        child.recording = true
        assertFalse(page.isBeingRecorded)
        assertTrue(combined.isBeingRecorded)
        child.recording = false
        assertFalse(combined.isBeingRecorded)
        page.recording = true
        assertTrue(combined.isBeingRecorded)
    }
    @Test fun bothEnginesFollowTheSameLiveCaptureAfterStateChanges() {
        val source = KyantSource()
        val bridge = com.lormusic.app.ui.components.NtBackdropAdapter(source)
        val combined = combined("io.github.ntvien.liquidglass.internal.backdrop.backdrops.Combined2Backdrops", bridge, NtSource()) as io.github.ntvien.liquidglass.internal.backdrop.Backdrop
        assertFalse(bridge.isBeingRecorded)
        source.recording = true
        assertTrue(bridge.isBeingRecorded)
        assertTrue(combined.isBeingRecorded)
        source.recording = false
        assertFalse(bridge.isBeingRecorded)
        assertFalse(combined.isBeingRecorded)
    }

}
