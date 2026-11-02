package com.harry12132.snoop.observer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.harry12132.snoop.model.BatteryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BatteryObserver(private val context: Context) {
    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState

    private var isRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(cntx: Context?, intent: Intent?) {
            intent?.let { updateStateFromIntent(it) }
        }
    }

    fun start() {
        if (isRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        val initialStatus = context.registerReceiver(receiver, filter)
        initialStatus?.let { updateStateFromIntent(it) }
        isRegistered = true
    }

    fun stop() {
        if (!isRegistered) return
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {
        }
        isRegistered = false
    }

    private fun updateStateFromIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percentage = if (level >= 0 && scale > 0) {
            (level * 100f / scale).toInt()
        } else 100

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val plugStr = when (plugType) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Charging"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Charging"
            else -> if (isCharging) "Charging" else "Unplugged"
        }

        _batteryState.value = BatteryState(
            percentage = percentage,
            isCharging = isCharging,
            plugType = plugStr,
            isLowBattery = percentage <= 20 && !isCharging
        )
    }
}
