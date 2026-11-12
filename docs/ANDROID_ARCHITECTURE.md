# ROYYA WATCH - Native Android Farmer Application Architecture

**Version:** 1.0  
**Status:** Approved for Implementation  
**Package:** `software.royya.watch`  
**Target Platform:** Android 8.0+ (API Level 26 to 35)  
**Primary Language / UI:** Kotlin 2.0+ / Jetpack Compose & Material 3  
**Primary Locale:** Telugu (`te-IN`), Secondary: English (`en-IN`)

---

## 1. System Ecosystem & Architectural Vision

Royya Watch is an intelligent shrimp pond monitoring and automated aeration platform designed for Indian aquaculture farmers. The ecosystem comprises three primary systems:

```
+-----------------------------------------------------------------------------------+
|                              ROYYA WATCH ECOSYSTEM                                |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  1. Next.js 16 Web Portal               2. Native Android Farmer App (This Project)
|     (Existing in /app, /components)        (Located in /android)                  |
|     - Farmer Web Portal                    - Voice-First Telugu Pond Assistant    |
|     - Admin Portal                         - Critical Audio Alarm Full-Screen UI  |
|     - Technical Operator (Nerd) Portal     - Glanceable Pond Health Indicators    |
|     - Web Telemetry & TelemetryContext     - Offline Caching & Replay             |
|     - In-browser Mock DO Simulator         - Field-Resilient Aerator Controls     |
|                                                                                   |
+-----------------------------------+-----------------------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------------------+
|  3. FastAPI Backend & Edge IoT System (System of Record - On Remote Machine)       |
|     - Telemetry Ingestion (ESP32 sensors: DO, Temp, Turbidity, pH, TDS)           |
|     - ML DO Prediction Engine & Auto Hysteresis Aeration Loop                     |
|     - Server-side Voice Generation (Telugu TTS pipeline -> Amazon S3)            |
|     - Alert Generation & FCM Notification Dispatch                                |
+-----------------------------------------------------------------------------------+
```

The Native Android Farmer Application is **not** an administrative telemetry dashboard. It is an **outdoor-friendly, voice-first, Telugu-first field assistant** built specifically for shrimp farmers who require immediate, actionable, and audible insights into pond health.

---

## 2. Core Architectural Principles

1. **Separation of Concerns (Clean Architecture + MVVM):**
   - **Domain Layer:** Pure Kotlin models, repository interfaces, and use cases with zero dependencies on Android frameworks or third-party network libraries.
   - **Data Layer:** Implements repository interfaces. Manages remote communication (Retrofit/OkHttp), local persistence (Room/DataStore), and deterministic offline mock simulation.
   - **Presentation Layer:** Jetpack Compose UI observing unidirectional data flow (`StateFlow`) emitted by Hilt-injected ViewModels.
2. **Repository Abstraction for Backend Decoupling:**
   - Because the FastAPI backend is hosted on a separate remote machine and its code is uncommitted, all UI features depend strictly on domain repository interfaces.
   - Build configurations toggle between `Mock*Repository` and `Remote*Repository` without changing a single line of presentation code.
3. **Voice-First & Localized:**
   - Critical events are communicated through high-clarity Telugu audio downloaded from the server and cached locally.
   - Every string, date, error, and status label is fully localized in `res/values/strings.xml` and `res/values-te/strings.xml`.
4. **Offline Resilience & Data Integrity:**
   - Room database acts as the single source of truth for cached pond state, historical metrics, and past alerts.
   - Simulated or cached metrics are explicitly distinguished from live telemetry. Missing values are represented as `null`/`Unknown`, never zero.
5. **Hardware Safety:**
   - The Android client never runs an independent aeration algorithm. All automation and hysteresis logic remain on the backend/embedded controller.
   - No hardware commands can be triggered in mock/simulation mode.

---

## 3. Package & Project Structure

The project conforms strictly to the agreed workspace layout:

