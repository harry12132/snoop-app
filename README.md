# Snoop

Snoop is an independent Android live-activity overlay inspired by the glanceable interaction pattern of Dynamic Island. It uses one native Android application-overlay window and public Android APIs.

## Current implementation

The project is under active repair and is **not yet a release candidate**.

Implemented in the current repair branch:

- One native `WindowManager` overlay with compact and expanded states.
- Animated size and corner-radius transitions, tap-to-expand, swipe-to-switch, long-press positioning, and automatic collapse.
- Persistent settings for camera alignment, position, dimensions, and enabled modules.
- Media-session detection with track metadata, artwork, progress, and play/pause/skip controls.
- Call-state detection and active-call duration. Android does not let an ordinary overlay app answer or hang up cellular calls, so Snoop directs users to the phone's native call controls.
- Persistent countdown timers and stopwatches, including pause/resume and stopwatch laps.
- Navigation information from supported navigation notifications.
- Notification previews, source-app open action, supported notification actions, and dismissal.
- Download progress notifications.
- Battery and charging events.
- Flashlight, ringer-mode, and media-volume controls.

Integrations depend on what Android and each source app expose. Navigation and download details are notification-driven; Snoop cannot guarantee that every third-party app publishes enough information.

## Required user-granted access

- **Display over other apps:** required for the floating overlay.
- **Notification access:** required for media sessions, notifications, navigation, and download notifications.
- **Notification permission:** allows Android to show Snoop's foreground-service notification on Android 13+.
- **Phone state:** enables call-state detection.
- **Camera permission:** used only when the user invokes the flashlight control. Snoop does not capture camera images.

Snoop should only start its monitoring observers while the user has enabled the overlay service.

## Development

Open the project in Android Studio and sync Gradle.

- Language: Kotlin
- App UI: Jetpack Compose
- Overlay: native Android views in a single `TYPE_APPLICATION_OVERLAY` window
- Minimum Android version: Android 8.0 (API 26)
- Compile/target SDK: 35
- Application ID: `com.harry12132.snoop`
- Android Gradle Plugin: 8.13.2
- Gradle: 8.13
- Java: 17

GitHub Actions builds the debug APK and uploads it as a workflow artifact.

## Platform and release notes

- Android requires users to grant overlay access in system settings.
- The foreground service uses Android 14+ `specialUse`. Its suitability and Play Console declaration must be reviewed before distribution.
- The notification listener and overlay permissions are sensitive special access. The app must provide clear in-app disclosure and follow Google Play policy.
- Camera-cutout positioning varies by device and Android version. Calibration settings cannot guarantee identical alignment across all phones.
- Cellular call controls are intentionally not presented as working buttons because ordinary apps cannot execute them through public APIs.
- A successful debug build is not proof of device compatibility. Physical-device testing, accessibility checks, privacy review, Play policy review, release signing, and a signed Android App Bundle are still required before publication.

## Principles

- Native Android APIs and the smallest practical permission set.
- No duplicate overlay windows or fake controls.
- Clear privacy explanations and user-controlled monitoring.
- Motion and state changes should remain responsive without rebuilding the entire overlay on every timer tick.
