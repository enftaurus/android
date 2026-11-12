# Royya Watch — Native Android Farmer Application

Royya Watch is an intelligent shrimp pond monitoring and automated aeration Android application designed specifically for shrimp farmers in Andhra Pradesh and Telangana.

## Features

- **Telugu-First Interface**: Native Telugu (`te-IN`) typography, large daylight-readable metric cards, and intuitive shrimp pond health indicators.
- **Incoming Emergency Call Alarm**: Automated full-screen emergency alert overlay with looping ringtone and haptic alarm when Dissolved Oxygen (DO) levels drop critically.
- **Interactive IVR Keypad**: Farmer can answer the call and hear native Telugu neural speech (`"నమస్కారం! రొయ్య వాచ్ నుండి అత్యవసర హెచ్చరిక..."`), with keypad options to control the pond aerator (`1 = ON`, `2 = OFF`).
- **Telemetry & Aerator Management**: Real-time Dissolved Oxygen, temperature, and turbidity monitoring with AUTO / MANUAL aerator mode control.
- **Offline & Fallback Audio**: Bundled raw Telugu voice assets in `res/raw/sample_alert_te.mp3` with automatic fallback when offline.

## Technology Stack

- **Language & UI**: Kotlin 2.0, Jetpack Compose, Material 3
- **Architecture**: MVVM + Clean Architecture, Coroutines, StateFlow
- **Dependency Injection**: Dagger Hilt
- **Networking**: Retrofit 2, OkHttp 3, Kotlinx Serialization
- **Audio & Media**: AndroidX Media3 ExoPlayer, Android MediaPlayer
- **Persistence**: Room Database, DataStore Preferences
- **Testing**: JUnit 4, MockK, Kotlinx Coroutines Test, AndroidX Test (Robolectric / AndroidJUnit4)

## Project Structure

```
android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/software/royya/watch/
│   │   │   │   ├── audio/          # ExoPlayer manager & playback state
│   │   │   │   ├── core/           # Design system, theme, typography, network
│   │   │   │   ├── data/           # Remote & local repositories, DTOs, API service
│   │   │   │   ├── di/             # Hilt modules (Network, Database, Repository)
│   │   │   │   ├── domain/         # Models, Repository interfaces
│   │   │   │   ├── features/       # Compose screens (Home, Call, Alerts, Settings, Onboarding)
│   │   │   │   ├── notifications/  # Notification channels & emergency launchers
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/                # XML layouts, drawables, Telugu/English strings, raw audio
│   │   ├── test/                   # Unit tests (Repositories, Domain, ViewModels)
│   │   └── androidTest/            # On-device reachability & instrumentation tests
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

## Build & Test Instructions

### Prerequisites
- Android Studio Ladybug / Meerkat (or newer)
- JDK 21
- Android SDK (API 35/36 compile target, minSdk 26)

### Run Unit Tests
```bash
./gradlew testDevDebugUnitTest --console=plain
```

### Build APK
```bash
./gradlew assembleDevDebug
```
Output APK location:
`app/build/outputs/apk/dev/debug/app-dev-debug.apk`

### Install onto Connected Phone
```bash
./gradlew installDevDebug
```
Or launch directly via ADB:
```bash
adb shell am start -n software.royya.watch.dev/software.royya.watch.MainActivity
```
