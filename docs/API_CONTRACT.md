# ROYYA WATCH - API Contract Specification

**Status:** Living Document  
**Version:** 1.0  
**Context:** The production FastAPI backend resides on a separate machine and its source code is currently uncommitted. This document categorizes contracts into **Category A (Observed in existing Web Frontend)** and **Category B (Proposed Future Native Android Contracts)**.

---

> [!WARNING]
> **Category B endpoints are proposed contracts for design and mock simulation only.**  
> They must **never** be assumed to exist or function in production until the remote FastAPI source code is verified. Android ViewModels must interact through repository interfaces backed by `Mock*` implementations in development mode.

---

## CATEGORY A: Existing Frontend-Observed Contracts

These contracts are extracted directly from the existing Next.js web application codebase (`lib/api.ts`, `lib/data.ts`, `lib/useTelemetry.ts`, `components/MockDOSimulator.tsx`).

### Default Base URL
`http://10.138.215.133:8000` (Configurable via `NEXT_PUBLIC_API_URL` or Android build flavor `BACKEND_BASE_URL`).

---

### A.1 Health Check
* **Method:** `GET`
* **Path:** `/`
* **Description:** Verifies whether the FastAPI server is reachable.
* **Request Headers:**
  * `Accept: application/json`
* **Response (200 OK):**
```json
{
  "message": "Royya Watch FastAPI Backend is operational",
  "status": "healthy"
}
```

---

### A.2 Current System Telemetry
* **Method:** `GET`
* **Path:** `/api/data`
* **Description:** Returns the latest telemetry reading collected from the active ESP32 device and the current aerator relay status.
* **Request Headers:**
  * `Accept: application/json`
* **Response (200 OK):**
```json
{
  "temperature": 28.4,
  "turbidity": 2.1,
  "dissolved_oxygen": 5.8,
  "mode": "AUTO",
  "aerator_state": "ON",
  "mock_do_enabled": false
}
```
* **Observed Data Types:**
  * `temperature`: Float (°C)
  * `turbidity`: Float (Sensor Voltage or NTU)
  * `dissolved_oxygen`: Float (mg/L, predicted or measured)
  * `mode`: Enum (`"AUTO"` | `"MANUAL"`)
  * `aerator_state`: Enum (`"ON"` | `"OFF"`)
  * `mock_do_enabled`: Boolean (optional)

---

### A.3 Aerator State
* **Method:** `GET`
* **Path:** `/api/aerator/state`
* **Description:** Retrieves the current automation mode and physical relay state.
* **Response (200 OK):**
```json
{
  "mode": "AUTO",
  "state": "ON"
}
```

---

### A.4 Set Aerator Mode
* **Method:** `POST`
* **Path:** `/api/aerator/mode`
* **Description:** Sets the control mode to either automatic hysteresis or manual operator override.
* **Request Headers:**
  * `Content-Type: application/json`
* **Request Body:**
```json
{
  "mode": "MANUAL"
}
```
* **Response (200 OK):**
```json
{
  "mode": "MANUAL",
  "state": "ON"
}
```

---

### A.5 Set Manual Aerator Relay State
* **Method:** `POST`
* **Path:** `/api/aerator/manual`
* **Description:** Manually toggles the physical aerator relay ON or OFF. Backend rejects or ignores this request if mode is `AUTO`.
* **Request Headers:**
  * `Content-Type: application/json`
* **Request Body:**
```json
{
  "state": "OFF"
}
```
* **Response (200 OK):**
```json
{
  "mode": "MANUAL",
  "state": "OFF"
}
```
* **Error Response (400 Bad Request):**
```json
{
  "detail": "Cannot switch manual state while system is in AUTO mode."
}
```

---