```
royya/
│
├── app/                       # Existing Next.js application
├── components/                # Existing Web React components
├── lib/                       # Existing Web API and telemetry utilities
├── public/                    # Web public assets
│
├── android/                   # Native Android application
│   ├── app/
│   │   ├── build.gradle.kts
│   │   ├── proguard-rules.pro
│   │   └── src/
│   │       ├── main/
│   │       │   ├── AndroidManifest.xml
│   │       │   ├── java/software/royya/watch/
│   │       │   └── res/
│   │       │       ├── values/strings.xml       # English (en-IN)
│   │       │       ├── values-te/strings.xml    # Telugu (te-IN)
│   │       │       └── raw/                     # Bundled audio fallbacks
│   │       └── test/
│   ├── gradle/
│   │   └── wrapper/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradle.properties
│
├── docs/                      # Engineering specifications & contracts
│   ├── ANDROID_ARCHITECTURE.md
│   ├── API_CONTRACT.md
│   └── BACKEND_INTEGRATION.md
│
└── README.md
```

### Kotlin Source Structure (`software.royya.watch`)

```
software.royya.watch/
│
├── core/
│   ├── common/             # DispatcherProvider, Result wrapper, DateTime utilities
│   ├── config/             # AppConfig, BuildFlavor (Dev vs Prod), DataSources
│   ├── network/            # OkHttp client, AuthInterceptor, NetworkMonitor
│   ├── database/           # Room Database, TypeConverters
│   ├── datastore/          # PreferencesDataStore (Language, Audio preferences)
│   ├── design/             # Material 3 Theme, Typography (Noto Sans Telugu), Color tokens
│   └── permissions/        # Notification & Audio permission handlers
│
├── domain/
│   ├── model/              # Pond, SensorReading, Alert, AeratorStatus, Connectivity
│   ├── repository/         # PondRepository, AlertRepository, AeratorRepository, AudioRepository, PreferencesRepository
│   └── usecase/            # GetPondSummaryUseCase, AcknowledgeAlertUseCase, SetAeratorModeUseCase, etc.
│
├── data/
│   ├── local/              # Room Entities, DAOs (PondDao, AlertDao, ReadingDao)
│   ├── remote/             # Retrofit Services (RoyyaApiService), DTOs, RemoteDataSource
│   ├── repository/         # Production Repository implementations (Offline-first Room + Retrofit)
│   └── mock/               # MockPondRepository, MockAlertRepository, MockAeratorRepository, MockAudioRepository
│
├── audio/
│   ├── player/             # AudioPlayerManager (Media3 ExoPlayer wrapper)
│   └── cache/              # AudioCacheManager (Disk LRU cache for alert MP3s)
│
├── notifications/
│   ├── fcm/                # RoyyaFirebaseMessagingService
│   ├── channels/           # NotificationChannelManager (Critical Alerts vs Warnings)
│   └── handlers/           # NotificationActionReceiver, FullScreenAlertIntent
│
├── localization/           # LocaleManager, TeluguNumberFormatter, DateFormatters
│
├── navigation/              # NavGraph, Screen routes, BottomNavigation
│
├── features/
│   ├── onboarding/         # Welcome, LanguageSelection, FarmerProfile, PondPairing, VoiceTest
│   ├── home/               # HomeScreen, PondHealthCard, OxygenCard, AeratorStatusCard
│   ├── ponddetails/        # PondDetailsScreen, MetricCharts, SensorBreakdown
│   ├── alerts/             # AlertsScreen, AlertHistoryList, FilterChips
│   ├── alertdetails/       # CriticalAlertActivity/Composable (Call-style alert with audio playback)
│   ├── aerator/            # AeratorControlDialog / Sheet, Mode Toggle, Relay Control
│   ├── settings/           # SettingsScreen, Profile, Language, Voice test, App info
│   └── debug/              # DeveloperSimulationScreen (10 test scenarios, DO slider, preset triggers)
│
├── di/                     # Hilt Modules (AppModule, DatabaseModule, NetworkModule, RepositoryModule)
└── MainActivity.kt         # Single-activity Compose host
```

