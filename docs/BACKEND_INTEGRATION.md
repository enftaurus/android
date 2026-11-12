# ROYYA WATCH - Backend Integration & Verification Guide

**Status:** Living Document  
**Version:** 1.0  
**Target Backend:** FastAPI (Python 3.10+) running on dedicated IoT gateway / edge server  
**Current State:** Uncommitted / Remote machine. Android client is built with Clean Architecture repository abstraction to ensure zero UI changes upon backend connectivity.

---

## 1. Current Integration Context & Reality Check

1. **Backend Machine Isolation:**  
   The FastAPI backend code is deployed on a remote machine (`http://10.138.215.133:8000` as observed in the Next.js portal). The source repository is not committed to the GitHub repo.
2. **Current Frontend Contracts (Category A):**  
   The existing Next.js frontend interacts with basic flat endpoints: `/api/data`, `/api/aerator/state`, `/api/aerator/mode`, `/api/aerator/manual`, and `/api/mock/do`. These endpoints operate synchronously on a single physical pond relay.
3. **Android Client Strategy:**  
   The Android native application must **never fabricate or assume undocumented backend behavior**.  
   - All ViewModels interface exclusively with domain interfaces (`PondRepository`, `AlertRepository`, `AeratorRepository`, `AudioRepository`).
   - Development builds default to `DATA_SOURCE=MOCK`, providing realistic, deterministic offline simulation.
   - Production builds flip `DATA_SOURCE=REMOTE`, activating Retrofit services without touching presentation or navigation layers.

---

## 2. Step-by-Step Backend Verification Checklist

When the backend source code is committed or network access to the FastAPI server is provided, the engineering team must execute this verification checklist before enabling `DATA_SOURCE=REMOTE`:

```
+--------------------------------------------------------------------------------+
|                   BACKEND INTEGRATION VERIFICATION PIPELINE                   |
+--------------------------------------------------------------------------------+
  [1] Router & Endpoint Audit
      Inspect all FastAPI APIRouter instances and OpenAPI docs (Swagger at /docs)
      Confirm paths, HTTP methods, and query parameters.
          |
          v
  [2] Pydantic Schema Verification
      Compare Pydantic models (e.g., SensorDataIn, AeratorCommand) against Android
      Kotlinx Serialization DTOs in software.royya.watch.data.remote.dto.
          |
          v
  [3] Authentication & Authorization Protocol
      Identify session mechanism: Bearer JWT vs Session Cookie vs API Key.
      Verify token refresh lifespan and header formatting.
          |
          v
  [4] Database Models & Multi-Tenancy
      Check PostgreSQL/SQLite models.
      Verify if ponds are partitioned by user/farmer ID or if single-pond model is used.
          |
          v
  [5] Edge Automation & Hysteresis Logic
      Confirm backend hysteresis thresholds:
      - Is DO < 5.0 mg/L strictly commanding Aerator ON?
      - Is DO > 6.0 mg/L strictly commanding Aerator OFF?
      - Does the 5.0 to 6.0 band hold previous state?
          |
          v
  [6] Server-Side Voice & TTS Pipeline
      Verify Telugu TTS provider integration (Sarvam AI / Azure Speech / Google TTS).
      Check S3 / MinIO signed URL generation and expiration headers.
          |
          v
  [7] Contract Integration Tests
      Execute Android unit tests with MockWebServer matching actual FastAPI payloads.
          |
          v
  [8] Flip Build Variant to REMOTE
      Switch DATA_SOURCE=REMOTE in build.gradle.kts and verify end-to-end telemetry.
+--------------------------------------------------------------------------------+
```

---

## 3. Server-Side Voice Generation & Telugu TTS Strategy

### 3.1 The Voice Pipeline
The Android client **never** performs on-device TTS. The backend is the single source of truth for alert voice synthesis:

