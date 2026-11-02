package com.harry12132.snoop

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.harry12132.snoop.service.SnoopOverlayService

class MainActivity : ComponentActivity() {
    private var overlayAllowed by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshPermissionState()
        setContent {
            SnoopTheme {
                SnoopHome(
                    overlayAllowed = overlayAllowed,
                    onEnableOverlay = ::openOverlayPermissionSettings,
                    onStart = ::startSnoop,
                    onStop = ::stopSnoop
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
    }

    private fun refreshPermissionState() {
        overlayAllowed = Settings.canDrawOverlays(this)
    }

    private fun openOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun startSnoop() {
        if (!Settings.canDrawOverlays(this)) {
            openOverlayPermissionSettings()
            return
        }
        ContextCompat.startForegroundService(
            this,
            Intent(this, SnoopOverlayService::class.java)
        )
    }

    private fun stopSnoop() {
        stopService(Intent(this, SnoopOverlayService::class.java))
    }
}

@Composable
private fun SnoopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF090A0C),
            surface = Color(0xFF141619),
            primary = Color(0xFFE8EAED),
            onBackground = Color(0xFFF4F5F6),
            onSurface = Color(0xFFF4F5F6)
        ),
        content = content
    )
}

@Composable
private fun SnoopHome(
    overlayAllowed: Boolean,
    onEnableOverlay: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "snoop",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-2).sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Everything worth a glance.",
                color = Color(0xFFB4BAC2),
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(30.dp))
            Text(
                text = if (overlayAllowed) "Overlay permission enabled" else "Overlay permission needed",
                color = if (overlayAllowed) Color(0xFF9BC8A5) else Color(0xFFE5B47A),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (!overlayAllowed) {
                Button(onClick = onEnableOverlay) {
                    Text("Enable display over other apps")
                }
            } else {
                Button(onClick = onStart) {
                    Text("Start Snoop")
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onStop) {
                    Text("Stop Snoop")
                }
            }
        }
    }
}