---

## 4. Architectural Layers Detail

### 4.1 Domain Layer (Clean Architecture)
- Zero framework dependencies.
- Models:
  - `Pond`: Identifiers, name, location, device connectivity, health score.
  - `SensorReading`: Temperature (°C), Turbidity (NTU/V), Dissolved Oxygen (mg/L), pH, TDS, OxygenSource (`MEASURED`, `PREDICTED`, `SIMULATED`, `UNKNOWN`), Timestamp.
  - `AeratorState`: Mode (`AUTO`, `MANUAL`, `UNKNOWN`), Physical Relay State (`ON`, `OFF`, `UNKNOWN`), LastCommandStatus.
  - `Alert`: ID, PondID, Severity (`INFO`, `WARNING`, `CRITICAL`), Type (`LOW_DISSOLVED_OXYGEN`, `HIGH_TEMPERATURE`, `WATER_QUALITY_WARNING`, `AERATOR_FAILURE`, `DEVICE_OFFLINE`), Localized Title/Message, AudioReference URL/Key, Timestamps, Status (`NEW`, `ACKNOWLEDGED`, `RESOLVED`).
- Repositories:
  - `PondRepository`: Observes pond health, retrieves latest readings, fetches historical series.
  - `AlertRepository`: Observes and acknowledges alerts.
  - `AeratorRepository`: Sends mode commands (`AUTO`/`MANUAL`) and manual states (`ON`/`OFF`).
  - `AudioRepository`: Downloads, caches, and provides playback handles for voice assets.
  - `PreferencesRepository`: Manages language (`te-IN` vs `en-IN`), voice alerts enabled, audio replay.

### 4.2 Data Layer & Mock Subsystem
- **Dependency Inversion via Hilt:** ViewModels inject repository interfaces.
- In `debug` / `dev` builds, `RepositoryModule` binds:
  - `PondRepository` -> `MockPondRepository`
  - `AlertRepository` -> `MockAlertRepository`
  - `AeratorRepository` -> `MockAeratorRepository`
  - `AudioRepository` -> `MockAudioRepository`
- In `release` / `prod` builds, `RepositoryModule` binds:
  - Production implementations wrapping Room DAOs and Retrofit services.
- **Offline Cache Policy:** Network-bound resource strategy: Room emits cached values instantly -> Network fetches latest in background -> Room updates -> UI receives fresh emission.

### 4.3 Audio Layer (Voice-First Engine)
- Powered by **AndroidX Media3 ExoPlayer** (`androidx.media3:media3-exoplayer:1.4.1`).
- `AudioCacheManager`: Maintains an encrypted or isolated disk cache under `context.cacheDir/audio_alerts/` with MD5/SHA256 filename keys.
- `AudioPlayerManager`:
  - Lifecycle-aware (stops on screen dismiss or background unless explicitly playing).
  - Handles network dropouts gracefully with local fallback alerts bundled in `res/raw/`.
  - Exposes playback state (`Idle`, `Loading`, `Playing`, `Paused`, `Completed`, `Error`) via `StateFlow<AudioPlaybackState>`.

### 4.4 Notifications & Call-Style Urgent Alarm
- Two Android Notification Channels:
  1. `channel_critical_alerts`: `IMPORTANCE_HIGH` / `MAX`. Custom alarm audio, vibration pattern, full-screen intent capability for nighttime low-DO emergencies.
  2. `channel_standard_alerts`: `IMPORTANCE_DEFAULT`. Informational and warning notifications.
- When a critical alert FCM payload arrives:
  - Notification displayed with action buttons: **వినండి (Listen)** and **అర్థమైంది (Acknowledge)**.
  - Clicking opens `AlertDetailsScreen` directly in call-style prominent layout.