```
FastAPI Event Engine
       | (Detects DO < 5.0 mg/L or abnormal telemetry)
       v
Localized Telugu Script Selector ("మీ చెరువులో ఆక్సిజన్ స్థాయి పడిపోయింది...")
       |
       v
TTS Provider (Synthesizes natural, conversational Telugu voice)
       |
       v
MP3 Asset Generation (Normalized 24kHz / 48kbps mono MP3)
       |
       v
Private Object Storage (Amazon S3 / Cloudflare R2 / MinIO)
       |
       v
Pre-signed Audio URL (15–60 minute expiration)
       |
       v
FCM Push Notification Payload (Carries audio URL + localized alert text)
       |
       v
Android Client (Caches MP3 to disk -> Plays via Media3 ExoPlayer upon user tap)
```

### 3.2 Evaluating Telugu TTS Providers

> [!CAUTION]
> **Amazon Polly does not currently support Telugu (`te-IN`).**  
> Do not attempt to synthesize Telugu audio using Polly. Use a provider with verified, high-naturalness Telugu neural voices.

Recommended Telugu TTS Providers:

| Provider | Voice / Model | Quality & Pronunciation | Latency | Viability |
|---|---|---|---|---|
| **Sarvam AI (Bulbul / Indic)** | `ananya` or `kavya` | Native Telugu phonetics, conversational, understands rural dialect | ~250ms | **Primary Recommendation** |
| **Azure Speech Services** | `te-IN-MohanNeural` / `te-IN-ShrutiNeural` | Highly natural, standard Telugu cadence | ~300ms | **Secondary Recommendation** |
| **Google Cloud Text-to-Speech** | `te-IN-Standard-A` / `Neural2` | Clear pronunciation, widely available | ~350ms | Solid Alternative |
| **Bhashini (Govt. of India)** | Indic-TTS Telugu | Native open-source models, cost-effective | Variable | Good for domestic deployments |

### 3.3 Audio Caching & Reuse Strategy
- **Static Alert Templates:** Alerts with static messages (e.g., "ఏరేటర్ విద్యుత్ వైఫల్యం" - Aerator Power Failure) should be synthesized once and cached permanently with a deterministic key (e.g., `s3://royya-voice/static/te_aerator_failure_v1.mp3`).
- **Dynamic Readings:** For alerts embedding dynamic values (e.g., "ఆక్సిజన్ 4.1కి పడిపోయింది"), cache keys should hash: `MD5(template_id + rounded_value + lang + voice_version)`.

---

## 4. Hardware Safety & Aerator Command Protocol

### 4.1 Safety Principles
1. **Never send real hardware commands in mock mode.**
2. **Never execute queued stale commands:** If an Android device reconnects after being offline for 10 minutes, do not fire queued manual ON/OFF requests.
3. **Two-phase command confirmation:**
   - Client sends command -> Receives HTTP 202 Accepted (`pending: true`).
   - Client displays spinner / pending state.
   - Background telemetry or WebSocket/FCM confirms actual relay state -> UI updates to confirmed state.
4. **Hysteresis Guarantee:** The client must never implement a competing mobile control loop. The embedded ESP32 or backend controller holds sole authority over automatic switching.

---

## 5. Transitioning from Mock to Remote

In `android/app/build.gradle.kts`, build variants configure the data source:

```kotlin
flavorDimensions += "environment"
productFlavors {
    create("dev") {
        dimension = "environment"
        buildConfigField("String", "DATA_SOURCE", "\"MOCK\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"http://10.138.215.133:8000\"")
        buildConfigField("Boolean", "DEBUG_MENU_ENABLED", "true")
        buildConfigField("Boolean", "ALLOW_SIMULATION", "true")
    }
    create("prod") {
        dimension = "environment"
        buildConfigField("String", "DATA_SOURCE", "\"REMOTE\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"https://api.royya.software\"")
        buildConfigField("Boolean", "DEBUG_MENU_ENABLED", "false")
        buildConfigField("Boolean", "ALLOW_SIMULATION", "false")
    }
}
```

In `RepositoryModule.kt`, Hilt injects the mock or remote implementations according to `BuildConfig.DATA_SOURCE`.
