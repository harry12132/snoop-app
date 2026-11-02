package com.harry12132.snoop.manager

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
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
    private var torchCallbackRegistered = false
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == this@SystemControlsManager.cameraId) {
                isTorchOn = enabled
                updateState()
            }
        }
    }

    init {
        try {
            cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) {
        }
        updateState()
    }

    fun start() {
        if (torchCallbackRegistered) return
        try {
            cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
            torchCallbackRegistered = true
        } catch (_: Exception) {
        }
    }

    fun stop() {
        if (!torchCallbackRegistered) return
        try {
            cameraManager.unregisterTorchCallback(torchCallback)
        } catch (_: Exception) {
        }
        torchCallbackRegistered = false
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
            onToggleRinger = { toggleRingerMode() },
            onVolumeDown = { adjustVolume(false) },
            onVolumeUp = { adjustVolume(true) }
        )
    }

    fun toggleFlashlight() {
        val id = cameraId ?: return
        val requestedState = !isTorchOn
        try {
            cameraManager.setTorchMode(id, requestedState)
            isTorchOn = requestedState
            updateState()
        } catch (_: SecurityException) {
            // Camera permission has not been granted. Keep the displayed state truthful.
        } catch (_: Exception) {
            updateState()
        }
    }

    private fun adjustVolume(increase: Boolean) {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                0
            )
            updateState()
        } catch (_: Exception) {
        }
    }

    fun toggleRingerMode() {
        try {
            // Avoid requesting Notification Policy Access just to silence the
            // device. The in-app toggle safely switches Normal and Vibrate.
            val nextMode = if (audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                AudioManager.RINGER_MODE_NORMAL
            } else {
                AudioManager.RINGER_MODE_VIBRATE
            }
            audioManager.ringerMode = nextMode
            updateState()
        } catch (_: Exception) {
        }
    }
}
