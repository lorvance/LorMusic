package com.lormusic.app.ui.screens

import android.os.Build
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.lormusic.app.R
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
import com.lormusic.app.ui.components.liquidGlassBubble

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
    pureMaterialYou: Boolean = false,
    showNavBarLabels: Boolean = false,
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
    onTogglePureMaterialYou: (Boolean) -> Unit = {},
    onToggleShowNavBarLabels: (Boolean) -> Unit = {},
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
    onExportBackup: () -> String = { "" },
    onImportBackup: (String) -> Boolean = { false },
    onCheckForUpdates: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    var updateClickCount by remember { mutableIntStateOf(0) }
    var easterEggStage by remember { mutableIntStateOf(0) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSearchPlacementDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLiquidGlassWarning by remember { mutableStateOf<String?>(null) }
    val isLiquidGlassSupported = Build.VERSION.SDK_INT >= 33
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreText by remember { mutableStateOf("") }

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.size(44.dp))
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .then(Modifier.liquidGlassBubble(RoundedCornerShape(22.dp)))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.settingsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .then(Modifier.liquidGlassBubble(CircleShape))
                    .clickable { showBackupDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = strings.backupTitle,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                iconChoices.forEach { option ->
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(cardBg)
                            .border(2.dp, borderColor, RoundedCornerShape(18.dp))
                            .clickable { onSelectAppIcon(option) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        AppIconVectorPreview(
                            option = option,
                            size = 52.dp,
                            cornerRadius = 14.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.getDisplayName(strings),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = option.getDisplaySubtitle(strings),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
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
                    headlineContent = { Text(strings.pureMaterialYouTitle) },
                    supportingContent = { Text(strings.pureMaterialYouDesc) },
                    leadingContent = { Icon(Icons.Filled.ColorLens, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = pureMaterialYou,
                            onCheckedChange = onTogglePureMaterialYou
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(strings.showNavBarLabelsTitle) },
                    supportingContent = { Text(strings.showNavBarLabelsDesc) },
                    leadingContent = { Icon(Icons.Filled.Label, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = showNavBarLabels,
                            onCheckedChange = onToggleShowNavBarLabels
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
                val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
                val isIgnoringBattery = remember(context) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
                    } else true
                }
                ListItem(
                    headlineContent = { Text(strings.backgroundPlaybackTitle) },
                    supportingContent = { Text(strings.backgroundPlaybackDesc) },
                    leadingContent = { Icon(Icons.Filled.ElectricBolt, contentDescription = null) },
                    trailingContent = {
                        Icon(
                            if (isIgnoringBattery) Icons.Filled.Check else Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = if (isIgnoringBattery) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier.clickable {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    }
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
                    modifier = Modifier.clickable {
                        updateClickCount++
                        if (updateClickCount == 10) {
                            playScreamSound()
                            easterEggStage = 1
                            android.widget.Toast.makeText(context, strings.easterEgg1Title, android.widget.Toast.LENGTH_LONG).show()
                        } else if (updateClickCount >= 20) {
                            playScreamSound()
                            easterEggStage = 2
                            android.widget.Toast.makeText(context, "${strings.easterEgg2Title} ${strings.easterEgg2Text}", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            onCheckForUpdates()
                        }
                    }
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

    if (easterEggStage == 1) {
        AlertDialog(
            onDismissRequest = { easterEggStage = 0 },
            confirmButton = {
                Button(
                    onClick = { easterEggStage = 0 },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(strings.easterEgg1Button)
                }
            },
            title = {
                Text(
                    text = strings.easterEgg1Title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.easter_cat_1),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = strings.easterEgg1Text,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    } else if (easterEggStage == 2) {
        AlertDialog(
            onDismissRequest = { easterEggStage = 0 },
            confirmButton = {
                Button(
                    onClick = { easterEggStage = 0 },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(strings.easterEgg2Button)
                }
            },
            title = {
                Text(
                    text = strings.easterEgg2Title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.easter_cat_2),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = strings.easterEgg2Text,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.backupTitle)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.backupDesc, style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = {
                            val json = onExportBackup()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("LorMusic Backup", json)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, strings.backupCopiedToast, android.widget.Toast.LENGTH_LONG).show()
                            try {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, json)
                                    putExtra(Intent.EXTRA_SUBJECT, "LorMusic-Settings-Backup.json")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, strings.shareBackup))
                            } catch (_: Exception) {}
                            showBackupDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.createBackup)
                    }
                    FilledTonalButton(
                        onClick = {
                            showBackupDialog = false
                            restoreText = ""
                            showRestoreDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Restore, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.restoreBackup)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.restoreBackup)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = restoreText,
                        onValueChange = { restoreText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        placeholder = { Text(strings.pasteBackupHint) },
                        shape = RoundedCornerShape(16.dp)
                    )
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val item = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                            if (!item.isNullOrBlank()) restoreText = item
                        }
                    ) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.pasteFromClipboard)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreText.isNotBlank()) {
                            val ok = onImportBackup(restoreText)
                            if (ok) showRestoreDialog = false
                        }
                    },
                    enabled = restoreText.isNotBlank()
                ) {
                    Text(strings.restore)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

private fun playScreamSound() {
    try {
        val sampleRate = 44100
        val durationMs = 600
        val numSamples = sampleRate * durationMs / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 800.0 + progress * 2400.0 + kotlin.math.sin(t * 150.0) * 180.0
            val angle = 2.0 * Math.PI * freq * t
            val amplitude = 32767.0 * (1.0 - progress * 0.15)
            val sample = (kotlin.math.sin(angle) * amplitude * 0.85).toInt().toShort()
            buffer[i] = sample
        }
        val track = android.media.AudioTrack.Builder()
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                android.media.AudioFormat.Builder()
                    .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(android.media.AudioTrack.MODE_STATIC)
            .build()
        track.write(buffer, 0, buffer.size)
        track.play()
    } catch (_: Exception) {
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
