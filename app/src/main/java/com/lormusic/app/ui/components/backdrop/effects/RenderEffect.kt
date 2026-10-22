package com.lormusic.app.ui.components.backdrop.effects

import androidx.compose.ui.graphics.RenderEffect
import com.lormusic.app.ui.components.backdrop.BackdropEffectScope
import com.lormusic.app.ui.components.backdrop.RuntimeShader
import com.lormusic.app.ui.components.backdrop.internal.RuntimeShaderEffect
import com.lormusic.app.ui.components.backdrop.internal.chain
import com.lormusic.app.ui.components.backdrop.isRenderEffectSupported
import com.lormusic.app.ui.components.backdrop.isRuntimeShaderSupported
import org.intellij.lang.annotations.Language
import kotlin.contracts.ExperimentalContracts

fun BackdropEffectScope.effect(effect: RenderEffect) {
    if (!isRenderEffectSupported()) return

    renderEffect = renderEffect.chain(effect)
}

@OptIn(ExperimentalContracts::class)
fun BackdropEffectScope.runtimeShaderEffect(
    key: String,
    @Language("AGSL") shaderString: String,
    uniformShaderName: String,
    block: RuntimeShader.() -> Unit
) {
    if (!isRuntimeShaderSupported()) return

    val effect =
        RuntimeShaderEffect(
            runtimeShader = obtainRuntimeShader(key, shaderString).apply(block),
            uniformShaderName = uniformShaderName
        )
    renderEffect = renderEffect.chain(effect)
}
