package com.lormusic.app.ui.screens

import android.os.Build
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
import com.lormusic.app.model.LocalStrings
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
    useLiquidGlass: Boolean = false,
    enableBetaUpdates: Boolean = false,
    useAlbumArtGradient: Boolean = false,
    useDynamicPlayerColors: Boolean = true,
    autoTagFixEnabled: Boolean = false,
    classicLibraryDesign: Boolean = false,
    appLanguage: com.lormusic.app.model.AppLanguage = com.lormusic.app.model.AppLanguage.SYSTEM,
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
    onToggleLiquidGlass: (Boolean) -> Unit = {},
    onToggleBetaUpdates: (Boolean) -> Unit = {},
    onToggleAlbumArtGradient: (Boolean) -> Unit = {},
    onToggleDynamicPlayerColors: (Boolean) -> Unit = {},
    onToggleAutoTagFix: (Boolean) -> Unit = {},
    onToggleClassicLibraryDesign: (Boolean) -> Unit = {},
    onSelectLanguage: (com.lormusic.app.model.AppLanguage) -> Unit = {},
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
    val strings = LocalStrings.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSearchPlacementDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLiquidGlassWarning by remember { mutableStateOf<String?>(null) }
    val isLiquidGlassSupported = Build.VERSION.SDK_INT >= 33

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
            text = strings.settingsTitle,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = strings.statsTracks,
                value = totalSongsCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = strings.statsFavorites,
                value = favoritesCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = strings.statsPlaylists,
                value = playlistsCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = strings.appIconSectionTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = strings.appIconSectionDesc,
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
                            text = option.getDisplayName(strings),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = option.getDisplaySubtitle(strings),
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
            text = strings.appearanceAndPlayerSection,
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
                    headlineContent = { Text(strings.themeModeTitle) },
                    supportingContent = { Text(themeMode.getDisplayName(strings)) },
                    leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null) },
                    modifier = Modifier.clickable { showThemeDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(strings.materialYouTitle) },
                    supportingContent = { Text(strings.materialYouDesc) },
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
                    headlineContent = { Text(strings.blurEffectTitle) },
                    supportingContent = { Text(strings.blurEffectDesc) },
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
                    headlineContent = { Text(strings.liquidGlassTitle) },
                    supportingContent = {
                        Text(
                            if (isLiquidGlassSupported) {
                                strings.liquidGlassDescSupported
                            } else {
                                "${strings.liquidGlassUnavailableTitle}. Android ${Build.VERSION.RELEASE}"
                            }
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.AutoFixNormal, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = useLiquidGlass,
                            onCheckedChange = { checked ->
                                if (!isLiquidGlassSupported) {
                                    showLiquidGlassWarning = "${strings.liquidGlassUnavailableTitle}. Android ${Build.VERSION.RELEASE}"
                                } else {
                                    onToggleLiquidGlass(checked)
                                }
                            }
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(strings.dynamicCoverGradientTitle) },
                    supportingContent = { Text(strings.dynamicCoverGradientDesc) },
                    leadingContent = { Icon(Icons.Filled.Gradient, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = useAlbumArtGradient,
                            onCheckedChange = onToggleAlbumArtGradient
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(strings.dynamicPlayerColorsTitle) },
                    supportingContent = { Text(strings.dynamicPlayerColorsDesc) },
                    leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = useDynamicPlayerColors,
                            onCheckedChange = onToggleDynamicPlayerColors
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(strings.searchPlacementTitle) },
                    supportingContent = { Text(searchBarPlacement.getDisplayName(strings)) },
                    leadingContent = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.clickable { showSearchPlacementDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(strings.playerSwipeTitle) },
                    supportingContent = { Text(strings.playerSwipeDesc) },
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
                    headlineContent = { Text(strings.squigglySliderTitle) },
                    supportingContent = { Text(strings.squigglySliderDesc) },
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
                    headlineContent = { Text(strings.languageTitle) },
                    supportingContent = { Text(appLanguage.getLabel(strings)) },
                    leadingContent = { Icon(Icons.Filled.Language, contentDescription = null) },
                    modifier = Modifier.clickable { showLanguageDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(strings.classicLibraryDesignTitle) },
                    supportingContent = { Text(strings.classicLibraryDesignDesc) },
                    leadingContent = { Icon(Icons.Filled.ViewAgenda, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = classicLibraryDesign,
                            onCheckedChange = onToggleClassicLibraryDesign
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(strings.hideDemoTracksTitle) },
                    supportingContent = { Text(strings.hideDemoTracksTitle) },
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
            text = strings.librarySection,
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
                            text = strings.tagFixSectionTitle,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    supportingContent = {
                        Text(strings.tagFixSubtitle)
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
                    headlineContent = { Text(strings.autoTagFixTitle) },
                    supportingContent = { Text(strings.autoTagFixDesc) },
                    leadingContent = { Icon(Icons.Filled.ElectricBolt, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = autoTagFixEnabled,
                            onCheckedChange = onToggleAutoTagFix
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(strings.betaUpdatesTitle) },
                    supportingContent = { Text(strings.betaUpdatesDesc) },
                    leadingContent = { Icon(Icons.Filled.BugReport, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = enableBetaUpdates,
                            onCheckedChange = onToggleBetaUpdates
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(strings.playbackSpeedTitle) },
                    supportingContent = { Text("${playbackSpeed}x") },
                    leadingContent = { Icon(Icons.Filled.Speed, contentDescription = null) },
                    modifier = Modifier.clickable { showSpeedDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(strings.sleepTimerTitle) },
                    supportingContent = {
                        val status = sleepTimerRemainingSec?.let {
                            formatDurationShort(it * 1000L)
                        } ?: strings.sleepTimerOff
                        Text(status)
                    },
                    leadingContent = { Icon(Icons.Filled.Bedtime, contentDescription = null) },
                    modifier = Modifier.clickable { showSleepDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(strings.rescanLibraryTitle) },
                    supportingContent = { Text("$localSongsCount ${strings.tracksCount}") },
                    leadingContent = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                    modifier = Modifier.clickable { onRescanLocalMusic() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(strings.checkForUpdatesTitle) },
                    supportingContent = { Text(strings.appVersion) },
                    leadingContent = { Icon(Icons.Filled.SystemUpdate, contentDescription = null) },
                    modifier = Modifier.clickable { onCheckForUpdates() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(strings.aboutAppSection) },
                    supportingContent = { Text(strings.appVersion) },
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
            title = { Text(strings.themeModeTitle) },
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
                            Text(mode.getDisplayName(strings))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text(strings.close) }
            }
        )
    }

    if (showSearchPlacementDialog) {
        AlertDialog(
            onDismissRequest = { showSearchPlacementDialog = false },
            title = { Text(strings.searchPlacementTitle) },
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
                                    text = option.getDisplayName(strings),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (searchBarPlacement == option) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = option.getDisplaySubtitle(strings),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchPlacementDialog = false }) { Text(strings.close) }
            }
        )
    }

    if (showSpeedDialog) {
        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text(strings.playbackSpeedTitle) },
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
                TextButton(onClick = { showSpeedDialog = false }) { Text(strings.close) }
            }
        )
    }

    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text(strings.sleepTimerTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        null to strings.sleepTimerOff,
                        5 to strings.minutesFormat.format(5),
                        15 to strings.minutesFormat.format(15),
                        30 to strings.minutesFormat.format(30),
                        60 to strings.minutesFormat.format(60)
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
                TextButton(onClick = { showSleepDialog = false }) { Text(strings.close) }
            }
        )
    }

    if (showLiquidGlassWarning != null) {
        AlertDialog(
            onDismissRequest = { showLiquidGlassWarning = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text(strings.liquidGlassUnavailableTitle, fontWeight = FontWeight.Bold) },
            text = { Text(showLiquidGlassWarning ?: "") },
            confirmButton = {
                TextButton(onClick = { showLiquidGlassWarning = null }) {
                    Text(strings.understood)
                }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(strings.languageTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    com.lormusic.app.model.AppLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (appLanguage == lang),
                                onClick = {
                                    onSelectLanguage(lang)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(lang.getLabel(strings), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(strings.close)
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(strings.appVersion) },
            text = {
                val aboutBody = when (strings) {
                    com.lormusic.app.model.StringsEn ->
                        "Music player styled with Material 3 Expressive + Apple Liquid Glass.\n\n" +
                            "• Redesigned YouTube Music with floating bubbles and 4-row recents scroll\n" +
                            "• Launch animations: equalizer for Default, ball bounce for Classic, pulse for Minimalism\n" +
                            "• Strict sequential track playback order when shuffle is off\n" +
                            "• Liquid Glass (Apple) design on Android 13+\n" +
                            "• Beta update notifications and Markdown changelog\n" +
                            "• Automatic TagFix and Shazam track recognition\n" +
                            "• Dynamic album art backdrop gradient and direct track downloads"
                    com.lormusic.app.model.StringsUk ->
                        "Музичний плеєр у стилі Material 3 Expressive + Apple Liquid Glass.\n\n" +
                            "• Новий дизайн YouTube Music з плаваючими баблами та 4-рядковим скролом недавніх\n" +
                            "• Анімації запуску: еквалайзер для стандартної, політ кульки для класичної, підстрибування для мінімалізму\n" +
                            "• Виправлено випадковий порядок треків при вимкненому перемішуванні\n" +
                            "• Дизайн Liquid Glass (Apple) для Android 13+\n" +
                            "• Отримання бета-оновлень та вікно автоапдейтера з Markdown\n" +
                            "• Автоматичний TagFix та розпізнавання через Shazam\n" +
                            "• Динамічний градієнт обкладинки та завантаження треків"
                    else ->
                        "Музыкальный плеер в стиле Material 3 Expressive + Apple Liquid Glass.\n\n" +
                            "• Новый дизайн YouTube Music с плавающими баблами и 4-строчным скроллом недавних (22 трека)\n" +
                            "• Анимации запуска: эквалайзер для обычной, полёт красного шарика для Old, подпрыгивание для минимализма\n" +
                            "• Исправлен случайный порядок треков при выключенном перемешивании (строгий линейный порядок)\n" +
                            "• Дизайн Liquid Glass (Apple) для Android 13+ с физическими бликами и переключателем\n" +
                            "• Получение бета-обновлений и редизайн окна автоапдейтера с Markdown\n" +
                            "• Автоматический TagFix и индивидуальное распознавание треков через Shazam\n" +
                            "• Динамический градиент обложки в плеере «Сейчас играет» и кнопка скачивания трека"
                }
                Text(aboutBody)
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text(strings.great) }
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
