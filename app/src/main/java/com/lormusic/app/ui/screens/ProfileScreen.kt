package com.lormusic.app.ui.screens

import android.os.Build
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.lormusic.app.model.AppIconOption
import com.lormusic.app.model.LocalStrings
import com.lormusic.app.model.SearchBarPlacement
import com.lormusic.app.model.ThemeMode
import com.lormusic.app.ui.components.AboutAutoUpdaterDialog
import com.lormusic.app.ui.components.AppIconVectorPreview
import com.lormusic.app.ui.components.PlaybackSpeedAutoUpdaterDialog
import com.lormusic.app.ui.components.SearchPlacementAutoUpdaterDialog
import com.lormusic.app.ui.components.ThemeAutoUpdaterDialog
import com.lormusic.app.ui.components.appBackdropSource
import com.lormusic.app.ui.components.formatDurationShort
import com.lormusic.app.ui.components.liquidGlassBubble
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

enum class SettingsCategory {
    APPEARANCE,
    NAVIGATION,
    PLAYBACK,
    BETA,
    SYSTEM
}

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
    hazeState: HazeState? = null,
    enableBetaUpdates: Boolean = false,
    betaExpandingSearch: Boolean = false,
    betaSeparateHeaderBubbles: Boolean = false,
    betaDynamicPlaybackContext: Boolean = true,
    betaAutoHideNavBar: Boolean = false,
    betaCategorizedSettings: Boolean = true,
    betaHideDividers: Boolean = false,
    useAlbumArtGradient: Boolean = true,
    useMyWaveCoverGradient: Boolean = false,
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
    onToggleBetaExpandingSearch: (Boolean) -> Unit = {},
    onToggleBetaSeparateHeaderBubbles: (Boolean) -> Unit = {},
    onToggleBetaDynamicPlaybackContext: (Boolean) -> Unit = {},
    onToggleBetaAutoHideNavBar: (Boolean) -> Unit = {},
    onToggleBetaCategorizedSettings: (Boolean) -> Unit = {},
    onToggleBetaHideDividers: (Boolean) -> Unit = {},
    onToggleAlbumArtGradient: (Boolean) -> Unit = {},
    onToggleMyWaveCoverGradient: (Boolean) -> Unit = {},
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
    onCheckForUpdates: () -> Unit = {},
    onBackToLibrary: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val prefs = remember(context) { com.lormusic.app.data.PreferencesRepository(context) }
    var aboutClickCount by remember { mutableIntStateOf(0) }
    var easterEggStage by remember { mutableIntStateOf(0) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSearchPlacementDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLiquidGlassWarning by remember { mutableStateOf<String?>(null) }
    val isLiquidGlassSupported = Build.VERSION.SDK_INT >= 33
    var showBackupDialog by remember { mutableStateOf(false) }
    val iconChoices = remember {
        listOf(
            AppIconOption.MINIMAL,
            AppIconOption.REGULAR,
            AppIconOption.OLD
        )
    }
    var activeCategory by remember { mutableStateOf<SettingsCategory?>(null) }
    val scrollState = rememberScrollState()
    LaunchedEffect(activeCategory) {
        scrollState.scrollTo(0)
    }
    BackHandler(enabled = betaCategorizedSettings && activeCategory != null) {
        activeCategory = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .then(if (hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier)
                .then(if ((showLanguageDialog || showSleepDialog || showThemeDialog || showSearchPlacementDialog || showSpeedDialog || showAboutDialog || showEqualizerSheet || easterEggStage > 0) && enableBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(24.dp) else Modifier)
                .appBackdropSource()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(start = 20.dp, end = 20.dp, top = 74.dp, bottom = 185.dp)
            ) {
                @Composable
                fun BetaBadge() {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = "BETA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                @Composable
                fun BetaHeadline(title: String) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        BetaBadge()
                    }
                }

                @Composable
                fun SettingsDivider() {
                    if (!betaHideDividers) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }

                val currentAppVersion = "${com.lormusic.app.BuildConfig.VERSION_NAME} | ${com.lormusic.app.BuildConfig.VERSION_CODE}"
                val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
                val isIgnoringBattery = remember(context) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
                    } else true
                }

                val waveGradTitle = when (strings) {
                    com.lormusic.app.model.StringsEn -> "Live 'My Wave' cover gradient"
                    com.lormusic.app.model.StringsUk -> "Живий градієнт «Моєї хвилі»"
                    com.lormusic.app.model.StringsPl -> "Żywy gradient «Mojej fali»"
                    else -> "Живой градиент «Моей волны»"
                }
                val waveGradDesc = when (strings) {
                    com.lormusic.app.model.StringsEn -> "Powerful moving gradient and Play button color from wave track covers"
                    com.lormusic.app.model.StringsUk -> "Потужний рухомий градієнт та колір кнопки Play з обкладинок треків хвилі"
                    com.lormusic.app.model.StringsPl -> "Dynamiczny ruchomy gradient i kolor przycisku Play z okładek utworów fali"
                    else -> "Мощный движущийся градиент и цвет кнопки Play из обложек треков волны"
                }

                @Composable
                fun AppIconSelectorContent() {
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
                }

                @Composable
                fun AppearanceContent(includeIconSelector: Boolean) {
                    if (includeIconSelector) {
                        AppIconSelectorContent()
                        Spacer(Modifier.height(20.dp))
                    }
                    Text(
                        text = strings.settingsCategoryAppearance,
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
                            SettingsDivider()
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
                            SettingsDivider()
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
                            SettingsDivider()
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
                            SettingsDivider()
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
                            SettingsDivider()
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
                            SettingsDivider()
                            ListItem(
                                headlineContent = { Text(text = waveGradTitle) },
                                supportingContent = { Text(waveGradDesc) },
                                leadingContent = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                                trailingContent = {
                                    Switch(
                                        checked = useMyWaveCoverGradient,
                                        onCheckedChange = onToggleMyWaveCoverGradient
                                    )
                                }
                            )
                        }
                    }
                }

                @Composable
                fun NavigationContent() {
                    Text(
                        text = strings.settingsCategoryNavigation,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    var previewTab by remember { mutableStateOf(com.lormusic.app.ui.components.LorTab.LIBRARY) }
                    var previewSearch by remember { mutableStateOf(false) }
                    val previewHaze = remember { HazeState() }
                    Text("Предпросмотр", style = MaterialTheme.typography.labelLarge)
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))) {
                        Box(Modifier.matchParentSize().hazeSource(previewHaze).background(
                            Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface))))
                        com.lormusic.app.ui.components.LorBottomNavBar(
                            selected = previewTab, onSelect = { previewTab = it },
                            hazeState = previewHaze, enableBlur = enableBlur && !pureMaterialYou,
                            useLiquidGlass = useLiquidGlass && !pureMaterialYou,
                            showLabels = showNavBarLabels,
                            showBottomSearchButton = !searchBarPlacement.localAtTop,
                            isLocalSearchActive = previewSearch, onToggleLocalSearch = { previewSearch = !previewSearch },
                            applyNavigationInsets = false
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text(strings.searchBarPlacementTitle) },
                                supportingContent = { Text(searchBarPlacement.getDisplayName(strings)) },
                                leadingContent = { Icon(Icons.Filled.Search, contentDescription = null) },
                                modifier = Modifier.clickable { showSearchPlacementDialog = true }
                            )
                            SettingsDivider()
                            ListItem(
                                headlineContent = { Text(strings.showNavBarLabelsTitle) },
                                supportingContent = { Text(strings.showNavBarLabelsDesc) },
                                leadingContent = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                                trailingContent = {
                                    Switch(
                                        checked = showNavBarLabels,
                                        onCheckedChange = onToggleShowNavBarLabels
                                    )
                                }
                            )
                            SettingsDivider()
                            ListItem(
                                headlineContent = { Text(strings.betaCategorizedSettingsTitle) },
                                supportingContent = { Text(strings.betaCategorizedSettingsDesc) },
                                leadingContent = { Icon(Icons.Filled.Category, contentDescription = null) },
                                trailingContent = {
                                    Switch(
                                        checked = betaCategorizedSettings,
                                        onCheckedChange = { checked ->
                                            if (!checked) {
                                                activeCategory = null
                                            }
                                            onToggleBetaCategorizedSettings(checked)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                @Composable
                fun PlaybackContent() {
                    Text(
                        text = strings.settingsCategoryPlayback,
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
                                headlineContent = { Text(strings.betaDynamicPlaybackContextTitle) },
                                supportingContent = { Text(strings.betaDynamicPlaybackContextDesc) },
                                leadingContent = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                                trailingContent = {
                                    Switch(
                                        checked = betaDynamicPlaybackContext,
                                        onCheckedChange = onToggleBetaDynamicPlaybackContext
                                    )
                                }
                            )
                            SettingsDivider()
                            ListItem(
                                headlineContent = { Text(strings.equalizerTitle) },
                                supportingContent = { Text(if (prefs.getEqualizerEnabled()) strings.equalizerStatusEnabled else strings.equalizerStatusDisabled) },
                                leadingContent = { Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.clickable { showEqualizerSheet = true }
                            )
                            SettingsDivider()
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
                            SettingsDivider()
                            ListItem(
                                headlineContent = { Text(strings.dynamicPlayerColorsTitle) },
                                supportingContent = { Text(strings.dynamicPlayerColorsDesc) },
                                leadingContent = { Icon(Icons.Filled.InvertColors, contentDescription = null) },
                                trailingContent = {
                                    Switch(
                                        checked = useDynamicPlayerColors,
                                        onCheckedChange = onToggleDynamicPlayerColors
                                    )
                                }
                            )
                            SettingsDivider()
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
                            SettingsDivider()
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
                            SettingsDivider()
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
                        }
                    }
                }

                @Composable
                fun BetaContent() {
                    Text(
                        text = strings.settingsCategoryBeta,
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
                                headlineContent = { BetaHeadline("Расширяемый поиск") },
                                supportingContent = { Text("Поле поиска плавно появляется внутри верхней панели вместо отдельной строки") },
                                leadingContent = { Icon(Icons.Filled.Search, null) },
                                trailingContent = { Switch(checked = betaExpandingSearch, onCheckedChange = onToggleBetaExpandingSearch) }
                            )
                            SettingsDivider()
                            ListItem(
                                headlineContent = { BetaHeadline(strings.betaAutoHideNavBarTitle) },
                                supportingContent = { Text(strings.betaAutoHideNavBarDesc) },
                                leadingContent = { Icon(Icons.Filled.VisibilityOff, contentDescription = null) },
                                trailingContent = {
                                    Switch(
                                        checked = betaAutoHideNavBar,
                                        onCheckedChange = onToggleBetaAutoHideNavBar
                                    )
                                }
                            )
                            SettingsDivider()
                            ListItem(
                                headlineContent = { BetaHeadline(strings.betaSeparateHeaderBubblesTitle) },
                                supportingContent = { Text(strings.betaSeparateHeaderBubblesDesc) },
                                leadingContent = { Icon(Icons.Filled.BubbleChart, contentDescription = null) },
                                trailingContent = {
                                    Switch(
                                        checked = betaSeparateHeaderBubbles,
                                        onCheckedChange = onToggleBetaSeparateHeaderBubbles
                                    )
                                }
                            )
                            SettingsDivider()
                            ListItem(
                                headlineContent = { BetaHeadline(strings.betaHideDividersTitle) },
                                supportingContent = { Text(strings.betaHideDividersDesc) },
                                leadingContent = { Icon(Icons.Filled.LineWeight, contentDescription = null) },
                                trailingContent = {
                                    Switch(
                                        checked = betaHideDividers,
                                        onCheckedChange = onToggleBetaHideDividers
                                    )
                                }
                            )
                        }
                    }
                }

                @Composable
                fun SystemContent() {
                    Text(
                        text = strings.settingsCategorySystem,
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
                                headlineContent = { Text(strings.languageTitle) },
                                supportingContent = { Text(appLanguage.getLabel(strings)) },
                                leadingContent = { Icon(Icons.Filled.Language, contentDescription = null) },
                                modifier = Modifier.clickable { showLanguageDialog = true }
                            )
                            SettingsDivider()
                            ListItem(
                                headlineContent = { Text(strings.backupTitle) },
                                supportingContent = { Text(strings.backupDesc) },
                                leadingContent = { Icon(Icons.Filled.Backup, contentDescription = null) },
                                modifier = Modifier.clickable { showBackupDialog = true }
                            )
                            SettingsDivider()
                            ListItem(
                                headlineContent = { Text(strings.checkForUpdatesTitle) },
                                supportingContent = { Text(currentAppVersion) },
                                leadingContent = { Icon(Icons.Filled.SystemUpdate, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    onCheckForUpdates()
                                }
                            )
                            SettingsDivider()
                            ListItem(
                                headlineContent = { Text(strings.aboutAppSection) },
                                supportingContent = { Text(currentAppVersion) },
                                leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    aboutClickCount++
                                    if (aboutClickCount == 10) {
                                        playScreamSound()
                                        easterEggStage = 1
                                    } else if (aboutClickCount >= 20) {
                                        playScreamSound()
                                        easterEggStage = 2
                                    } else {
                                        showAboutDialog = true
                                    }
                                }
                            )
                        }
                    }
                }

                if (betaCategorizedSettings) {
                    AnimatedContent(
                        targetState = activeCategory,
                        transitionSpec = {
                            if (targetState != null && initialState == null) {
                                (slideInHorizontally(animationSpec = tween(280)) { width -> (width * 0.25f).toInt() } + fadeIn(animationSpec = tween(240)))
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(240)) { width -> -(width * 0.2f).toInt() } + fadeOut(animationSpec = tween(180)))
                            } else if (targetState == null && initialState != null) {
                                (slideInHorizontally(animationSpec = tween(280)) { width -> -(width * 0.2f).toInt() } + fadeIn(animationSpec = tween(240)))
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(240)) { width -> (width * 0.25f).toInt() } + fadeOut(animationSpec = tween(180)))
                            } else {
                                (fadeIn(animationSpec = tween(220))).togetherWith(fadeOut(animationSpec = tween(180)))
                            }
                        },
                        label = "settingsCategoryAnimation"
                    ) { category ->
                        if (category == null) {
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        ListItem(
                                            headlineContent = { Text(strings.settingsCategoryAppearance, fontWeight = FontWeight.SemiBold) },
                                            supportingContent = { Text(strings.settingsCategoryAppearanceDesc) },
                                            leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                                            modifier = Modifier.clickable { activeCategory = SettingsCategory.APPEARANCE }
                                        )
                                        SettingsDivider()
                                        ListItem(
                                            headlineContent = { Text(strings.settingsCategoryNavigation, fontWeight = FontWeight.SemiBold) },
                                            supportingContent = { Text(strings.settingsCategoryNavigationDesc) },
                                            leadingContent = { Icon(Icons.Filled.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                                            modifier = Modifier.clickable { activeCategory = SettingsCategory.NAVIGATION }
                                        )
                                        SettingsDivider()
                                        ListItem(
                                            headlineContent = { Text(strings.settingsCategoryPlayback, fontWeight = FontWeight.SemiBold) },
                                            supportingContent = { Text(strings.settingsCategoryPlaybackDesc) },
                                            leadingContent = { Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                                            modifier = Modifier.clickable { activeCategory = SettingsCategory.PLAYBACK }
                                        )
                                        SettingsDivider()
                                        ListItem(
                                            headlineContent = { BetaHeadline(strings.settingsCategoryBeta) },
                                            supportingContent = { Text(strings.settingsCategoryBetaDesc) },
                                            leadingContent = { Icon(Icons.Filled.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                                            modifier = Modifier.clickable { activeCategory = SettingsCategory.BETA }
                                        )
                                        SettingsDivider()
                                        ListItem(
                                            headlineContent = { Text(strings.settingsCategorySystem, fontWeight = FontWeight.SemiBold) },
                                            supportingContent = { Text(strings.settingsCategorySystemDesc) },
                                            leadingContent = { Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                                            modifier = Modifier.clickable { activeCategory = SettingsCategory.SYSTEM }
                                        )
                                    }
                                }
                            }
                        } else {
                            Column {
                                when (category) {
                                    SettingsCategory.APPEARANCE -> AppearanceContent(includeIconSelector = true)
                                    SettingsCategory.NAVIGATION -> NavigationContent()
                                    SettingsCategory.PLAYBACK -> PlaybackContent()
                                    SettingsCategory.BETA -> BetaContent()
                                    SettingsCategory.SYSTEM -> SystemContent()
                                }
                            }
                        }
                    }
                } else {
                    AppIconSelectorContent()
                    Spacer(Modifier.height(20.dp))
                    AppearanceContent(includeIconSelector = false)
                    Spacer(Modifier.height(20.dp))
                    NavigationContent()
                    Spacer(Modifier.height(20.dp))
                    PlaybackContent()
                    Spacer(Modifier.height(20.dp))
                    BetaContent()
                    Spacer(Modifier.height(20.dp))
                    SystemContent()
                }

                Spacer(Modifier.height(210.dp))
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .clipToBounds()
        ) {
            val topScrimColor = MaterialTheme.colorScheme.background
            val effectiveBlur = enableBlur && !pureMaterialYou && hazeState != null
            if (effectiveBlur) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .hazeEffect(state = hazeState!!) {
                            blurEnabled = true
                            backgroundColor = topScrimColor
                            blurRadius = 24.dp
                            tints = listOf(HazeTint(topScrimColor.copy(alpha = 0.65f)))
                        }
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    topScrimColor.copy(alpha = 0.85f),
                                    topScrimColor.copy(alpha = 0.50f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    topScrimColor.copy(alpha = 0.96f),
                                    topScrimColor.copy(alpha = 0.78f),
                                    topScrimColor.copy(alpha = 0.42f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .then(
                            Modifier.liquidGlassBubble(
                                shape = CircleShape,
                                hazeState = hazeState,
                                enableBlur = enableBlur && !pureMaterialYou,
                                isLiquidGlass = useLiquidGlass && !pureMaterialYou
                            )
                        )
                        .clickable {
                            if (betaCategorizedSettings && activeCategory != null) {
                                activeCategory = null
                            } else {
                                onBackToLibrary()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    com.lormusic.app.ui.components.IosBackChevron(color = MaterialTheme.colorScheme.onSurface)
                }
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .then(
                            Modifier.liquidGlassBubble(
                                shape = RoundedCornerShape(22.dp),
                                hazeState = hazeState,
                                enableBlur = enableBlur && !pureMaterialYou,
                                isLiquidGlass = useLiquidGlass && !pureMaterialYou
                            )
                        )
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val headerTitle = if (betaCategorizedSettings && activeCategory != null) {
                        when (activeCategory) {
                            SettingsCategory.APPEARANCE -> strings.settingsCategoryAppearance
                            SettingsCategory.NAVIGATION -> strings.settingsCategoryNavigation
                            SettingsCategory.PLAYBACK -> strings.settingsCategoryPlayback
                            SettingsCategory.BETA -> strings.settingsCategoryBeta
                            SettingsCategory.SYSTEM -> strings.settingsCategorySystem
                            null -> strings.settingsTitle
                        }
                    } else {
                        strings.settingsTitle
                    }
                    AnimatedContent(
                        targetState = headerTitle,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)).togetherWith(fadeOut(animationSpec = tween(150)))
                        },
                        label = "headerTitleAnim"
                    ) { titleText ->
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .then(
                            Modifier.liquidGlassBubble(
                                shape = CircleShape,
                                hazeState = hazeState,
                                enableBlur = enableBlur && !pureMaterialYou,
                                isLiquidGlass = useLiquidGlass && !pureMaterialYou
                            )
                        )
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
        }
    }
    if (showThemeDialog) {
        ThemeAutoUpdaterDialog(
            currentTheme = themeMode,
            enableBlur = enableBlur,
            onSelectTheme = { onChangeThemeMode(it) },
            onDismiss = { showThemeDialog = false }
        )
    }
    if (showSearchPlacementDialog) {
        SearchPlacementAutoUpdaterDialog(
            currentPlacement = searchBarPlacement,
            enableBlur = enableBlur,
            onSelectPlacement = { onChangeSearchBarPlacement(it) },
            onDismiss = { showSearchPlacementDialog = false }
        )
    }
    if (showSpeedDialog) {
        PlaybackSpeedAutoUpdaterDialog(
            currentSpeed = playbackSpeed,
            enableBlur = enableBlur,
            onSelectSpeed = { onChangePlaybackSpeed(it) },
            onDismiss = { showSpeedDialog = false }
        )
    }
    if (showSleepDialog) {
        com.lormusic.app.ui.components.SleepTimerAutoUpdaterDialog(
            currentRemainingSec = null,
            enableBlur = enableBlur,
            onSelectMinutes = onSetSleepTimer,
            onDismiss = { showSleepDialog = false }
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
        Dialog(
            onDismissRequest = { showLanguageDialog = false },
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
                    ) { showLanguageDialog = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 92.dp)
                        .navigationBarsPadding()
                        .clip(RoundedCornerShape(28.dp))
                        .background(if (enableBlur) surfaceContainerHigh.copy(alpha = 0.96f) else surfaceContainerHigh)
                        .border(1.dp, if (enableBlur) Color.White.copy(alpha = 0.18f) else outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = strings.languageTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        com.lormusic.app.model.AppLanguage.entries.forEach { lang ->
                            val isSelected = appLanguage == lang
                            val itemBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(itemBg)
                                    .clickable {
                                        onSelectLanguage(lang)
                                        showLanguageDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSelectLanguage(lang)
                                        showLanguageDialog = false
                                    }
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = lang.getLabel(strings),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = { showLanguageDialog = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(strings.close)
                        }
                    }
                }
            }
        }
    }
    if (showAboutDialog) {
        val aboutBody = when (strings) {
            com.lormusic.app.model.StringsEn ->
                "Music player styled with Material 3 Expressive + Apple Liquid Glass.\n\n" +
                    "• Redesigned YouTube Music with floating bubbles and 4-row recents scroll\n" +
                    "• Launch animations: equalizer for Default, ball bounce for Classic, pulse for Minimalism\n" +
                    "• Strict sequential track playback order when shuffle is off\n" +
                    "• Liquid Glass (Apple) design on Android 13+\n" +
                    "• Markdown update changelog and Auto-Updater\n" +
                    "• Automatic TagFix and Shazam track recognition\n" +
                    "• Dynamic album art backdrop gradient and direct track downloads"
            com.lormusic.app.model.StringsUk ->
                "Музичний плеєр у стилі Material 3 Expressive + Apple Liquid Glass.\n\n" +
                    "• Новий дизайн YouTube Music з плаваючими баблами та 4-рядковим скролом недавніх\n" +
                    "• Анімації запуску: еквалайзер для стандартної, політ кульки для класичної, підстрибування для мінімалізму\n" +
                    "• Виправлено випадковий порядок треків при вимкненому перемішуванні\n" +
                    "• Дизайн Liquid Glass (Apple) для Android 13+\n" +
                    "• Вікно автоапдейтера з Markdown\n" +
                    "• Автоматичний TagFix та розпізнавання через Shazam\n" +
                    "• Динамічний градієнт обкладинки та завантаження треків"
            com.lormusic.app.model.StringsPl ->
                "Odtwarzacz muzyczny w stylu Material 3 Expressive + Apple Liquid Glass.\n\n" +
                    "• Integracja z YouTube Music oraz SoundCloud i wspólnymi playlistami\n" +
                    "• Nowoczesny interfejs z pływającymi bąbelkami i efektami Liquid Glass\n" +
                    "• Wiele źródeł tekstów piosenek z synchronizacją i tłumaczeniem\n" +
                    "• Automatyczny TagFix oraz rozpoznawanie utworów przez Shazam\n" +
                    "• Dynamiczny gradient okładki w odtwarzaczu i bezpośrednie pobieranie"
            else ->
                "Музыкальный плеер в стиле Material 3 Expressive + Apple Liquid Glass.\n\n" +
                    "• Интеграция SoundCloud с чартами, поиском и общими плейлистами с YouTube Music\n" +
                    "• Новый дизайн YouTube Music с плавающими баблами и 4-строчным скроллом недавних (22 трека)\n" +
                    "• Анимации запуска: эквалайзер для обычной, полёт красного шарика для Old, подпрыгивание для минимализма\n" +
                    "• Исправлен случайный порядок треков при выключенном перемешивании (строгий линейный порядок)\n" +
                    "• Дизайн Liquid Glass (Apple) для Android 13+ с физическими бликами и переключателем\n" +
                    "• Редизайн окна автоапдейтера с Markdown\n" +
                    "• Автоматический TagFix и индивидуальное распознавание треков через Shazam\n" +
                    "• Динамический градиент обложки в плеере «Сейчас играет» и кнопка скачивания трека"
        }
        AboutAutoUpdaterDialog(
            aboutText = aboutBody,
            enableBlur = enableBlur,
            onDismiss = { showAboutDialog = false }
        )
    }
    if (easterEggStage == 1) {
        Dialog(
            onDismissRequest = { easterEggStage = 0 },
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
                    ) { easterEggStage = 0 },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 92.dp)
                        .navigationBarsPadding()
                        .clip(RoundedCornerShape(28.dp))
                        .background(if (enableBlur) surfaceContainerHigh.copy(alpha = 0.96f) else surfaceContainerHigh)
                        .border(1.dp, if (enableBlur) Color.White.copy(alpha = 0.18f) else outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.easterEgg1Title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(12.dp))
                        Image(
                            painter = painterResource(id = R.drawable.easter_cat_1),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = strings.easterEgg1Text,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { easterEggStage = 0 },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.easterEgg1Button)
                        }
                    }
                }
            }
        }
    } else if (easterEggStage == 2) {
        Dialog(
            onDismissRequest = { easterEggStage = 0 },
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
                    ) { easterEggStage = 0 },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 92.dp)
                        .navigationBarsPadding()
                        .clip(RoundedCornerShape(28.dp))
                        .background(if (enableBlur) surfaceContainerHigh.copy(alpha = 0.96f) else surfaceContainerHigh)
                        .border(1.dp, if (enableBlur) Color.White.copy(alpha = 0.18f) else outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.easterEgg2Title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        Image(
                            painter = painterResource(id = R.drawable.easter_cat_2),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
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
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { easterEggStage = 0 },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.easterEgg2Button)
                        }
                    }
                }
            }
        }
    }
    if (showBackupDialog) {
        com.lormusic.app.ui.components.BackupDialogs(
            onExport = onExportBackup, onImport = onImportBackup,
            onDismiss = { showBackupDialog = false }
        )
    }
    if (showEqualizerSheet) {
        com.lormusic.app.ui.components.EqualizerSheet(
            onDismiss = { showEqualizerSheet = false }
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
