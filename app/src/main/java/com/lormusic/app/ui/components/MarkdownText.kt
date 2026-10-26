package com.lormusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val lines = markdown.lines()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.isBlank() -> {
                    Spacer(Modifier.height(4.dp))
                }

                trimmed == "---" || trimmed == "___" || trimmed == "***" -> {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                trimmed.startsWith("### ") -> {
                    Text(
                        text = parseMarkdownInline(trimmed.removePrefix("### ")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                trimmed.startsWith("## ") -> {
                    Text(
                        text = parseMarkdownInline(trimmed.removePrefix("## ")),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseMarkdownInline(trimmed.removePrefix("# ")),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                trimmed.startsWith("* ") || trimmed.startsWith("- ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, top = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp, end = 8.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = parseMarkdownInline(trimmed.substring(2)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                        )
                    }
                }

                else -> {
                    Text(
                        text = parseMarkdownInline(trimmed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
            }
        }
    }
}

@Composable
private fun parseMarkdownInline(text: String): AnnotatedString {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant

    return buildAnnotatedString {
        var i = 0
        val len = text.length

        while (i < len) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = onSurfaceColor)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append("**")
                        i += 2
                    }
                }

                text.startsWith("`", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBgColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append(" " + text.substring(i + 1, end) + " ")
                        }
                        i = end + 1
                    } else {
                        append("`")
                        i++
                    }
                }

                text.startsWith("[", i) -> {
                    val closeBracket = text.indexOf("]", i)
                    val openParen = if (closeBracket != -1) text.indexOf("(", closeBracket) else -1
                    val closeParen = if (openParen == closeBracket + 1) text.indexOf(")", openParen) else -1

                    if (closeBracket != -1 && openParen == closeBracket + 1 && closeParen != -1) {
                        val linkText = text.substring(i + 1, closeBracket)
                        withStyle(
                            SpanStyle(
                                color = primaryColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append(linkText)
                        }
                        i = closeParen + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }

                text.startsWith("http://", i) || text.startsWith("https://", i) -> {
                    var end = i
                    while (end < len && !text[end].isWhitespace() && text[end] != ')' && text[end] != ']') {
                        end++
                    }
                    val url = text.substring(i, end)
                    withStyle(
                        SpanStyle(
                            color = primaryColor,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(url)
                    }
                    i = end
                }

                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
