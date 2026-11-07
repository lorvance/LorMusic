package com.lormusic.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.SpatialAudio
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lormusic.app.data.EqualizerManager
import com.lormusic.app.data.PreferencesRepository
import com.lormusic.app.model.LocalStrings

private val BAND_LABELS = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val strings = LocalStrings.current
    val prefs = remember(context) { PreferencesRepository(context) }

    var state by remember { mutableStateOf(EqualizerManager.loadState(prefs)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = strings.equalizerTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (state.enabled) strings.moodForYourTaste else strings.sleepTimerOff,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = state.enabled,
                    onCheckedChange = { checked ->
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        EqualizerManager.setEnabled(checked, prefs)
                        state = state.copy(enabled = checked)
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(visible = state.enabled) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EqualizerManager.presets.forEachIndexed { index, preset ->
                            val isSelected = state.presetIndex == index
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    EqualizerManager.applyPreset(index, prefs)
                                    state = EqualizerManager.loadState(prefs)
                                },
                                label = { Text(preset.name, fontSize = 13.sp) },
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = "5-полосный эквалайзер",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(10.dp))

                            BAND_LABELS.forEachIndexed { bandIndex, label ->
                                val bandLevel = state.bandLevels.getOrNull(bandIndex) ?: 0
                                val dbVal = bandLevel / 100f
                                val formattedDb = (if (dbVal > 0) "+" else "") + "%.1f dB".format(dbVal)

                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = formattedDb,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Slider(
                                        value = bandLevel.toFloat(),
                                        onValueChange = { newLvl ->
                                            val intLvl = newLvl.toInt()
                                            EqualizerManager.setBandLevel(bandIndex, intLvl, prefs)
                                            val updated = state.bandLevels.toMutableList()
                                            if (bandIndex in updated.indices) {
                                                updated[bandIndex] = intLvl
                                            }
                                            state = state.copy(bandLevels = updated, presetIndex = -1)
                                        },
                                        valueRange = -1200f..1200f,
                                        steps = 23,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Speaker,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Усиление баса (Bass Boost)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "${(state.bassBoost / 10)}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = state.bassBoost.toFloat(),
                                onValueChange = { v ->
                                    val intV = v.toInt()
                                    EqualizerManager.setBassBoost(intV, prefs)
                                    state = state.copy(bassBoost = intV)
                                },
                                valueRange = 0f..1000f,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SpatialAudio,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Объёмный звук (Virtualizer)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "${(state.virtualizer / 10)}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = state.virtualizer.toFloat(),
                                onValueChange = { v ->
                                    val intV = v.toInt()
                                    EqualizerManager.setVirtualizer(intV, prefs)
                                    state = state.copy(virtualizer = intV)
                                },
                                valueRange = 0f..1000f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            EqualizerManager.reset(prefs)
                            state = EqualizerManager.loadState(prefs)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(strings.clear)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
