package com.lormusic.app.data

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lormusic.app.model.LocalStrings
import com.lormusic.app.model.resolveStrings

enum class AudioDeviceCategory {
    SPEAKER,
    BLUETOOTH,
    HEADPHONES
}

data class CurrentAudioDevice(
    val category: AudioDeviceCategory,
    val name: String,
    val icon: ImageVector,
    val rawDevice: AudioDeviceInfo? = null
)

data class OutputDeviceChoice(
    val id: String,
    val name: String,
    val category: AudioDeviceCategory,
    val icon: ImageVector,
    val rawDevice: AudioDeviceInfo?,
    val isActive: Boolean
)

fun detectAudioOutput(context: Context): CurrentAudioDevice {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        ?: return CurrentAudioDevice(AudioDeviceCategory.SPEAKER, "Динамик", Icons.Outlined.Speaker)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val btDevice = outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                it.type == AudioDeviceInfo.TYPE_BLE_BROADCAST
            ))
        }
        if (btDevice != null) {
            val rawName = btDevice.productName?.toString()?.trim()
            val name = if (!rawName.isNullOrBlank()) rawName else "Bluetooth"
            return CurrentAudioDevice(AudioDeviceCategory.BLUETOOTH, name, Icons.Outlined.Bluetooth, btDevice)
        }

        val wiredDevice = outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE
        }
        if (wiredDevice != null) {
            return CurrentAudioDevice(AudioDeviceCategory.HEADPHONES, "Наушники", Icons.Outlined.Headphones, wiredDevice)
        }
    } else {
        @Suppress("DEPRECATION")
        if (audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn) {
            return CurrentAudioDevice(AudioDeviceCategory.BLUETOOTH, "Bluetooth", Icons.Outlined.Bluetooth)
        }
        @Suppress("DEPRECATION")
        if (audioManager.isWiredHeadsetOn) {
            return CurrentAudioDevice(AudioDeviceCategory.HEADPHONES, "Наушники", Icons.Outlined.Headphones)
        }
    }

    return CurrentAudioDevice(AudioDeviceCategory.SPEAKER, "Динамик", Icons.Outlined.Speaker)
}

fun getAvailableAudioOutputs(context: Context, activeDevice: CurrentAudioDevice): List<OutputDeviceChoice> {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    val list = mutableListOf<OutputDeviceChoice>()
    val strings = resolveStrings(PreferencesRepository(context).getAppLanguage())

    var speakerDevice: AudioDeviceInfo? = null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        speakerDevice = outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

        val btOutputs = outputs.filter {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                it.type == AudioDeviceInfo.TYPE_BLE_BROADCAST
            ))
        }.distinctBy { it.productName?.toString()?.trim() ?: it.id.toString() }
        for (bt in btOutputs) {
            val rawName = bt.productName?.toString()?.trim()
            val name = if (!rawName.isNullOrBlank()) rawName else strings.audioOutputBluetooth
            val isCurrent = activeDevice.category == AudioDeviceCategory.BLUETOOTH && (activeDevice.name == name || activeDevice.rawDevice?.id == bt.id)
            list.add(OutputDeviceChoice(bt.id.toString(), name, AudioDeviceCategory.BLUETOOTH, Icons.Outlined.Bluetooth, bt, isCurrent))
        }

        val wiredOutputs = outputs.filter {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE
        }
        for (w in wiredOutputs) {
            val name = strings.audioOutputHeadphones
            val isCurrent = activeDevice.category == AudioDeviceCategory.HEADPHONES
            list.add(OutputDeviceChoice(w.id.toString(), name, AudioDeviceCategory.HEADPHONES, Icons.Outlined.Headphones, w, isCurrent))
        }
    }

    val isSpeakerActive = activeDevice.category == AudioDeviceCategory.SPEAKER
    list.add(0, OutputDeviceChoice("builtin_speaker", strings.audioOutputSpeaker, AudioDeviceCategory.SPEAKER, Icons.Outlined.Speaker, speakerDevice, isSpeakerActive))
    return list
}

@Composable
fun rememberAudioOutputDevice(context: Context): CurrentAudioDevice {
    val strings = LocalStrings.current
    val deviceState = remember { mutableStateOf(detectAudioOutput(context)) }

    DisposableEffect(context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
            val callback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    deviceState.value = detectAudioOutput(context)
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    deviceState.value = detectAudioOutput(context)
                }
            }
            audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
            onDispose {
                audioManager.unregisterAudioDeviceCallback(callback)
            }
        } else {
            onDispose {}
        }
    }

    val current = deviceState.value
    val localizedName = when (current.category) {
        AudioDeviceCategory.SPEAKER -> strings.audioOutputSpeaker
        AudioDeviceCategory.HEADPHONES -> strings.audioOutputHeadphones
        AudioDeviceCategory.BLUETOOTH -> if (current.name.equals("Bluetooth", ignoreCase = true)) strings.audioOutputBluetooth else current.name
    }
    return current.copy(name = localizedName)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioOutputPickerSheet(
    onDismissRequest: () -> Unit,
    onSelectDevice: (AudioDeviceInfo?) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val view = LocalView.current
    val activeDevice = rememberAudioOutputDevice(context)
    val availableDevices = remember(activeDevice) {
        getAvailableAudioOutputs(context, activeDevice)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = strings.audioOutputPickerTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = strings.audioOutputPickerDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )

            Spacer(Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    availableDevices.forEachIndexed { index, device ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = device.name,
                                    fontWeight = if (device.isActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (device.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = device.icon,
                                    contentDescription = null,
                                    tint = if (device.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            trailingContent = {
                                if (device.isActive) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    onSelectDevice(device.rawDevice)
                                    onDismissRequest()
                                }
                        )
                        if (index < availableDevices.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    onDismissRequest()
                    try {
                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (_: Exception) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        } catch (_: Exception) {}
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bluetooth,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strings.connectBluetoothDevice,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
