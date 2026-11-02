package com.harry12132.snoop

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.harry12132.snoop.data.CutoutMode
import com.harry12132.snoop.data.SnoopSettings
import com.harry12132.snoop.data.SnoopSettingsManager
import com.harry12132.snoop.service.SnoopOverlayService

class MainActivity : ComponentActivity() {

    private var hasOverlayPermission by mutableStateOf(false)
    private var hasNotifListenerPermission by mutableStateOf(false)
    private var hasPhonePermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(false)
    private var hasCameraPermission by mutableStateOf(false)
    private var serviceRunning by mutableStateOf(false)

    private val requestPhonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasPhonePermission = isGranted
            SnoopApp.getInstance().stateHub.refreshCallPermission()
        }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasNotificationPermission = isGranted
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasCameraPermission = isGranted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshPermissions()

        val app = SnoopApp.getInstance()

        setContent {
            SnoopTheme {
                MainContent(
                    settingsManager = app.settingsManager,
                    hasOverlayPermission = hasOverlayPermission,
                    hasNotifListenerPermission = hasNotifListenerPermission,
                    hasPhonePermission = hasPhonePermission,
                    hasNotificationPermission = hasNotificationPermission,
                    hasCameraPermission = hasCameraPermission,
                    serviceRunning = serviceRunning,
                    onEnableOverlayPermission = ::openOverlayPermissionSettings,
                    onEnableNotifListenerPermission = ::openNotifListenerPermissionSettings,
                    onRequestPhonePermission = {
                        requestPhonePermissionLauncher.launch(android.Manifest.permission.READ_PHONE_STATE)
                    },
                    onRequestNotificationPermission = ::requestNotificationPermission,
                    onRequestCameraPermission = ::requestCameraPermission,
                    onStartService = ::startSnoopService,
                    onStopService = ::stopSnoopService
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun refreshPermissions() {
        hasOverlayPermission = Settings.canDrawOverlays(this)
        serviceRunning = SnoopOverlayService.isRunning()
        val phonePermissionNowGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (phonePermissionNowGranted != hasPhonePermission) {
            hasPhonePermission = phonePermissionNowGranted
            SnoopApp.getInstance().stateHub.refreshCallPermission()
        }
        hasNotifListenerPermission = isNotificationListenerEnabled(this)
        hasNotificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        hasCameraPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestCameraPermission() {
        requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }

    private fun openOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun openNotifListenerPermissionSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun startSnoopService() {
        if (!Settings.canDrawOverlays(this)) {
            openOverlayPermissionSettings()
            return
        }
        try {
            ContextCompat.startForegroundService(
                this,
                Intent(this, SnoopOverlayService::class.java)
            )
            serviceRunning = true
        } catch (_: Exception) {
            serviceRunning = SnoopOverlayService.isRunning()
        }
    }

    private fun stopSnoopService() {
        stopService(Intent(this, SnoopOverlayService::class.java))
        serviceRunning = false
    }
}

@Composable
private fun SnoopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            background = Color(0xFFF2F2F7),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE5E5EA),
            primary = Color(0xFF007AFF),
            secondary = Color(0xFF34C759),
            onBackground = Color(0xFF000000),
            onSurface = Color(0xFF1C1C1E)
        ),
        content = content
    )
}

