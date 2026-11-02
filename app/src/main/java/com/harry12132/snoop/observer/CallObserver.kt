package com.harry12132.snoop.observer

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.harry12132.snoop.model.CallState
import com.harry12132.snoop.model.CallStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CallObserver(private val context: Context) {
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private val _callState = MutableStateFlow<CallState?>(null)
    val callState: StateFlow<CallState?> = _callState

    private var durationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private var legacyListener: PhoneStateListener? = null
    private var api31Callback: Any? = null

    fun start() {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                registerApi31Callback()
            } else {
                registerLegacyListener()
            }
        } catch (_: Exception) {
        }
    }

    fun stop() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (api31Callback as? TelephonyCallback)?.let {
                    telephonyManager.unregisterTelephonyCallback(it)
                }
            } else {
                legacyListener?.let {
                    telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
                }
            }
        } catch (_: Exception) {
        }
        durationJob?.cancel()
        durationJob = null
        legacyListener = null
        api31Callback = null
        _callState.value = null
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerApi31Callback() {
        val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                handleCallStateChange(state, null)
            }
        }
        api31Callback = callback
        try {
            telephonyManager.registerTelephonyCallback(
                ContextCompat.getMainExecutor(context),
                callback
            )
        } catch (_: Exception) {
        }
    }

    @Suppress("DEPRECATION")
    private fun registerLegacyListener() {
        val listener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                handleCallStateChange(state, phoneNumber)
            }
        }
        legacyListener = listener
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    fun handleCallStateChange(state: Int, incomingNumber: String?) {
        val number = incomingNumber?.takeIf { it.isNotBlank() } ?: "Unknown Contact"
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                durationJob?.cancel()
                _callState.value = CallState(
                    callerName = number,
                    callerNumber = number,
                    status = CallStatus.INCOMING,
                    durationSeconds = 0
                )
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                val current = _callState.value
                val updatedStatus = if (current?.status == CallStatus.INCOMING) {
                    CallStatus.ACTIVE
                } else {
                    CallStatus.OUTGOING
                }
                // Keep the caller identity we learned while ringing — OFFHOOK
                // reports no number and would otherwise reset it to "Unknown".
                startDurationTicker(current?.callerName ?: number, updatedStatus)
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                durationJob?.cancel()
                _callState.value = null
            }
        }
    }

    private fun startDurationTicker(number: String, status: CallStatus) {
        durationJob?.cancel()
        durationJob = scope.launch {
            var seconds = 0
            while (isActive) {
                _callState.value = CallState(
                    callerName = number,
                    callerNumber = number,
                    status = status,
                    durationSeconds = seconds
                )
                delay(1000L)
                seconds++
            }
        }
    }

}
