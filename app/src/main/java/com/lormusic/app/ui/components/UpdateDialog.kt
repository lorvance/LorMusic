package com.lormusic.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lormusic.app.data.AppUpdateInfo
import com.lormusic.app.data.UpdateState
import com.lormusic.app.model.AppLanguage
import com.lormusic.app.model.LocalStrings
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private suspend fun translateChangelog(text: String, targetLang: String): String = withContext(Dispatchers.IO) {
    if (text.isBlank()) return@withContext text
    try {
        val token = "___LORMUSIC_BRAND___"
        val preprocessed = text.replace("LorMusic", token, ignoreCase = false).replace("LorMuisc", token, ignoreCase = false)
        val encoded = URLEncoder.encode(preprocessed, "UTF-8")
        val url = URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")

            setRequestProperty("User-Agent", "Mozilla/5.0")
        }
        conn.outputStream.use { it.write("q=$encoded".toByteArray(Charsets.UTF_8)) }
        if (conn.responseCode in 200..299) {
            val resp = conn.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(resp)
            val sentences = jsonArray.optJSONArray(0) ?: return@withContext text
            val sb = StringBuilder()
            for (i in 0 until sentences.length()) {
                val item = sentences.optJSONArray(i)
                if (item != null) {
                    sb.append(item.optString(0))
                }
            }
            val res = sb.toString()
            if (res.isNotBlank()) {
                res.replace(token, "LorMusic")
                    .replace("___ LORMUSIC_BRAND ___", "LorMusic")
                    .replace("___LORMUSIC_BRAND___", "LorMusic")
                    .replace("ЛорМьюзик", "LorMusic", ignoreCase = true)
                    .replace("Лормьюзик", "LorMusic", ignoreCase = true)
                    .replace("Лормузик", "LorMusic", ignoreCase = true)
                    .replace("Лор Музыка", "LorMusic", ignoreCase = true)
                    .replace("ЛорМузік", "LorMusic", ignoreCase = true)
                    .replace("Лормузік", "LorMusic", ignoreCase = true)
            } else text
        } else {
            text
        }
    } catch (_: Exception) {
        text
    }
}

@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    updateState: UpdateState,
    onDownload: () -> Unit,
    onInstall: (File) -> Unit,
    onDismiss: () -> Unit,
    appLanguage: AppLanguage = AppLanguage.RU,
    hazeState: HazeState? = null,
    enableBlur: Boolean = true
) {
    val strings = LocalStrings.current
    val targetLang = when (appLanguage) {
        AppLanguage.UK -> "uk"
        AppLanguage.RU -> "ru"
        AppLanguage.EN -> null
        AppLanguage.SYSTEM -> {
            val loc = java.util.Locale.getDefault().language.lowercase()
            when {
                loc.startsWith("uk") -> "uk"
                loc.startsWith("ru") || loc.startsWith("be") || loc.startsWith("kk") -> "ru"
                else -> null
            }
        }
    }
    val displayedChangelog by produceState(initialValue = updateInfo.changelog, updateInfo.changelog, targetLang) {
        if (targetLang != null) {
            value = runCatching { translateChangelog(updateInfo.changelog, targetLang) }.getOrDefault(updateInfo.changelog)
        }
    }
    var isChangelogExpanded by remember { mutableStateOf(true) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (isChangelogExpanded) 180f else 0f,
        label = "arrowRotation"
    )

    Dialog(
        onDismissRequest = {
            if (updateState !is UpdateState.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = updateState !is UpdateState.Downloading,
            dismissOnClickOutside = false
        )
    ) {
        val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
        val outlineVariant = MaterialTheme.colorScheme.outlineVariant

        val backdropColor = if (enableBlur) {
            Color.Black.copy(alpha = 0.35f)
        } else {
            Color.Black.copy(alpha = 0.75f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (updateState !is UpdateState.Downloading) {
                        onDismiss()
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 92.dp)
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        if (enableBlur) surfaceContainerHigh.copy(alpha = 0.90f)
                        else surfaceContainerHigh
                    )
                    .border(
                        1.dp,
                        if (enableBlur) Color.White.copy(alpha = 0.18f)
                        else outlineVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(28.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* absorb clicks */ }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.updateAvailable,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "v${updateInfo.versionName.removePrefix("v").removePrefix("V")}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isChangelogExpanded = !isChangelogExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.NewReleases,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = strings.changelogTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(arrowRotation)
                            )
                        }

                        AnimatedVisibility(
                            visible = isChangelogExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .heightIn(max = 220.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                                MarkdownText(markdown = displayedChangelog)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                when (updateState) {
                    is UpdateState.Downloading -> {
                        val progress = updateState.progress
                        val downloadedMb = updateState.downloadedBytes / (1024f * 1024f)
                        val totalMb = updateState.totalBytes / (1024f * 1024f)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.downloadingUpdate,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            WavyProgressBar(progress = progress)

                            Spacer(modifier = Modifier.height(6.dp))

                            if (totalMb > 0.1f) {
                                Text(
                                    text = "%.1f / %.1f MB".format(downloadedMb, totalMb),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is UpdateState.ReadyToInstall -> {
                        Button(
                            onClick = { onInstall(updateState.apkFile) },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(strings.installUpdate, style = MaterialTheme.typography.titleSmall)
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onDownload,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(strings.downloadUpdate, style = MaterialTheme.typography.titleSmall)
                            }

                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(strings.remindLater, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
}
