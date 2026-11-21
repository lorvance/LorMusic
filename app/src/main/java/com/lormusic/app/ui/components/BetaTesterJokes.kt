package com.lormusic.app.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lormusic.app.R

internal enum class BetaTesterJoke(val label: String, val icon: Int, val artwork: Int, val sound: Int) {
    VIBE("Вайбкодить", R.drawable.ic_beta_clawd, R.drawable.beta_joke_vibe, R.raw.beta_joke_vibe),
    DOORS("двери гнуться", R.drawable.ic_beta_door, R.drawable.beta_joke_doors, R.raw.beta_joke_doors),
}

@Composable
internal fun BetaTesterJokeIcon(joke: BetaTesterJoke, tint: Color = LocalContentColor.current) {
    Image(
        painterResource(joke.icon),
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        colorFilter = if (joke == BetaTesterJoke.DOORS) ColorFilter.tint(tint) else null,
    )
}

@Composable
internal fun BetaTesterJokeDialog(joke: BetaTesterJoke, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(joke, context, lifecycle) {
        val player = MediaPlayer.create(context, joke.sound)
        player?.start()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) onDismiss()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            player?.release()
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black).clickable(onClick = onDismiss)) {
            Image(
                painter = painterResource(joke.artwork),
                contentDescription = joke.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