### A.6 Mock DO Simulation Override
* **Method:** `POST`
* **Path:** `/api/mock/do`
* **Description:** Sets an override dissolved oxygen value to test automation and hysteresis loops on the backend.
* **Request Body:**
```json
{
  "enabled": true,
  "dissolved_oxygen": 4.2
}
```
* **Response (200 OK):**
```json
{
  "mock_do_enabled": true,
  "dissolved_oxygen": 4.2,
  "mode": "AUTO",
  "aerator_state": "ON"
}
```

---

### Limitations of Category A Endpoints
1. **Single-Device / Single-Pond:** No `pond_id` or `device_id` parameters exist in the observed paths.
2. **No Authentication:** No JWT or Bearer token headers were observed in `lib/api.ts`.
3. **No Historical Metrics Endpoint:** Historical data in `lib/data.ts` was mocked purely in the frontend.
4. **No Alert Generation or Voice Delivery Endpoints:** Alerts in `FarmerAlerts.tsx` were evaluated client-side based on the single `/api/data` payload.

---

## CATEGORY B: Proposed Future Android Integration Contracts

These endpoints represent the target production API required to support multi-pond farming, secure authentication, historical charts, push notifications, and server-side voice alerts.

---

### B.1 Farmer Authentication & Session

#### `POST /api/v1/auth/login`
* **Request Body:**
```json
{
  "phone_number": "+919876543210",
  "pin": "1234"
}
```
* **Response (200 OK):**
```json
{
  "access_token": "eyJhbGciOi...",
  "refresh_token": "eyJhbGciOi...",
  "token_type": "Bearer",
  "expires_in": 86400,
  "farmer": {
    "id": "farmer_01",
    "name": "Ravi Kumar",
    "phone": "+919876543210",
    "preferred_language": "te-IN"
  }
}
```

#### `POST /api/v1/devices/fcm-token`
* **Headers:** `Authorization: Bearer <token>`
* **Description:** Registers the Android device FCM token for push notification dispatch.
* **Request Body:**
```json
{
  "fcm_token": "fX4a8...d9K",
  "platform": "android",
  "app_version": "2.0.0",
  "language": "te-IN"
}
```
* **Response (200 OK):**
```json
{
  "registered": true,
  "device_id": "android_dev_882"
}
```

---

### B.2 Multi-Pond & Telemetry Management

#### `GET /api/v1/ponds`
* **Headers:** `Authorization: Bearer <token>`
* **Description:** Returns all shrimp ponds assigned to the authenticated farmer.
* **Response (200 OK):**
```json
{
  "ponds": [
    {
      "id": "pond_01",
      "name": "Pond 01 (ఉత్తర చెరువు)",
      "location": "Bhimavaram Cluster A",
      "device_id": "esp32_relay_01",
      "connectivity": "ONLINE",
      "health_score": 92,
      "health_status": "HEALTHY",
      "last_updated": "2026-11-10T00:15:30Z"
    },
    {
      "id": "pond_02",
      "name": "Pond 02 (దక్షిణ చెరువు)",
      "location": "Bhimavaram Cluster B",
      "device_id": "esp32_relay_02",
      "connectivity": "ONLINE",
      "health_score": 68,
      "health_status": "ATTENTION",
      "last_updated": "2026-11-10T00:14:10Z"
    }
  ]
}
```

#### `GET /api/v1/ponds/{pond_id}/summary`
* **Headers:** `Authorization: Bearer <token>`
* **Response (200 OK):**
```json
{
  "pond_id": "pond_01",
  "health_score": 92,
  "health_status": "HEALTHY",
  "latest_reading": {
    "temperature": 28.5,
    "dissolved_oxygen": 6.4,
    "turbidity": 2.1,
    "ph": 7.8,
    "tds": 780,
    "oxygen_source": "PREDICTED",
    "timestamp": "2026-11-10T00:15:30Z"
  },
  "aerator": {
    "mode": "AUTO",
    "relay_state": "OFF",
    "last_state_change": "2026-11-09T23:45:00Z"
  },
  "connectivity": "ONLINE",
  "active_alert_count": 0
}
```

