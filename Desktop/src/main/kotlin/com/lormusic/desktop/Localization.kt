package com.lormusic.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.gson.reflect.TypeToken

val LocalLanguage = staticCompositionLocalOf { "ru" }
private val dictionaries = mutableMapOf<String, Map<String, String>>()

@Composable
fun t(text: String): String = translate(LocalLanguage.current, text)

internal fun translate(lang: String, text: String): String {
    if (lang == "ru") return text
    val map =
        dictionaries.getOrPut(lang) {
            runCatching {
                    val raw =
                        object {}
                            .javaClass
                            .getResourceAsStream("/i18n-$lang.json")!!
                            .bufferedReader()
                            .readText()
                    gson.fromJson<Map<String, String>>(
                        raw,
                        object : TypeToken<Map<String, String>>() {}.type,
                    )
                }
                .getOrDefault(emptyMap())
        }
    return map[text] ?: text
}

@Composable
fun Flag(code: String) {
    Canvas(Modifier.size(30.dp, 21.dp).clip(RoundedCornerShape(4.dp))) {
        val w = size.width
        val h = size.height
        when (code) {
            "ru" -> {
                drawRect(Color.White)
                drawRect(Color(0xFF2454BB), Offset(0f, h / 3), Size(w, h / 3))
                drawRect(Color(0xFFD04449), Offset(0f, h * 2 / 3), Size(w, h / 3))
            }
            "en" -> {
                drawRect(Color.White)
                drawRect(Color(0xFFCE3046), Offset(w * .41f, 0f), Size(w * .18f, h))
                drawRect(Color(0xFFCE3046), Offset(0f, h * .39f), Size(w, h * .22f))
            }
            "uk" -> {
                drawRect(Color(0xFF2878CC))
                drawRect(Color(0xFFFFD34B), Offset(0f, h / 2), Size(w, h / 2))
            }
            "pl" -> {
                drawRect(Color.White)
                drawRect(Color(0xFFDC3C58), Offset(0f, h / 2), Size(w, h / 2))
            }
            "de" -> {
                drawRect(Color(0xFF222222))
                drawRect(Color(0xFFC93E42), Offset(0f, h / 3), Size(w, h / 3))
                drawRect(Color(0xFFF4CD48), Offset(0f, h * 2 / 3), Size(w, h / 3))
            }
        }
    }
}
