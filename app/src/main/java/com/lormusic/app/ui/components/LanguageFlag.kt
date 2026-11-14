package com.lormusic.app.ui.components

import androidx.compose.material.icons.filled.Language
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lormusic.app.model.AppLanguage

/** Original vector flags, drawn at the current display density. */
@Composable
fun LanguageFlag(language: AppLanguage) {
    if (language == AppLanguage.SYSTEM) {
        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Language,
            null, Modifier.size(30.dp, 20.dp))
        return
    }
    Canvas(Modifier.size(30.dp, 20.dp)) {
        fun stripe(color: Color, y: Float, height: Float) = drawRect(color, Offset(0f, size.height * y), Size(size.width, size.height * height))
        drawRect(Color.White)
        when (language) {
            AppLanguage.RU -> { stripe(Color(0xFF0039A6), 1f/3, 1f/3); stripe(Color(0xFFD52B1E), 2f/3, 1f/3) }
            AppLanguage.EN -> {
                drawRect(Color(0xFFCE1124), Offset(size.width * .4f, 0f), Size(size.width * .2f, size.height))
                stripe(Color(0xFFCE1124), .4f, .2f)
            }
            AppLanguage.UK -> { stripe(Color(0xFF0057B7), 0f, .5f); stripe(Color(0xFFFFD700), .5f, .5f) }
            AppLanguage.PL -> stripe(Color(0xFFDC143C), .5f, .5f)
            else -> Unit
        }
    }
}