### 4.5 Developer Simulation Subsystem
- Located in `software.royya.watch.features.debug`.
- Completely excluded from release builds via ProGuard / build configuration.
- Provides 10 real-world aquaculture test scenarios:
  1. Healthy pond (DO: 6.8 mg/L, Temp: 28.5°C)
  2. Warning DO (DO: 5.5 mg/L, inside hysteresis band)
  3. Critical Low DO (DO: 4.2 mg/L, triggers auto aerator ON)
  4. Extreme Emergency DO (DO: 2.5 mg/L, urgent alarm + voice)
  5. Recovering DO (DO: 6.2 mg/L, triggers auto aerator OFF)
  6. Aerator Failure (Relay commanded ON but feedback confirms OFF)
  7. Device / ESP32 Offline (Telemetry timeout > 15s)
  8. Missing Sensor Metrics (pH / TDS missing, DO predicted)
  9. Audio Download / Playback Failure (Fallback to localized text)
  10. Duplicate & Delayed Alert FCM Delivery

---

## 5. Technology Stack & Library Version Decisions

To guarantee compatibility with OpenJDK 21 and Android SDK 35, the following stable, production-tested versions are selected:

| Component | Library / Tool | Selected Version | Rationale |
|---|---|---|---|
| **Build System** | Gradle | 8.10.2 | JDK 21 support and AGP 8.7 compatibility |
| **Android Gradle Plugin** | AGP (`com.android.application`) | 8.7.2 | Official stable plugin supporting API 35 & Compose |
| **Language** | Kotlin | 2.0.21 | Modern language features, stability with Compose compiler plugin |
| **Compiler Plugin** | `org.jetbrains.kotlin.plugin.compose` | 2.0.21 | Integrated Kotlin 2.0 Compose compiler |
| **Target / Compile SDK** | Android SDK | 35 | Target Android 15, backward compatible to Android 8.0 (minSdk 26) |
| **UI Toolkit** | Jetpack Compose BOM | 2024.10.01 | Stable foundation for Material 3 and Compose Foundation |
| **Design System** | Material 3 | 1.3.1 (via BOM) | Dynamic colors, accessible touch targets (48dp+), typography |
| **Navigation** | AndroidX Navigation Compose | 2.8.3 | Type-safe Kotlin navigation and back-stack handling |
| **Dependency Injection**| Google Hilt | 2.52 | Standard Android DI, Hilt Navigation Compose integration |
| **Local Database** | AndroidX Room | 2.6.1 | Type-safe SQLite persistence with Coroutines Flow |
| **Preferences** | AndroidX DataStore | 1.1.1 | Reactive, asynchronous preference storage |
| **Audio Engine** | AndroidX Media3 ExoPlayer | 1.4.1 | Robust streaming, local caching, audio focus management |
| **Networking** | Square Retrofit / OkHttp | 2.11.0 / 4.12.0 | Stable HTTP client with timeout management |
| **Serialization** | Kotlinx Serialization | 1.7.3 | Reflection-free, lightweight JSON parsing |
| **Push Notifications** | Firebase Cloud Messaging (FCM)| 24.0.3 | Push notification delivery for critical alerts |
| **Background Work** | AndroidX WorkManager | 2.9.1 | Deferred offline telemetry synchronization |
| **Testing** | JUnit 4, MockK, Coroutines Test | 4.13.2 / 1.13.13 / 1.9.0 | Comprehensive unit, repository, and ViewModel test suites |

---

## 6. Security & Hardware Safety Measures

1. **No Embedded Secrets:** Zero AWS IAM keys, Firebase service account credentials, or database passwords compiled into the APK.
2. **Audio URL Protection:** Audio URLs returned by the alert payload must be short-lived signed URLs (e.g. AWS S3 Pre-signed URLs with 15-minute expiry) or authenticated endpoint streams.
3. **No Direct Hardware Control:** The mobile app only sends requests to the backend server. The backend validates farm ownership, user session, and hardware safety limits before dispatching MQTT/HTTP commands to the ESP32.
4. **Offline Isolation:** When offline, device-control actions are strictly disabled in the UI to prevent queuing hazardous, stale relay commands.