@Composable
private fun MainContent(
    settingsManager: SnoopSettingsManager,
    hasOverlayPermission: Boolean,
    hasNotifListenerPermission: Boolean,
    hasPhonePermission: Boolean,
    hasNotificationPermission: Boolean,
    hasCameraPermission: Boolean,
    serviceRunning: Boolean,
    onEnableOverlayPermission: () -> Unit,
    onEnableNotifListenerPermission: () -> Unit,
    onRequestPhonePermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val settings by settingsManager.settingsFlow.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Snoop",
                        color = Color.Black,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.5).sp
                    )
                    Text(
                        text = "Dynamic Island for Android",
                        color = Color(0xFF8E8E93),
                        fontSize = 14.sp
                    )
                }

                val isRunning = serviceRunning
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isRunning) Color(0xFF34C759).copy(alpha = 0.15f) else Color(0xFFFF3B30).copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isRunning) "ACTIVE" else "STOPPED",
                        color = if (isRunning) Color(0xFF34C759) else Color(0xFFFF3B30),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Tabs
            val tabs = listOf("Overview", "Camera & Pos", "Features")
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> OverviewTab(
                        hasOverlayPermission = hasOverlayPermission,
                        hasNotifListenerPermission = hasNotifListenerPermission,
                        hasPhonePermission = hasPhonePermission,
                        hasNotificationPermission = hasNotificationPermission,
                        hasCameraPermission = hasCameraPermission,
                        serviceRunning = serviceRunning,
                        onEnableOverlayPermission = onEnableOverlayPermission,
                        onEnableNotifListenerPermission = onEnableNotifListenerPermission,
                        onRequestPhonePermission = onRequestPhonePermission,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onRequestCameraPermission = onRequestCameraPermission,
                        onStartService = onStartService,
                        onStopService = onStopService
                    )
                    1 -> CameraPositionTab(settingsManager = settingsManager, settings = settings)
                    2 -> FeaturesTab(settingsManager = settingsManager, settings = settings)
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(
    hasOverlayPermission: Boolean,
    hasNotifListenerPermission: Boolean,
    hasPhonePermission: Boolean,
    hasNotificationPermission: Boolean,
    hasCameraPermission: Boolean,
    serviceRunning: Boolean,
    onEnableOverlayPermission: () -> Unit,
    onEnableNotifListenerPermission: () -> Unit,
    onRequestPhonePermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    val isRunning = serviceRunning

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // The live island is rendered only by the foreground overlay service.
        // Keeping a second in-app preview here created a duplicate island.
        Spacer(modifier = Modifier.height(16.dp))

        // Master Service Control
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Floating Overlay Controller",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isRunning) "Snoop is displaying live activities over active apps." else "Turn on Snoop to display the floating overlay.",
                    color = Color(0xFF8E8E93),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (!isRunning) {
                    Button(
                        onClick = onStartService,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Turn On Snoop Overlay", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onStopService,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Turn Off Snoop Overlay", color = Color(0xFFFF3B30))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Permissions Section
        Text(
            text = "Required Permissions",
            color = Color.Black,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        PermissionCard(
            title = "Display over other apps",
            subtitle = "Draws the dynamic island overlay across apps.",
            isGranted = hasOverlayPermission,
            onGrant = onEnableOverlayPermission
        )
        Spacer(modifier = Modifier.height(10.dp))

        PermissionCard(
            title = "Notification Access",
            subtitle = "Monitors media, timers, navigation and system alerts.",
            isGranted = hasNotifListenerPermission,
            onGrant = onEnableNotifListenerPermission
        )
        Spacer(modifier = Modifier.height(10.dp))

        PermissionCard(
            title = "Phone State Access",
            subtitle = "Detects incoming calls and call duration.",
            isGranted = hasPhonePermission,
            onGrant = onRequestPhonePermission
        )

        Spacer(modifier = Modifier.height(10.dp))

        PermissionCard(
            title = "Notification Permission",
            subtitle = "Allows Android to show Snoop's ongoing service notification.",
            isGranted = hasNotificationPermission,
            onGrant = onRequestNotificationPermission
        )

        Spacer(modifier = Modifier.height(10.dp))

        PermissionCard(
            title = "Camera access",
            subtitle = "Required for the flashlight toggle. Snoop does not capture camera images.",
            isGranted = hasCameraPermission,
            onGrant = onRequestCameraPermission
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, color = Color(0xFF8E8E93), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            if (isGranted) {
                Text(text = "✓ Granted", color = Color(0xFF34C759), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            } else {
                Button(
                    onClick = onGrant,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5E5EA)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Grant", color = Color.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CameraPositionTab(
    settingsManager: SnoopSettingsManager,
    settings: SnoopSettings
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("Camera Cutout & Position", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Align Snoop precisely around your physical front camera hole", color = Color(0xFF8E8E93), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(14.dp))

        // Red Calibration Guide Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Show Alignment Guide Line", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Draws a red calibration outline around Snoop to align with camera", color = Color(0xFF8E8E93), fontSize = 12.sp)
                }
                Switch(
                    checked = settings.showAlignmentGuide,
                    onCheckedChange = { checked ->
                        settingsManager.updateSettings { it.copy(showAlignmentGuide = checked) }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFF3B30))
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Camera Preset Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Camera Cutout Preset", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                CutoutMode.values().forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                settingsManager.updateSettings {
                                    val alignment = when (mode) {
                                        CutoutMode.PUNCH_HOLE_LEFT -> com.harry12132.snoop.data.OverlayAlignment.LEFT
                                        CutoutMode.PUNCH_HOLE_RIGHT -> com.harry12132.snoop.data.OverlayAlignment.RIGHT
                                        CutoutMode.CUSTOM -> it.alignment
                                        else -> com.harry12132.snoop.data.OverlayAlignment.CENTER
                                    }
                                    it.copy(cutoutMode = mode, alignment = alignment)
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = settings.cutoutMode == mode,
                            onClick = {
                                settingsManager.updateSettings {
                                    val alignment = when (mode) {
                                        CutoutMode.PUNCH_HOLE_LEFT -> com.harry12132.snoop.data.OverlayAlignment.LEFT
                                        CutoutMode.PUNCH_HOLE_RIGHT -> com.harry12132.snoop.data.OverlayAlignment.RIGHT
                                        CutoutMode.CUSTOM -> it.alignment
                                        else -> com.harry12132.snoop.data.OverlayAlignment.CENTER
                                    }
                                    it.copy(cutoutMode = mode, alignment = alignment)
                                }
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(mode.name.replace("_", " "), color = Color.Black, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sliders
        SliderCard(
            label = "Vertical Offset from Top (Y)",
            valueStr = "${settings.offsetYDp} dp",
            value = settings.offsetYDp.toFloat(),
            range = 0f..100f,
            onValueChange = { v -> settingsManager.updateSettings { it.copy(offsetYDp = v.toInt()) } }
        )

        Spacer(modifier = Modifier.height(10.dp))

        SliderCard(
            label = "Horizontal Offset (X)",
            valueStr = "${settings.offsetXDp} dp",
            value = settings.offsetXDp.toFloat(),
            range = -100f..100f,
            onValueChange = { v -> settingsManager.updateSettings { it.copy(offsetXDp = v.toInt()) } }
        )

        Spacer(modifier = Modifier.height(10.dp))

        SliderCard(
            label = "Collapsed Island Width",
            valueStr = "${settings.collapsedWidthDp} dp",
            value = settings.collapsedWidthDp.toFloat(),
            range = 100f..250f,
            onValueChange = { v -> settingsManager.updateSettings { it.copy(collapsedWidthDp = v.toInt()) } }
        )

        Spacer(modifier = Modifier.height(10.dp))

        SliderCard(
            label = "Collapsed Island Height",
            valueStr = "${settings.collapsedHeightDp} dp",
            value = settings.collapsedHeightDp.toFloat(),
            range = 28f..60f,
            onValueChange = { v -> settingsManager.updateSettings { it.copy(collapsedHeightDp = v.toInt()) } }
        )
    }
}

@Composable
private fun FeaturesTab(
    settingsManager: SnoopSettingsManager,
    settings: SnoopSettings
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("Active Modules", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Enable or disable specific features displayed in Snoop", color = Color(0xFF8E8E93), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Quick tools", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("With the overlay running, started activities appear in the floating island.", color = Color(0xFF8E8E93), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { SnoopApp.getInstance().stateHub.startTimer(300, "5-minute timer") },
                        modifier = Modifier.weight(1f)
                    ) { Text("5 min timer") }
                    Button(
                        onClick = { SnoopApp.getInstance().stateHub.startTimer(600, "10-minute timer") },
                        modifier = Modifier.weight(1f)
                    ) { Text("10 min timer") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { SnoopApp.getInstance().stateHub.startStopwatch() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Start stopwatch") }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        FeatureToggleItem(
            icon = Icons.Default.PlayArrow,
            title = "Music & Live Waveform",
            description = "Track, artist, artwork & live audio visualizer",
            checked = settings.enableMusic,
            onCheckedChange = { checked -> settingsManager.updateSettings { it.copy(enableMusic = checked) } }
        )

        FeatureToggleItem(
            icon = Icons.Default.Call,
            title = "Call detection",
            description = "Incoming and active call status with an elapsed call timer. Call controls stay in your Phone app.",
            checked = settings.enableCalls,
            onCheckedChange = { checked -> settingsManager.updateSettings { it.copy(enableCalls = checked) } }
        )

        FeatureToggleItem(
            icon = Icons.Default.Add,
            title = "Timers",
            description = "Countdown timers & remaining time ring",
            checked = settings.enableTimers,
            onCheckedChange = { checked -> settingsManager.updateSettings { it.copy(enableTimers = checked) } }
        )

        FeatureToggleItem(
            icon = Icons.Default.Edit,
            title = "Stopwatches",
            description = "Elapsed time, start, pause, reset & laps",
            checked = settings.enableStopwatches,
            onCheckedChange = { checked -> settingsManager.updateSettings { it.copy(enableStopwatches = checked) } }
        )

        FeatureToggleItem(
            icon = Icons.Default.Share,
            title = "Navigation",
            description = "Turn-by-turn directions, distance & ETA",
            checked = settings.enableNavigation,
            onCheckedChange = { checked -> settingsManager.updateSettings { it.copy(enableNavigation = checked) } }
        )

        FeatureToggleItem(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            description = "Incoming notification previews & app icons",
            checked = settings.enableNotifications,
            onCheckedChange = { checked -> settingsManager.updateSettings { it.copy(enableNotifications = checked) } }
        )

        FeatureToggleItem(
            icon = Icons.Default.Info,
            title = "Battery & Charging",
            description = "Charging state, percentage & low-battery events",
            checked = settings.enableBattery,
            onCheckedChange = { checked -> settingsManager.updateSettings { it.copy(enableBattery = checked) } }
        )

        FeatureToggleItem(
            icon = Icons.Default.Check,
            title = "Downloads",
            description = "Live download progress bar & completion alerts",
            checked = settings.enableDownloads,
            onCheckedChange = { checked -> settingsManager.updateSettings { it.copy(enableDownloads = checked) } }
        )

        FeatureToggleItem(
            icon = Icons.Default.Build,
            title = "System Controls",
            description = "Flashlight, ringer mode & volume controls",
            checked = settings.enableSystemControls,
            onCheckedChange = { checked -> settingsManager.updateSettings { it.copy(enableSystemControls = checked) } }
        )
    }
}

@Composable
private fun FeatureToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(text = description, color = Color(0xFF8E8E93), fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun SliderCard(
    label: String,
    valueStr: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(valueStr, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
