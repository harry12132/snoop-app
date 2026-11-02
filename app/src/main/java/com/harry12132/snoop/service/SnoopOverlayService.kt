package com.harry12132.snoop.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.harry12132.snoop.MainActivity
import com.harry12132.snoop.R
import com.harry12132.snoop.overlay.SnoopOverlayController

class SnoopOverlayService : Service() {
    private var overlayController: SnoopOverlayController? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSnoop()
            return START_NOT_STICKY
        }

        // Promote immediately; Android requires a foreground notification.
        startAsForegroundService()

        if (!Settings.canDrawOverlays(this)) {
            stopSnoop()
            return START_NOT_STICKY
        }

        return try {
            if (overlayController == null) {
                overlayController = SnoopOverlayController(this)
            }
            overlayController?.show()
            START_STICKY
        } catch (_: SecurityException) {
            stopSnoop()
            START_NOT_STICKY
        } catch (_: android.view.WindowManager.BadTokenException) {
            stopSnoop()
            START_NOT_STICKY
        }
    }

    private fun startAsForegroundService() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, SnoopOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_snoop_notification)
            .setContentTitle("Snoop is active")
            .setContentText("Your glance overlay is running.")
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Snoop overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when the Snoop overlay is active."
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun stopSnoop() {
        overlayController?.hide()
        overlayController = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        overlayController?.hide()
        overlayController = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.harry12132.snoop.action.STOP_OVERLAY"
        private const val CHANNEL_ID = "snoop_overlay"
        private const val NOTIFICATION_ID = 4101
    }
}
