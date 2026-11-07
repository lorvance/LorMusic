package com.lormusic.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lormusic.app.R
import com.lormusic.app.model.AppIconOption
import com.lormusic.app.model.LocalStrings
import com.lormusic.app.model.SearchBarPlacement
import com.lormusic.app.model.ThemeMode
import com.lormusic.app.ui.components.AppIconVectorPreview
import com.lormusic.app.ui.components.SearchPlacementAutoUpdaterDialog
import com.lormusic.app.ui.components.ThemeAutoUpdaterDialog

enum class OnboardingStep {
    WELCOME,
    NOTIFICATIONS,
    STORAGE,
    SETUP_CHOICE,
    QUICK_SETTINGS
}

@Composable
fun OnboardingScreen(
    themeMode: ThemeMode,
    useDynamicColor: Boolean,
    pureMaterialYou: Boolean,
    enableBlur: Boolean,
    useLiquidGlass: Boolean,
    showNavBarLabels: Boolean,
    searchBarPlacement: SearchBarPlacement,
    useAlbumArtGradient: Boolean,
    useDynamicPlayerColors: Boolean,
    betaDynamicPlaybackContext: Boolean,
    onChangeThemeMode: (ThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onTogglePureMaterialYou: (Boolean) -> Unit,
    onToggleBlur: (Boolean) -> Unit,
    onToggleLiquidGlass: (Boolean) -> Unit,
    onToggleShowNavBarLabels: (Boolean) -> Unit,
    onChangeSearchBarPlacement: (SearchBarPlacement) -> Unit,
    onToggleAlbumArtGradient: (Boolean) -> Unit,
    onToggleDynamicPlayerColors: (Boolean) -> Unit,
    onToggleBetaDynamicPlaybackContext: (Boolean) -> Unit,
    onOpenEqualizer: () -> Unit,
    onFinishOnboarding: (configured: Boolean) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }

    val requiredStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, requiredStoragePermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val storageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStoragePermission = granted
    }

    BackHandler(enabled = currentStep != OnboardingStep.WELCOME) {
        currentStep = when (currentStep) {
            OnboardingStep.NOTIFICATIONS -> OnboardingStep.WELCOME
            OnboardingStep.STORAGE -> OnboardingStep.NOTIFICATIONS
            OnboardingStep.SETUP_CHOICE -> OnboardingStep.STORAGE
            OnboardingStep.QUICK_SETTINGS -> OnboardingStep.SETUP_CHOICE
            OnboardingStep.WELCOME -> OnboardingStep.WELCOME
        }
    }

    val isFinishing = remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = !isFinishing.value,
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(350)) + slideOutVertically(animationSpec = tween(350)) { it / 6 }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally(animationSpec = tween(320)) { width -> width } + fadeIn(animationSpec = tween(280)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> -width / 2 } + fadeOut(animationSpec = tween(220)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(320)) { width -> -width } + fadeIn(animationSpec = tween(280)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> width / 2 } + fadeOut(animationSpec = tween(220)))
                    }
                },
                label = "onboardingStepTransition"
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> {
                        WelcomeStepContent(
                            onNext = { currentStep = OnboardingStep.NOTIFICATIONS }
                        )
                    }
                    OnboardingStep.NOTIFICATIONS -> {
                        NotificationStepContent(
                            isGranted = hasNotificationPermission,
                            onRequest = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    hasNotificationPermission = true
                                }
                            },
                            onNext = { currentStep = OnboardingStep.STORAGE }
                        )
                    }
                    OnboardingStep.STORAGE -> {
                        StorageStepContent(
                            isGranted = hasStoragePermission,
                            onRequest = { storageLauncher.launch(requiredStoragePermission) },
                            onNext = { currentStep = OnboardingStep.SETUP_CHOICE }
                        )
                    }
                    OnboardingStep.SETUP_CHOICE -> {
                        SetupChoiceStepContent(
                            onConfigure = { currentStep = OnboardingStep.QUICK_SETTINGS },
                            onSkip = { isFinishing.value = true }
                        )
                    }
                    OnboardingStep.QUICK_SETTINGS -> {
                        QuickSettingsStepContent(
                            themeMode = themeMode,
                            useDynamicColor = useDynamicColor,
                            pureMaterialYou = pureMaterialYou,
                            enableBlur = enableBlur,
                            useLiquidGlass = useLiquidGlass,
                            showNavBarLabels = showNavBarLabels,
                            searchBarPlacement = searchBarPlacement,
                            useAlbumArtGradient = useAlbumArtGradient,
                            useDynamicPlayerColors = useDynamicPlayerColors,
                            betaDynamicPlaybackContext = betaDynamicPlaybackContext,
                            onChangeThemeMode = onChangeThemeMode,
                            onToggleDynamicColor = onToggleDynamicColor,
                            onTogglePureMaterialYou = onTogglePureMaterialYou,
                            onToggleBlur = onToggleBlur,
                            onToggleLiquidGlass = onToggleLiquidGlass,
                            onToggleShowNavBarLabels = onToggleShowNavBarLabels,
                            onChangeSearchBarPlacement = onChangeSearchBarPlacement,
                            onToggleAlbumArtGradient = onToggleAlbumArtGradient,
                            onToggleDynamicPlayerColors = onToggleDynamicPlayerColors,
                            onToggleBetaDynamicPlaybackContext = onToggleBetaDynamicPlaybackContext,
                            onOpenEqualizer = onOpenEqualizer,
                            onFinish = { isFinishing.value = true }
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(isFinishing.value) {
        if (isFinishing.value) {
            kotlinx.coroutines.delay(350)
            onFinishOnboarding(true)
        }
    }
}

@Composable
private fun WelcomeStepContent(onNext: () -> Unit) {
    val strings = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(10.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.size(136.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.ic_lor_logo_vector),
                    contentDescription = null,
                    modifier = Modifier.size(130.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = strings.welcomeTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(28.dp))

            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FeatureRow(
                        icon = Icons.Filled.CloudQueue,
                        title = strings.welcomeFeature1Title,
                        desc = strings.welcomeFeature1Desc
                    )
                    FeatureRow(
                        icon = Icons.Filled.FolderOpen,
                        title = strings.welcomeFeature2Title,
                        desc = strings.welcomeFeature2Desc
                    )
                    FeatureRow(
                        icon = Icons.Filled.GraphicEq,
                        title = strings.welcomeFeature3Title,
                        desc = strings.welcomeFeature3Desc
                    )
                    FeatureRow(
                        icon = Icons.Filled.Lyrics,
                        title = strings.welcomeFeature4Title,
                        desc = strings.welcomeFeature4Desc
                    )
                }
            }
        }

        Button(
            onClick = onNext,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = strings.nextButton,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun NotificationStepContent(
    isGranted: Boolean,
    onRequest: () -> Unit,
    onNext: () -> Unit
) {
    val strings = LocalStrings.current

    val bellAngle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(250)
        bellAngle.animateTo(-24f, tween(110, easing = FastOutSlowInEasing))
        bellAngle.animateTo(24f, tween(110, easing = FastOutSlowInEasing))
        bellAngle.animateTo(-18f, tween(100, easing = FastOutSlowInEasing))
        bellAngle.animateTo(18f, tween(100, easing = FastOutSlowInEasing))
        bellAngle.animateTo(-10f, tween(90, easing = FastOutSlowInEasing))
        bellAngle.animateTo(10f, tween(90, easing = FastOutSlowInEasing))
        bellAngle.animateTo(0f, tween(120, easing = FastOutSlowInEasing))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(10.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                modifier = Modifier.size(160.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isGranted) Icons.Filled.NotificationsActive else Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(84.dp)
                            .graphicsLayer {
                                rotationZ = bellAngle.value
                                transformOrigin = TransformOrigin(0.5f, 0.1f)
                            }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = strings.permissionNotificationsTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = strings.permissionNotificationsDesc,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        AnimatedContent(
            targetState = isGranted,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)).togetherWith(fadeOut(animationSpec = tween(150)))
            },
            label = "notifButtonAnim"
        ) { granted ->
            if (granted) {
                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = strings.nextButton,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNext,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(0.42f)
                            .height(56.dp)
                    ) {
                        Text(
                            text = strings.skipButton,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = onRequest,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(0.58f)
                            .height(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.allowButton,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageStepContent(
    isGranted: Boolean,
    onRequest: () -> Unit,
    onNext: () -> Unit
) {
    val strings = LocalStrings.current

    val infiniteTransition = rememberInfiniteTransition(label = "paperAnim")
    val paper1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "p1"
    )
    val paper2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, delayMillis = 600, easing = LinearEasing), RepeatMode.Restart),
        label = "p2"
    )
    val paper3Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, delayMillis = 1200, easing = LinearEasing), RepeatMode.Restart),
        label = "p3"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(10.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.size(175.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    modifier = Modifier.size(160.dp)
                ) {}

                FlyingSheet(progress = paper1Progress, xOffset = (-30).dp, rotationTarget = -16f)
                FlyingSheet(progress = paper2Progress, xOffset = 0.dp, rotationTarget = 4f)
                FlyingSheet(progress = paper3Progress, xOffset = 30.dp, rotationTarget = 18f)

                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(86.dp)
                        .align(Alignment.Center)
                        .padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = strings.permissionStorageTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = strings.permissionStorageDesc,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        AnimatedContent(
            targetState = isGranted,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)).togetherWith(fadeOut(animationSpec = tween(150)))
            },
            label = "storageButtonAnim"
        ) { granted ->
            if (granted) {
                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = strings.nextButton,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNext,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(0.42f)
                            .height(56.dp)
                    ) {
                        Text(
                            text = strings.skipButton,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = onRequest,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(0.58f)
                            .height(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.allowButton,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlyingSheet(
    progress: Float,
    xOffset: androidx.compose.ui.unit.Dp,
    rotationTarget: Float
) {
    val alpha = when {
        progress < 0.15f -> progress / 0.15f
        progress > 0.78f -> (1f - progress) / 0.22f
        else -> 1f
    }.coerceIn(0f, 1f)

    val currentY = (-62).dp + (74.dp * progress)
    val currentRotation = rotationTarget * (1f - progress)

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shadowElevation = 3.dp,
        modifier = Modifier
            .offset(x = xOffset, y = currentY)
            .graphicsLayer {
                this.alpha = alpha
                rotationZ = currentRotation
                scaleX = 0.75f + (0.25f * (1f - progress))
                scaleY = 0.75f + (0.25f * (1f - progress))
            }
            .size(24.dp, 28.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SetupChoiceStepContent(
    onConfigure: () -> Unit,
    onSkip: () -> Unit
) {
    val strings = LocalStrings.current

    val infiniteTransition = rememberInfiniteTransition(label = "gearTransition")
    val gearAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing)),
        label = "gearAngle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(10.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                modifier = Modifier.size(160.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(88.dp)
                            .graphicsLayer { rotationZ = gearAngle }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = strings.setupChoiceTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = strings.setupChoiceDesc,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onConfigure,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strings.setupButton,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onSkip,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = strings.skipButton,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun QuickSettingsStepContent(
    themeMode: ThemeMode,
    useDynamicColor: Boolean,
    pureMaterialYou: Boolean,
    enableBlur: Boolean,
    useLiquidGlass: Boolean,
    showNavBarLabels: Boolean,
    searchBarPlacement: SearchBarPlacement,
    useAlbumArtGradient: Boolean,
    useDynamicPlayerColors: Boolean,
    betaDynamicPlaybackContext: Boolean,
    onChangeThemeMode: (ThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onTogglePureMaterialYou: (Boolean) -> Unit,
    onToggleBlur: (Boolean) -> Unit,
    onToggleLiquidGlass: (Boolean) -> Unit,
    onToggleShowNavBarLabels: (Boolean) -> Unit,
    onChangeSearchBarPlacement: (SearchBarPlacement) -> Unit,
    onToggleAlbumArtGradient: (Boolean) -> Unit,
    onToggleDynamicPlayerColors: (Boolean) -> Unit,
    onToggleBetaDynamicPlaybackContext: (Boolean) -> Unit,
    onOpenEqualizer: () -> Unit,
    onFinish: () -> Unit
) {
    val strings = LocalStrings.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSearchPlacementDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 100.dp)
        ) {
            Text(
                text = strings.setupChoiceTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = strings.setupChoiceDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = strings.settingsCategoryAppearance,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
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
                            Switch(checked = useDynamicColor, onCheckedChange = onToggleDynamicColor)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text(strings.pureMaterialYouTitle) },
                        supportingContent = { Text(strings.pureMaterialYouDesc) },
                        leadingContent = { Icon(Icons.Filled.ColorLens, contentDescription = null) },
                        trailingContent = {
                            Switch(checked = pureMaterialYou, onCheckedChange = onTogglePureMaterialYou)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text(strings.blurEffectTitle) },
                        supportingContent = { Text(strings.blurEffectDesc) },
                        leadingContent = { Icon(Icons.Filled.BlurOn, contentDescription = null) },
                        trailingContent = {
                            Switch(checked = enableBlur, onCheckedChange = onToggleBlur)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text(strings.liquidGlassTitle) },
                        supportingContent = { Text(strings.liquidGlassDescSupported) },
                        leadingContent = { Icon(Icons.Filled.AutoFixNormal, contentDescription = null) },
                        trailingContent = {
                            Switch(checked = useLiquidGlass, onCheckedChange = onToggleLiquidGlass)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text(strings.dynamicCoverGradientTitle) },
                        supportingContent = { Text(strings.dynamicCoverGradientDesc) },
                        leadingContent = { Icon(Icons.Filled.Gradient, contentDescription = null) },
                        trailingContent = {
                            Switch(checked = useAlbumArtGradient, onCheckedChange = onToggleAlbumArtGradient)
                        }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = strings.settingsCategoryNavigation,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text(strings.searchBarPlacementTitle) },
                        supportingContent = { Text(searchBarPlacement.getDisplayName(strings)) },
                        leadingContent = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.clickable { showSearchPlacementDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text(strings.showNavBarLabelsTitle) },
                        supportingContent = { Text(strings.showNavBarLabelsDesc) },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                        trailingContent = {
                            Switch(checked = showNavBarLabels, onCheckedChange = onToggleShowNavBarLabels)
                        }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = strings.settingsCategoryPlayback,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text(strings.betaDynamicPlaybackContextTitle) },
                        supportingContent = { Text(strings.betaDynamicPlaybackContextDesc) },
                        leadingContent = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                        trailingContent = {
                            Switch(checked = betaDynamicPlaybackContext, onCheckedChange = onToggleBetaDynamicPlaybackContext)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text(strings.equalizerTitle) },
                        supportingContent = { Text(strings.equalizerTitle) },
                        leadingContent = { Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { onOpenEqualizer() }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text(strings.dynamicPlayerColorsTitle) },
                        supportingContent = { Text(strings.dynamicPlayerColorsDesc) },
                        leadingContent = { Icon(Icons.Filled.InvertColors, contentDescription = null) },
                        trailingContent = {
                            Switch(checked = useDynamicPlayerColors, onCheckedChange = onToggleDynamicPlayerColors)
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Button(
                onClick = onFinish,
                shape = RoundedCornerShape(26.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = strings.allSetButton,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showThemeDialog) {
        ThemeAutoUpdaterDialog(
            currentTheme = themeMode,
            onSelectTheme = {
                onChangeThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showSearchPlacementDialog) {
        SearchPlacementAutoUpdaterDialog(
            currentPlacement = searchBarPlacement,
            onSelectPlacement = {
                onChangeSearchBarPlacement(it)
                showSearchPlacementDialog = false
            },
            onDismiss = { showSearchPlacementDialog = false }
        )
    }
}
