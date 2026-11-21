package com.lormusic.app

import com.lormusic.app.data.GlassNavigationPosition as Position
import org.junit.Assert.*
import org.junit.Test

class GlassNavigationTest {
    @Test fun dragCentersAndRightToLeftSelectTheCorrectTab() {
        assertEquals(0f, Position.finger(50f, 100f, 3, false), .001f)
        assertEquals(1f, Position.finger(150f, 100f, 3, false), .001f)
        assertEquals(2f, Position.finger(250f, 100f, 3, false), .001f)
        assertEquals(2f, Position.finger(50f, 100f, 3, true), .001f)
        assertEquals(0f, Position.finger(250f, 100f, 3, true), .001f)
        assertEquals(2, Position.target(1.8f, 3))
        assertEquals(0, Position.target(.2f, 3))
    }
    @Test fun EdgeDragsAndUnavailableGeometryStayInsideTabBounds() {
        assertEquals(0, Position.target(-100f, 3))
        assertEquals(2, Position.target(100f, 3))
        assertEquals(0, Position.target(Float.NaN, 3))
        assertEquals(0f, Position.finger(20f, 0f, 3, false), .001f)
        assertEquals(0f, Position.finger(Float.NaN, 100f, 3, false), .001f)
        assertEquals(0f, Position.finger(100f, 100f, 0, false), .001f)
    }
}
