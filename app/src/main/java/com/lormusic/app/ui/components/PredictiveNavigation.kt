package com.lormusic.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collect

internal val LocalBackPreview = compositionLocalOf<(Float) -> Unit> { {} }

@Composable
internal fun PreviewBackHandler(enabled: Boolean = true, previewOverride: ((Float) -> Unit)? = null, onBack: () -> Unit) {
    val context = LocalContext.current
    val predictive = remember(context) { predictiveBackPreference(context) }
    val preview = previewOverride ?: LocalBackPreview.current
    val commit by rememberUpdatedState(onBack)
    BackHandler(enabled && !predictive.value) { commit() }
    PredictiveBackHandler(enabled && predictive.value) { events ->
        try {
            events.collect { preview(it.progress.coerceIn(0f, 1f)) }
            commit()
        } finally {
            preview(0f)
        }
    }
}

private var preference: MutableState<Boolean>? = null

internal fun predictiveBackPreference(context: android.content.Context): MutableState<Boolean> =
    preference ?: mutableStateOf(backPreferences(context).getBoolean("enabled", false)).also { preference = it }

internal fun backPreferences(context: android.content.Context) =
    context.getSharedPreferences("predictive_navigation", android.content.Context.MODE_PRIVATE)

internal fun Modifier.backPreviewTransform(progress: () -> Float): Modifier = graphicsLayer {
    val amount = progress()
    scaleX = 1f - amount * .06f
    scaleY = scaleX
    alpha = 1f - amount * .14f
}
