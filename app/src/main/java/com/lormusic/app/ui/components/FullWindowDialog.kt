package com.lormusic.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.unit.dp

val LocalDialogBottomInset = compositionLocalOf { 0.dp }
val LocalDialogTopInset = compositionLocalOf { 0.dp }

private tailrec fun Context.dialogActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext !== this) baseContext.dialogActivity() else null
    else -> null
}

/** One physical window viewport: floating dialog decor cannot shift its bottom off-screen. */
@Composable
fun FullWindowDialog(onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        val configuration = LocalConfiguration.current
        val manager = LocalContext.current.getSystemService(WindowManager::class.java)
        val bounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.currentWindowMetrics.bounds
        } else {
            @Suppress("DEPRECATION")
            val metrics = DisplayMetrics().also { manager.defaultDisplay.getRealMetrics(it) }
            android.graphics.Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
        }
        val density = LocalDensity.current
        // Floating dialogs on some devices report zero navigation insets.
        // Read the activity's real system bars rather than guessing a dp offset.
        val activityView = LocalContext.current.dialogActivity()?.window?.decorView
        val activityInsets = activityView?.let { ViewCompat.getRootWindowInsets(it) }
            ?.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val safeBottom = with(density) { (activityInsets?.bottom ?: 0).toDp() }
        val safeTop = with(density) { (activityInsets?.top ?: 0).toDp() }
        val width = with(density) { bounds.width().toDp() }
        val height = with(density) { bounds.height().toDp() }
        DisposableEffect(window, configuration) {
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
                it.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
                it.setGravity(Gravity.TOP or Gravity.START)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    it.attributes = it.attributes.apply {
                        layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                        else WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
                it.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            onDispose { }
        }
        // Consume physical system bars once. Inner screens and pinned footers
        // now share the same safe rectangle, even if dialog insets report zero.
        val safePadding = PaddingValues(top = safeTop, bottom = safeBottom)
        CompositionLocalProvider(LocalDialogBottomInset provides 0.dp,
            LocalDialogTopInset provides 0.dp) {
            Box(Modifier.requiredSize(width, height).padding(safePadding)
                .consumeWindowInsets(safePadding).imePadding()) { content() }
        }
    }
}
