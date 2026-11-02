# Snoop

A native Android glance interface for live activity, media, notifications, and useful system events.

Snoop is an independent implementation using Android's public SDKs and documentation. Its source code, architecture, UI, assets, and product identity are original to this project.

## Current status

The first overlay-engine foundation is in place:

- Explicit "Display over other apps" permission flow.
- User-started foreground service with a persistent notification.
- A single `TYPE_APPLICATION_OVERLAY` window.
- Collapsed and expanded overlay states with animated size and corner-radius changes.
- Stop control in the app and foreground notification.

This is an early implementation. Device-specific positioning, status-bar/cutout behaviour, accessibility, notification/media integrations, and lifecycle edge cases still need physical-device testing.

## Development

Open the project in Android Studio and sync Gradle.

- Language: Kotlin
- UI: Jetpack Compose for the app screen; native Android views for the overlay window
- Minimum Android version: Android 8.0 (API 26)
- Target SDK: 35
- Application ID: `com.harry12132.snoop`

## Platform notes

- Android requires the user to grant overlay access in system settings.
- The service currently declares the Android 14+ `specialUse` foreground-service type. Its suitability and Play Console declaration must be reviewed before distribution.
- Application overlays are layered below critical system windows such as the status bar on some Android builds. Exact camera-cutout alignment is device- and OS-dependent and is not guaranteed by this first pass.
- A foreground-service notification is part of the user-visible running state. Android 13+ notification permission affects whether it appears in the notification drawer.

## Principles

- Native Android APIs, with explicit and minimal permissions.
- Clear privacy explanations and user-controlled features.
- Responsive, restrained UI and purposeful motion.
- Testable components and documented platform limitations.
