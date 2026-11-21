package com.kyant.backdrop.backdrops

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import com.kyant.backdrop.internal.recordLayer

fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier =
    this then LayerBackdropElement(backdrop)

private class LayerBackdropElement(
    val backdrop: LayerBackdrop
) : ModifierNodeElement<LayerBackdropNode>() {

    override fun create(): LayerBackdropNode {
        return LayerBackdropNode(backdrop)
    }

    override fun update(node: LayerBackdropNode) {
        node.replaceBackdrop(backdrop)
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "layerBackdrop"
        properties["backdrop"] = backdrop
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LayerBackdropElement) return false

        if (backdrop != other.backdrop) return false

        return true
    }

    override fun hashCode(): Int {
        return backdrop.hashCode()
    }
}

private class LayerBackdropNode(
    var backdrop: LayerBackdrop
) : DrawModifierNode, GlobalPositionAwareModifierNode, Modifier.Node() {

    private var lastCoordinates: LayoutCoordinates? = null

    fun replaceBackdrop(next: LayerBackdrop) {
        if (backdrop === next) return
        if (backdrop.layerCoordinates === lastCoordinates) backdrop.layerCoordinates = null
        backdrop = next
        next.layerCoordinates = lastCoordinates?.takeIf { it.isAttached }
    }


    override fun ContentDrawScope.draw() {
        drawContent()
        if (com.lormusic.app.ui.components.backdrop.backdrops.activeBackdropRecordings == 0 && size.width > 0f && size.height > 0f) {
            com.lormusic.app.ui.components.backdrop.backdrops.activeBackdropRecordings++
            try {
                backdrop.recording = true
                recordLayer(backdrop.graphicsLayer) { backdrop.onDraw(this@draw) }
            } finally {
                backdrop.recording = false
                com.lormusic.app.ui.components.backdrop.backdrops.activeBackdropRecordings--
            }
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            lastCoordinates = coordinates
            backdrop.layerCoordinates = coordinates
        }
    }

    override fun onDetach() {
        if (backdrop.layerCoordinates === lastCoordinates) backdrop.layerCoordinates = null
        lastCoordinates = null
    }
}
