# ROYYA WATCH - Voice Alert Architecture & Call-Style Incident Handling

**Status:** Living Document  
**Version:** 1.0  
**Target Platform:** Native Android (Jetpack Compose, AndroidX Media3 ExoPlayer, Notification Channels)

---

## 1. High-Level Vision

The voice alert experience is the primary differentiating feature of Royya Watch. Shrimp farmers work outdoors under bright sunlight and may be asleep during the night when dissolved oxygen levels drop rapidly to lethal thresholds (< 4.0 mg/L).

The system acts as a **vocal, reliable pond assistant**:
- Critical events break through silent/vibrate modes via a dedicated high-priority notification channel.
- Farmers receive a full-screen, call-inspired alert interface on their phone without impersonating a fake PSTN telephone call.
- Natural, conversational Telugu audio explains the situation clearly and provides the immediate next step.
- The farmer can tap **వినండి (Listen)** to replay audio or **అర్థమైంది (Acknowledge)** to record response and stop the alert.

---

## 2. End-to-End Alert Flow

```
[Shrimp Pond Sensors (ESP32)]
              |
              | Continuous telemetry (Temp, DO, Turbidity)
              v
[FastAPI Telemetry Engine]
              |
              | DO drops < 5.0 mg/L (Critical Threshold)
              v
[Server-Side Incident Generator]
              |
              |-- 1. Select localized script (Telugu / English)
              |-- 2. Synthesize audio via Telugu TTS provider (Sarvam AI / Azure)
              |-- 3. Upload MP3 to S3 with pre-signed retrieval URL
              |-- 4. Store Alert record in database
              v
[Firebase Cloud Messaging (FCM)]
              |
              | Dispatch High-Priority Data Payload (< 4KB)
              v
[Android FCM Service (RoyyaFirebaseMessagingService)]
              |
              |-- Parse & validate schema
              |-- Deduplicate alert ID
              |-- Trigger Full-Screen Intent / Heads-Up Notification
              v
[Critical Alert Screen (AlertDetailsScreen)]
              |
              |-- Display bold status card ("అత్యవసర హెచ్చరిక")
              |-- Download & cache alert audio in background
              |-- Farmer taps "వినండి" (Listen) -> Media3 ExoPlayer streams/plays audio
              |-- Farmer reviews advice ("ఎరేటర్ ఆన్‌లో ఉంది...")
              |-- Farmer taps "అర్థమైంది" (Acknowledge)
              v
[Repository Acknowledgment]
              |
              | Updates local Room DB & sends POST /api/v1/alerts/{id}/acknowledge
              v
[Incident Resolved / Calmed]
```

---

## 3. Alert Severity & Types

| Alert Type | Default Severity | Trigger Condition | Example Telugu Title | Example Telugu Audio Script |
|---|---|---|---|---|
| `LOW_DISSOLVED_OXYGEN` | **CRITICAL** | DO < 5.0 mg/L (or forecast < 4.5 in 30m) | అత్యవసర హెచ్చరిక: తక్కువ ఆక్సిజన్ | "నమస్కారం రైతు సోదరులారా. మీ చెరువులో ఆక్సిజన్ స్థాయి ప్రమాదకరంగా పడిపోయింది. ఆటోమేటిక్ ఏరేటర్ ఆన్ చేయబడింది. దయచేసి వెంటనే చెరువు వద్దకు వెళ్లి పరిశీలించండి." |
| `AERATOR_FAILURE` | **CRITICAL** | Relayed ON command received, but motor feedback/power draw is 0 | ఏరేటర్ వైఫల్యం | "హెచ్చరిక. చెరువు ఏరేటర్ ఆన్ కాలేదు. మోటార్ విద్యుత్ సరఫరా మరియు స్టార్టర్ తనిఖీ చేయండి." |
| `HIGH_TEMPERATURE` | **WARNING** | Water temp > 32°C or < 24°C | అధిక నీటి ఉష్ణోగ్రత | "గమనిక. చెరువు నీటి ఉష్ణోగ్రత ఎక్కువగా ఉంది. ఫీడింగ్ సర్దుబాటు చేయండి." |
| `WATER_QUALITY_WARNING`| **WARNING** | Turbidity > 3.5V or abnormal pH | నీటి స్వచ్ఛత హెచ్చరిక | "చెరువు నీటి నాణ్యతలో మార్పు వచ్చింది. స్వచ్ఛతను తనిఖీ చేయండి." |
| `DEVICE_OFFLINE` | **WARNING** | No telemetry packets for > 15 minutes | పరికరం ఆఫ్‌లైన్ | "చెరువు సెన్సార్ నుండి సమాచారం అందడం లేదు. ఇంటర్నెట్ మరియు కరెంట్ చెక్ చేయండి." |
| `SYSTEM_INFO` | **INFO** | Firmware update, successful maintenance | సాధారణ సమాచారం | "చెరువు పర్యవేక్షణ సాధారణ స్థితిలో కొనసాగుతోంది." |

---

## 4. Audio Playback Engine (`AudioPlayerManager`)

### Architecture
- Powered by `androidx.media3:media3-exoplayer`.
- Encapsulated behind `AudioPlayerManager` singleton.
- **Audio Focus Management:** Uses `AudioAttributes.Builder()` configured with `USAGE_ALARM` or `USAGE_ASSISTANCE_SONIFICATION` for critical warnings, pausing background media playback cleanly.
- **Disk Caching:** `AudioCacheManager` uses a dedicated cache directory `files/audio_cache/` indexed by `alert_id`. Subsequent taps on "వినండి" (Listen) play directly from local cache with zero latency and zero data consumption.
- **Resilience:** If network is unavailable or audio download fails, the app falls back to bundled emergency Telugu clips in `res/raw/audio_low_do_te.mp3` or renders high-contrast translated text without crashing.
