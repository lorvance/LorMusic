package com.lormusic.app.ui.components.backdrop.backdrops

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import com.lormusic.app.ui.components.backdrop.internal.recordLayer

fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier =
    this then LayerBackdropElement(backdrop)

private class LayerBackdropElement(
    val backdrop: LayerBackdrop
) : ModifierNodeElement<LayerBackdropNode>() {

    override fun create(): LayerBackdropNode {
        return LayerBackdropNode(backdrop)
    }

    override fun update(node: LayerBackdropNode) {
        if (node.backdrop != backdrop) {
            node.backdrop.layerCoordinates = null
            node.backdrop = backdrop
        }
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

    override fun ContentDrawScope.draw() {
        drawContent()
        if (size.width >= 1f && size.height >= 1f && size.width <= 8192f && size.height <= 8192f) {
            try {
                backdrop.isRecording = true
                recordLayer(this@LayerBackdropNode, backdrop.graphicsLayer) { backdrop.onDraw(this@draw) }
            } catch (_: Throwable) {
            } finally {
                backdrop.isRecording = false
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
        if (backdrop.layerCoordinates === lastCoordinates) {
            backdrop.layerCoordinates = null
        }
        lastCoordinates = null
    }
}