#### `GET /api/v1/ponds/{pond_id}/history?range={1h|6h|24h|7d}`
* **Response (200 OK):**
```json
{
  "pond_id": "pond_01",
  "range": "24h",
  "points": [
    {
      "timestamp": "2026-11-09T01:00:00Z",
      "temperature": 27.8,
      "dissolved_oxygen": 6.8,
      "turbidity": 2.0,
      "aerator_state": "OFF"
    },
    {
      "timestamp": "2026-11-09T03:00:00Z",
      "temperature": 27.2,
      "dissolved_oxygen": 4.8,
      "turbidity": 2.3,
      "aerator_state": "ON"
    }
  ]
}
```

---

### B.3 Alert & Voice Notification Delivery

#### FCM Alert Payload Structure
* **Protocol:** FCM High Priority Data Message
* **Payload Size:** < 4 KB
```json
{
  "data": {
    "type": "CRITICAL_ALERT",
    "alert_id": "alert_9021",
    "pond_id": "pond_01",
    "pond_name": "Pond 01",
    "severity": "CRITICAL",
    "alert_type": "LOW_DISSOLVED_OXYGEN",
    "current_value": "4.1 mg/L",
    "language": "te-IN",
    "title_te": "అత్యవసర హెచ్చరిక",
    "message_te": "మీ చెరువులో ఆక్సిజన్ 4.1కి పడిపోయింది. ఆటో మోడ్‌లో ఏరేటర్ ఆన్ చేయబడింది.",
    "audio_url": "https://storage.royya.software/voice/alert_9021_te.mp3?Expires=1762734000&Signature=...",
    "created_at": "2026-11-10T00:18:00Z",
    "schema_version": "1"
  }
}
```

#### `GET /api/v1/alerts?pond_id={pond_id}&severity={severity}`
* **Response (200 OK):**
```json
{
  "alerts": [
    {
      "id": "alert_9021",
      "pond_id": "pond_01",
      "severity": "CRITICAL",
      "type": "LOW_DISSOLVED_OXYGEN",
      "title": "అత్యవసర హెచ్చరిక",
      "message": "మీ చెరువులో ఆక్సిజన్ స్థాయి చాలా తక్కువగా ఉంది.",
      "parameter": "Dissolved Oxygen",
      "value": "4.1 mg/L",
      "recommendation": "ఎరేటర్ ఆన్‌లో ఉంది. పాడిల్ వీల్స్ తిరుగుతున్నాయో లేదో చూడండి.",
      "audio_reference": "https://storage.royya.software/voice/alert_9021_te.mp3",
      "created_at": "2026-11-10T00:18:00Z",
      "acknowledged_at": null,
      "resolved_at": null,
      "status": "NEW"
    }
  ]
}
```

#### `POST /api/v1/alerts/{alert_id}/acknowledge`
* **Response (200 OK):**
```json
{
  "alert_id": "alert_9021",
  "status": "ACKNOWLEDGED",
  "acknowledged_at": "2026-11-10T00:20:15Z"
}
```

---

### B.4 Pond-Specific Aerator Control

#### `POST /api/v1/ponds/{pond_id}/aerator/mode`
* **Request Body:**
```json
{
  "mode": "AUTO"
}
```
* **Response (200 OK):**
```json
{
  "pond_id": "pond_01",
  "mode": "AUTO",
  "confirmed_state": "ON",
  "pending": false,
  "updated_at": "2026-11-10T00:21:00Z"
}
```

#### `POST /api/v1/ponds/{pond_id}/aerator/manual`
* **Request Body:**
```json
{
  "state": "ON"
}
```
* **Response (200 OK):**
```json
{
  "pond_id": "pond_01",
  "mode": "MANUAL",
  "confirmed_state": "ON",
  "pending": false,
  "updated_at": "2026-11-10T00:21:30Z"
}
```
