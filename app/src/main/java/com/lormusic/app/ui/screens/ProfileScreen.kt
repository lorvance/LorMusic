package com.lormusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lormusic.app.model.AppIconOption
import com.lormusic.app.model.SearchBarPlacement
import com.lormusic.app.model.ThemeMode
import com.lormusic.app.ui.components.AppIconVectorPreview
import com.lormusic.app.ui.components.formatDurationShort

@Composable
fun ProfileScreen(
    totalSongsCount: Int,
    localSongsCount: Int,
    favoritesCount: Int,
    playlistsCount: Int,
    themeMode: ThemeMode,
    useDynamicColor: Boolean,
    useSquigglySlider: Boolean,
    enableBlur: Boolean,
    searchBarPlacement: SearchBarPlacement,
    enablePlayerSwipe: Boolean,
    hideDemoTracks: Boolean,
    selectedAppIcon: AppIconOption,
    playbackSpeed: Float,
    sleepTimerRemainingSec: Int?,
    onChangeThemeMode: (ThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onToggleSquigglySlider: (Boolean) -> Unit,
    onToggleBlur: (Boolean) -> Unit,
    onChangeSearchBarPlacement: (SearchBarPlacement) -> Unit,
    onTogglePlayerSwipe: (Boolean) -> Unit,
    onToggleHideDemoTracks: (Boolean) -> Unit,
    onSelectAppIcon: (AppIconOption) -> Unit,
    onChangePlaybackSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onRescanLocalMusic: () -> Unit,
    onOpenTagFix: () -> Unit,
    onCheckForUpdates: () -> Unit = {}
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSearchPlacementDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val iconChoices = listOf(
        AppIconOption.MINIMAL,
        AppIconOption.REGULAR,
        AppIconOption.OLD
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Треков",
                value = totalSongsCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Любимых",
                value = favoritesCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Плейлистов",
                value = playlistsCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Иконка приложения",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Выбери стиль иконки на рабочем столе (предпросмотр в реальном времени)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                iconChoices.forEachIndexed { idx, option ->
                    val isSelected = selectedAppIcon == option
                    val borderColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    }
                    val cardBg = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(cardBg)
                            .border(2.dp, borderColor, RoundedCornerShape(20.dp))
                            .clickable { onSelectAppIcon(option) }
                            .padding(vertical = 12.dp, horizontal = 6.dp)
                    ) {
                        AppIconVectorPreview(
                            option = option,
                            size = 66.dp,
                            cornerRadius = 18.dp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${idx + 1}) ${option.label}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = option.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                        Spacer(Modifier.height(6.dp))
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectAppIcon(option) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Оформление, навигация и плеер",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("Тема оформления") },
                    supportingContent = { Text(themeMode.label) },
                    leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null) },
                    modifier = Modifier.clickable { showThemeDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("Цвета Material You") },
                    supportingContent = { Text("При отключении используется лавандовая палитра под новую иконку") },
                    leadingContent = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = useDynamicColor,
                            onCheckedChange = onToggleDynamicColor
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("Размытие интерфейса (Blur)") },
                    supportingContent = { Text("Живой эффект матового стекла на верхней шапке и нижней панели") },
                    leadingContent = { Icon(Icons.Filled.BlurOn, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = enableBlur,
                            onCheckedChange = onToggleBlur
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("Расположение поиска") },
                    supportingContent = { Text(searchBarPlacement.label) },
                    leadingContent = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.clickable { showSearchPlacementDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("Свайпы треков в плеере") },
                    supportingContent = { Text("Свайп вправо — следующий трек, свайп влево — предыдущий") },
                    leadingContent = { Icon(Icons.Filled.Swipe, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = enablePlayerSwipe,
                            onCheckedChange = onTogglePlayerSwipe
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("Волнистый ползунок") },
                    supportingContent = { Text("Живая бегущая волна при воспроизведении") },
                    leadingContent = { Icon(Icons.Filled.Waves, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = useSquigglySlider,
                            onCheckedChange = onToggleSquigglySlider
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("Скрыть демо-треки") },
                    supportingContent = { Text("Показывать только реальную музыку с устройства") },
                    leadingContent = { Icon(Icons.Filled.VisibilityOff, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = hideDemoTracks,
                            onCheckedChange = onToggleHideDemoTracks
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Инструменты тегов и медиатека",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Column {
                ListItem(
                    headlineContent = {
                        Text(
                            text = "TagFix — Автоисправление тегов и Shazam-поиск",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    supportingContent = {
                        Text("Очистка названий, отделение исполнителя, поиск обложек и распознавание неизвестных треков")
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.AutoFixHigh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable { onOpenTagFix() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("Скорость воспроизведения") },
                    supportingContent = { Text("${playbackSpeed}x") },
                    leadingContent = { Icon(Icons.Filled.Speed, contentDescription = null) },
                    modifier = Modifier.clickable { showSpeedDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("Таймер сна") },
                    supportingContent = {
                        val status = sleepTimerRemainingSec?.let {
                            "Осталось ${formatDurationShort(it * 1000L)}"
                        } ?: "Выключен"
                        Text(status)
                    },
                    leadingContent = { Icon(Icons.Filled.Bedtime, contentDescription = null) },
                    modifier = Modifier.clickable { showSleepDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("Сканировать память устройства") },
                    supportingContent = { Text("Локальных треков найдено: $localSongsCount") },
                    leadingContent = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                    modifier = Modifier.clickable { onRescanLocalMusic() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("Проверить обновления") },
                    supportingContent = { Text("Текущая версия 1.5.6") },
                    leadingContent = { Icon(Icons.Filled.SystemUpdate, contentDescription = null) },
                    modifier = Modifier.clickable { onCheckForUpdates() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("О приложении") },
                    supportingContent = { Text("LorMusic 1.5.6") },
                    leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                    modifier = Modifier.clickable { showAboutDialog = true }
                )
            }
        }

        Spacer(Modifier.height(210.dp))
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Тема оформления") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onChangeThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = {
                                    onChangeThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(mode.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Закрыть") }
            }
        )
    }

    if (showSearchPlacementDialog) {
        AlertDialog(
            onDismissRequest = { showSearchPlacementDialog = false },
            title = { Text("Расположение кнопки поиска") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SearchBarPlacement.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onChangeSearchBarPlacement(option)
                                    showSearchPlacementDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = searchBarPlacement == option,
                                onClick = {
                                    onChangeSearchBarPlacement(option)
                                    showSearchPlacementDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (searchBarPlacement == option) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = option.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchPlacementDialog = false }) { Text("Закрыть") }
            }
        )
    }

    if (showSpeedDialog) {
        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Скорость воспроизведения") },
            text = {
                Column {
                    speeds.forEach { sp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onChangePlaybackSpeed(sp)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = playbackSpeed == sp,
                                onClick = {
                                    onChangePlaybackSpeed(sp)
                                    showSpeedDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${sp}x")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Закрыть") }
            }
        )
    }

    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text("Таймер сна") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        null to "Выключить таймер",
                        5 to "Через 5 минут",
                        15 to "Через 15 минут",
                        30 to "Через 30 минут",
                        60 to "Через 60 минут"
                    ).forEach { (mins, label) ->
                        TextButton(
                            onClick = {
                                onSetSleepTimer(mins)
                                showSleepDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepDialog = false }) { Text("Закрыть") }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("LorMusic 1.5.6") },
            text = {
                Text(
                    "Музыкальный плеер в стиле Material 3 Expressive + iOS Blur.\n\n" +
                        "• Исправлена ошибка воспроизведения (source error) в YouTube Music + полная длина всех треков\n" +
                        "• Жест навигации скольжением (Hold & Swipe) по нижнему навбару с плавным указателем\n" +
                        "• Плавная инерционная прокрутка (Smooth Scrolling) списков без рывков\n" +
                        "• Свайпы переключения треков в мини-плеере и полноэкранном плеере + кнопка предыдущего трека в мини-плеере\n" +
                        "• Удаление песен с устройства и скрытие треков из основного потока в папку «Скрытые»\n" +
                        "• Гибкая настройка расположения поиска и включения/выключения размытия (Blur)"
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Отлично") }
            }
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}
