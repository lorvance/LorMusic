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
import androidx.compose.material.icons.filled.*
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
import com.lormusic.app.model.SearchBarPlacement
import com.lormusic.app.model.ThemeMode
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
        AppLanguage.PL -> "pl"
        AppLanguage.RU -> "ru"
        AppLanguage.EN -> null
        AppLanguage.SYSTEM -> {
            val loc = java.util.Locale.getDefault().language.lowercase()
            when {
                loc.startsWith("uk") -> "uk"
                loc.startsWith("pl") -> "pl"
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
            Color.Black.copy(alpha = 0.55f)
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
                        if (enableBlur) surfaceContainerHigh.copy(alpha = 0.96f)
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
                    ) {}
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

@Composable
fun SleepTimerAutoUpdaterDialog(
    currentRemainingSec: Int? = null,
    enableBlur: Boolean = true,
    onSelectMinutes: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
        val outlineVariant = MaterialTheme.colorScheme.outlineVariant
        val backdropColor = if (enableBlur) Color.Black.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.75f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 92.dp)
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        if (enableBlur) surfaceContainerHigh.copy(alpha = 0.96f)
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
                    ) {}
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
                                imageVector = Icons.Filled.Bedtime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.sleepTimerTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (currentRemainingSec != null && currentRemainingSec > 0) {
                                val mins = currentRemainingSec / 60
                                val secs = currentRemainingSec % 60
                                Text(
                                    text = "%02d:%02d".format(mins, secs),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            null to strings.sleepTimerOff,
                            5 to strings.minutesFormat.format(5),
                            15 to strings.minutesFormat.format(15),
                            30 to strings.minutesFormat.format(30),
                            60 to strings.minutesFormat.format(60)
                        ).forEach { (mins, label) ->
                            val isOff = mins == null
                            Button(
                                onClick = {
                                    onSelectMinutes(mins)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(18.dp),
                                colors = if (isOff) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(strings.close, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeAutoUpdaterDialog(
    currentTheme: ThemeMode,
    enableBlur: Boolean = true,
    onSelectTheme: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
        val outlineVariant = MaterialTheme.colorScheme.outlineVariant
        val backdropColor = if (enableBlur) Color.Black.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.75f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 92.dp)
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        if (enableBlur) surfaceContainerHigh.copy(alpha = 0.96f)
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
                    ) {}
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
                                imageVector = Icons.Filled.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.themeModeTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val isSelected = currentTheme == mode
                            Button(
                                onClick = {
                                    onSelectTheme(mode)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(18.dp),
                                colors = if (isSelected) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = mode.getDisplayName(strings),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(strings.close, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchPlacementAutoUpdaterDialog(
    currentPlacement: SearchBarPlacement,
    enableBlur: Boolean = true,
    onSelectPlacement: (SearchBarPlacement) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
        val outlineVariant = MaterialTheme.colorScheme.outlineVariant
        val backdropColor = if (enableBlur) Color.Black.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.75f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 92.dp)
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        if (enableBlur) surfaceContainerHigh.copy(alpha = 0.96f)
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
                    ) {}
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
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.searchPlacementTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SearchBarPlacement.entries.forEach { option ->
                            val isSelected = currentPlacement == option
                            Button(
                                onClick = {
                                    onSelectPlacement(option)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(18.dp),
                                colors = if (isSelected) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = option.getDisplayName(strings),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(strings.close, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun PlaybackSpeedAutoUpdaterDialog(
    currentSpeed: Float,
    enableBlur: Boolean = true,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
        val outlineVariant = MaterialTheme.colorScheme.outlineVariant
        val backdropColor = if (enableBlur) Color.Black.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.75f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 92.dp)
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        if (enableBlur) surfaceContainerHigh.copy(alpha = 0.96f)
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
                    ) {}
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
                                imageVector = Icons.Filled.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.playbackSpeedTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        speeds.forEach { sp ->
                            val isSelected = currentSpeed == sp
                            Button(
                                onClick = {
                                    onSelectSpeed(sp)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(18.dp),
                                colors = if (isSelected) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${sp}x",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(strings.close, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun AboutAutoUpdaterDialog(
    aboutText: String,
    enableBlur: Boolean = true,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
        val outlineVariant = MaterialTheme.colorScheme.outlineVariant
        val backdropColor = if (enableBlur) Color.Black.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.75f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 92.dp)
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        if (enableBlur) surfaceContainerHigh.copy(alpha = 0.96f)
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
                    ) {}
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
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LorMusic",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "v${com.lormusic.app.BuildConfig.VERSION_NAME} | ${com.lormusic.app.BuildConfig.VERSION_CODE}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 280.dp)
                            .clip(RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            Text(
                                text = aboutText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(strings.great, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BackupAutoUpdaterDialog(
    enableBlur: Boolean = true,
    onExport: () -> Unit,
    onOpenRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
        val outlineVariant = MaterialTheme.colorScheme.outlineVariant
        val backdropColor = if (enableBlur) Color.Black.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.75f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 92.dp)
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        if (enableBlur) surfaceContainerHigh.copy(alpha = 0.96f)
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
                    ) {}
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
                                imageVector = androidx.compose.material.icons.Icons.Filled.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.backupTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.backupDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onExport,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.createBackup, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FilledTonalButton(
                        onClick = onOpenRestore,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.restoreBackup, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.cancel, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun RestoreAutoUpdaterDialog(
    enableBlur: Boolean = true,
    restoreText: String,
    onTextChange: (String) -> Unit,
    onPaste: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
        val outlineVariant = MaterialTheme.colorScheme.outlineVariant
        val backdropColor = if (enableBlur) Color.Black.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.75f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 92.dp)
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        if (enableBlur) surfaceContainerHigh.copy(alpha = 0.96f)
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
                    ) {}
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
                                imageVector = androidx.compose.material.icons.Icons.Filled.Restore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.restoreBackup,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.backupDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedTextField(
                        value = restoreText,
                        onValueChange = onTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        placeholder = { Text(strings.pasteBackupHint, style = MaterialTheme.typography.bodySmall) },
                        shape = RoundedCornerShape(18.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onPaste,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.pasteFromClipboard, style = MaterialTheme.typography.labelLarge)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onRestore,
                        enabled = restoreText.isNotBlank(),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(strings.restore, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.cancel, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

