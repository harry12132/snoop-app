package com.harry12132.snoop.manager

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import com.harry12132.snoop.model.RingerMode
import com.harry12132.snoop.model.SystemControlState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SystemControlsManager(private val context: Context) {
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val _systemState = MutableStateFlow(SystemControlState())
    val systemState: StateFlow<SystemControlState> = _systemState

    private var cameraId: String? = null
    private var isTorchOn = false

    init {
        try {
            val cameraIds = cameraManager.cameraIdList
            if (cameraIds.isNotEmpty()) {
                cameraId = cameraIds[0]
            }
        } catch (_: Exception) {
        }
        updateState()
    }

    fun updateState() {
        val currentRingerMode = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> RingerMode.SILENT
            AudioManager.RINGER_MODE_VIBRATE -> RingerMode.VIBRATE
            else -> RingerMode.NORMAL
        }

        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volPct = if (maxVol > 0) (curVol * 100 / maxVol) else 50

        _systemState.value = SystemControlState(
            isFlashlightOn = isTorchOn,
            ringerMode = currentRingerMode,
            volumePercent = volPct,
            onToggleFlashlight = { toggleFlashlight() },
            onToggleRinger = { toggleRingerMode() }
        )
    }

    fun toggleFlashlight() {
        val id = cameraId ?: return
        try {
            isTorchOn = !isTorchOn
            cameraManager.setTorchMode(id, isTorchOn)
            updateState()
        } catch (_: Exception) {
        }
    }

    fun toggleRingerMode() {
        try {
            val nextMode = when (audioManager.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                else -> AudioManager.RINGER_MODE_NORMAL
            }
            audioManager.ringerMode = nextMode
            updateState()
        } catch (_: Exception) {
        }
    }
}
