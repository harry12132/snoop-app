package com.harry12132.snoop.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.harry12132.snoop.model.DownloadState
import com.harry12132.snoop.model.MusicState
import com.harry12132.snoop.model.NavigationState
import com.harry12132.snoop.model.NotificationActionItem
import com.harry12132.snoop.model.NotificationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SnoopNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        syncActiveNotifications()
    }

    override fun onListenerDisconnected() {
        instance = null
        latestMediaNotificationKey = null
        _latestMusicFlow.value = null
        _latestNavigationFlow.value = null
        _latestNotificationFlow.value = null
        _latestDownloadFlow.value = null
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        processNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        syncActiveNotifications()
    }

    private fun syncActiveNotifications() {
        try {
            val active = activeNotifications ?: emptyArray()
            latestMediaNotificationKey = null
            _latestMusicFlow.value = null
            _latestNavigationFlow.value = null
            _latestNotificationFlow.value = null
            _latestDownloadFlow.value = null
            active.sortedBy { it.postTime }.forEach(::processNotification)
        } catch (_: Exception) {
        }
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: Bundle()
        val pkg = sbn.packageName ?: ""
        val appName = getAppName(pkg)

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val category = notification.category

        // Check if it's Navigation
        if (category == Notification.CATEGORY_NAVIGATION ||
            pkg.contains("maps", ignoreCase = true) ||
            pkg.contains("waze", ignoreCase = true) ||
            pkg.contains("navigation", ignoreCase = true)
        ) {
            val icon = getNotificationLargeIcon(extras, pkg)
            _latestNavigationFlow.value = NavigationState(
                appName = appName,
                instruction = text.ifBlank { title },
                distanceText = subText.ifBlank { "Navigating" },
                etaText = title.ifBlank { "ETA Live" },
                iconBitmap = icon
            )
            return
        }

        // Check if it's Download progress
        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val progressCurrent = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val isProgressIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)

        if (progressMax > 0 || isProgressIndeterminate || category == Notification.CATEGORY_PROGRESS) {
            val pct = if (progressMax > 0) (progressCurrent * 100 / progressMax) else 0
            _latestDownloadFlow.value = DownloadState(
                downloadId = sbn.key,
                fileName = title.ifBlank { text }.ifBlank { "Downloading item" },
                percentage = pct,
                bytesDownloaded = progressCurrent.toLong(),
                totalBytes = progressMax.toLong(),
                isCompleted = pct >= 100 && progressMax > 0
            )
            return
        }

        // Check Media / Music session notification
        val isMediaCategory = category == Notification.CATEGORY_TRANSPORT ||
                extras.containsKey(Notification.EXTRA_MEDIA_SESSION)

        if (isMediaCategory) {
            updateMediaStateFromNotification(sbn, title, text, extras)
            return
        }

        // Ignore Snoop's own persistent service notification and unrelated
        // ongoing system notifications. They are not user-facing alerts.
        if (pkg == packageName || (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        // General Notification
        if (title.isNotBlank() || text.isNotBlank()) {
            val appIcon = getNotificationLargeIcon(extras, pkg)
            val actionsList = mutableListOf<NotificationActionItem>()
            notification.actions
                ?.filter { it.actionIntent != null && it.remoteInputs.isNullOrEmpty() }
                ?.take(2)
                ?.forEach { act ->
                actionsList.add(
                    NotificationActionItem(
                        title = act.title?.toString() ?: "Action",
                        intent = act.actionIntent
                    )
                )
            }

            _latestNotificationFlow.value = NotificationState(
                id = sbn.key,
                packageName = pkg,
                appName = appName,
                appIcon = appIcon,
                title = title.ifBlank { appName },
                text = text,
                postTime = sbn.postTime,
                actions = actionsList,
                onOpen = sbn.notification.contentIntent?.let { contentIntent ->
                    {
                        try {
                            contentIntent.send()
                        } catch (_: PendingIntent.CanceledException) {
                            // The source app may have withdrawn this notification.
                        }
                    }
                },
                onDismiss = {
                    try {
                        cancelNotification(sbn.key)
                    } catch (_: Exception) {
                    }
                }
            )
        }
    }

    private fun updateMediaStateFromNotification(
        sbn: StatusBarNotification,
        title: String,
        artist: String,
        extras: Bundle
    ) {
        latestMediaNotificationKey = sbn.key
        val artwork = getNotificationLargeIcon(extras, sbn.packageName)
        val currentMusic = _latestMusicFlow.value ?: MusicState()
        val actionLabels = sbn.notification.actions
            ?.mapNotNull { it.title?.toString()?.lowercase() }
            .orEmpty()
        val isPlaying = when {
            actionLabels.any { "pause" in it } -> true
            actionLabels.any { "play" in it } -> false
            else -> currentMusic.isPlaying
        }

        val actions = sbn.notification.actions.orEmpty()
        fun actionIntent(vararg keywords: String): (() -> Unit)? {
            val action = actions.firstOrNull { item ->
                val label = item.title?.toString()?.lowercase().orEmpty()
                keywords.any(label::contains) && item.actionIntent != null
            } ?: return null
            return {
                try {
                    action.actionIntent.send()
                } catch (_: PendingIntent.CanceledException) {
                    // The media app may have replaced its notification.
                }
            }
        }

        _latestMusicFlow.value = currentMusic.copy(
            title = title.ifBlank { "Playing Media" },
            artist = artist.ifBlank { getAppName(sbn.packageName) },
            artwork = artwork ?: currentMusic.artwork,
            isPlaying = isPlaying,
            packageName = sbn.packageName,
            onPlayPause = actionIntent(if (isPlaying) "pause" else "play"),
            onSkipNext = actionIntent("next", "skip"),
            onSkipPrev = actionIntent("previous", "back")
        )
    }

    private fun getAppName(pkg: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    private fun getNotificationLargeIcon(extras: Bundle, pkg: String): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_LARGE_ICON, Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_LARGE_ICON) as? Bitmap
            } ?: run {
                val drawable = packageManager.getApplicationIcon(pkg)
                (drawable as? BitmapDrawable)?.bitmap
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private var instance: SnoopNotificationListenerService? = null
        private var latestMediaNotificationKey: String? = null

        private val _latestMusicFlow = MutableStateFlow<MusicState?>(null)
        val latestMusicFlow: StateFlow<MusicState?> = _latestMusicFlow

        private val _latestNavigationFlow = MutableStateFlow<NavigationState?>(null)
        val latestNavigationFlow: StateFlow<NavigationState?> = _latestNavigationFlow

        private val _latestNotificationFlow = MutableStateFlow<NotificationState?>(null)
        val latestNotificationFlow: StateFlow<NotificationState?> = _latestNotificationFlow

        private val _latestDownloadFlow = MutableStateFlow<DownloadState?>(null)
        val latestDownloadFlow: StateFlow<DownloadState?> = _latestDownloadFlow

        fun isServiceConnected(): Boolean = instance != null

        fun updateMediaSessions(context: Context) {
            try {
                val mediaManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                val componentName = android.content.ComponentName(context, SnoopNotificationListenerService::class.java)
                val controllers = mediaManager?.getActiveSessions(componentName)
                if (controllers.isNullOrEmpty()) {
                    if (latestMediaNotificationKey == null) {
                        _latestMusicFlow.value = null
                    }
                    return
                }
                val controller = controllers.firstOrNull {
                    it.playbackState?.state == PlaybackState.STATE_PLAYING
                } ?: controllers.first()
                run {
                    val metadata = controller.metadata
                    val playbackState = controller.playbackState

                    val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Active Track"
                    val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Artist"
                    val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
                    val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                        ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
                    val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
                    val pos = playbackState?.position ?: 0L

                    _latestMusicFlow.value = MusicState(
                        title = title,
                        artist = artist,
                        album = album,
                        artwork = art,
                        isPlaying = isPlaying,
                        durationMs = duration,
                        positionMs = pos,
                        packageName = controller.packageName,
                        onPlayPause = {
                            if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                                controller.transportControls.pause()
                            } else {
                                controller.transportControls.play()
                            }
                        },
                        onSkipNext = { controller.transportControls.skipToNext() },
                        onSkipPrev = { controller.transportControls.skipToPrevious() }
                    )
                }
            } catch (_: Exception) {
            }
        }
    }
}
