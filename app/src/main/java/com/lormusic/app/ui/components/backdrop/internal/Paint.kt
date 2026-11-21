package com.lormusic.app.ui.components.backdrop.internal

import android.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativePaint
import com.lormusic.app.ui.components.backdrop.RuntimeShader
import com.lormusic.app.ui.components.backdrop.asAndroidRuntimeShader

internal fun Paint.blur(radius: Float) {
    this.nativePaint.maskFilter =
        if (radius > 0f) BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        else null
}

internal fun Paint.setRuntimeShader(runtimeShader: RuntimeShader?) {
    this.nativePaint.shader = runtimeShader?.asAndroidRuntimeShader()
}
